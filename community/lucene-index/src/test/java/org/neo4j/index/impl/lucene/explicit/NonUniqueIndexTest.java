/*
 * Copyright (c) 2018-2020 "Graph Foundation,"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * This file is part of ONgDB.
 *
 * ONgDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.index.impl.lucene.explicit;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import org.neo4j.collection.primitive.PrimitiveLongCollections;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactoryState;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.index.internal.gbptree.RecoveryCleanupWorkCollector;
import org.neo4j.internal.kernel.api.IndexQuery;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.kernel.api.impl.schema.LuceneIndexProviderFactory;
import org.neo4j.kernel.api.impl.schema.NativeLuceneFusionIndexProviderFactory10;
import org.neo4j.kernel.api.impl.schema.NativeLuceneFusionIndexProviderFactory20;
import org.neo4j.kernel.api.index.IndexAccessor;
import org.neo4j.kernel.api.index.IndexProvider;
import org.neo4j.kernel.api.schema.index.SchemaIndexDescriptorFactory;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.impl.api.index.sampling.IndexSamplingConfig;
import org.neo4j.kernel.impl.factory.CommunityEditionModule;
import org.neo4j.kernel.impl.factory.DatabaseInfo;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacadeFactory;
import org.neo4j.kernel.impl.factory.OperationalMode;
import org.neo4j.kernel.impl.factory.PlatformModule;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.impl.logging.NullLogService;
import org.neo4j.kernel.impl.scheduler.CentralJobScheduler;
import org.neo4j.logging.LogProvider;
import org.neo4j.storageengine.api.schema.IndexReader;
import org.neo4j.test.rule.PageCacheAndDependenciesRule;
import org.neo4j.test.rule.fs.DefaultFileSystemRule;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.neo4j.graphdb.Label.label;
import static org.neo4j.graphdb.factory.GraphDatabaseSettings.SchemaIndex.LUCENE10;
import static org.neo4j.graphdb.factory.GraphDatabaseSettings.SchemaIndex.NATIVE10;

public class NonUniqueIndexTest
{
    @Rule
    public PageCacheAndDependenciesRule resources =
            new PageCacheAndDependenciesRule( DefaultFileSystemRule::new, NonUniqueIndexTest.class );

    @Test
    public void concurrentIndexPopulationAndInsertsShouldNotProduceDuplicates() throws Exception
    {
        // Given
        Config config = Config.defaults();
        GraphDatabaseService db = newEmbeddedGraphDatabaseWithSlowJobScheduler( config );

        // When
        try ( Transaction tx = db.beginTx() )
        {
            db.schema().indexFor( label( "SomeLabel" ) ).on( "key" ).create();
            tx.success();
        }
        Node node;
        try ( Transaction tx = db.beginTx() )
        {
            node = db.createNode( label( "SomeLabel" ) );
            node.setProperty( "key", "value" );
            tx.success();
        }

        // Await index population before shutdown, because db.shutdown won't await it. After shutdown this test verifies
        // the index directly through the index provider so it must have been populated for that to work properly.
        try ( Transaction tx = db.beginTx() )
        {
            db.schema().awaitIndexesOnline( 1, MINUTES );
            tx.success();
        }
        db.shutdown();

        // Then
        assertThat( nodeIdsInIndex( config, 1, "value" ), equalTo( singletonList( node.getId() ) ) );
    }

    private GraphDatabaseService newEmbeddedGraphDatabaseWithSlowJobScheduler( Config config )
    {
        GraphDatabaseFactoryState graphDatabaseFactoryState = new GraphDatabaseFactoryState();
        graphDatabaseFactoryState.setUserLogProvider( NullLogService.getInstance().getUserLogProvider() );
        return new GraphDatabaseFacadeFactory( DatabaseInfo.COMMUNITY, CommunityEditionModule::new )
        {
            @Override
            protected PlatformModule createPlatform( File storeDir, Config config, Dependencies dependencies,
                    GraphDatabaseFacade graphDatabaseFacade )
            {
                return new PlatformModule( storeDir, config, databaseInfo, dependencies, graphDatabaseFacade )
                {
                    @Override
                    protected CentralJobScheduler createJobScheduler()
                    {
                        return newSlowJobScheduler();
                    }

                    @Override
                    protected LogService createLogService( LogProvider userLogProvider )
                    {
                        return NullLogService.getInstance();
                    }
                };
            }
        }.newFacade( resources.directory().graphDbDir(), config,
                graphDatabaseFactoryState.databaseDependencies() );
    }

    private static CentralJobScheduler newSlowJobScheduler()
    {
        return new CentralJobScheduler()
        {
            @Override
            public JobHandle schedule( Group group, Runnable job )
            {
                return super.schedule( group, slowRunnable( job ) );
            }
        };
    }

    private static Runnable slowRunnable( final Runnable target )
    {
        return () ->
        {
            LockSupport.parkNanos( 100_000_000L );
            target.run();
        };
    }

    private List<Long> nodeIdsInIndex( Config config, int indexId, String value ) throws Exception
    {
        PageCache pageCache = resources.pageCache();
        File storeDir = resources.directory().graphDbDir();
        FileSystemAbstraction fs = resources.fileSystem();
        IndexProvider.Monitor monitor = IndexProvider.Monitor.EMPTY;
        OperationalMode operationalMode = OperationalMode.single;
        IndexProvider indexProvider = selectIndexProvider( pageCache, storeDir, fs, monitor, config, operationalMode );
        IndexSamplingConfig samplingConfig = new IndexSamplingConfig( config );
        try ( IndexAccessor accessor = indexProvider.getOnlineAccessor( indexId,
                SchemaIndexDescriptorFactory.forLabel( 0, 0 ), samplingConfig );
              IndexReader reader = accessor.newReader() )
        {
            return PrimitiveLongCollections.asList( reader.query( IndexQuery.exact( 1, value ) ) );
        }
    }

    private IndexProvider selectIndexProvider( PageCache pageCache, File storeDir, FileSystemAbstraction fs, IndexProvider.Monitor monitor, Config config,
            OperationalMode operationalMode )
    {
        String defaultSchemaProvider = config.get( GraphDatabaseSettings.default_schema_provider );
        RecoveryCleanupWorkCollector recoveryCleanupWorkCollector = RecoveryCleanupWorkCollector.immediate();
        if ( LUCENE10.providerName().equals( defaultSchemaProvider ) )
        {
            return LuceneIndexProviderFactory
                    .newInstance( pageCache, storeDir, fs, monitor, config, operationalMode, recoveryCleanupWorkCollector );
        }
        else if ( NATIVE10.providerName().equals( defaultSchemaProvider ) )
        {
            return NativeLuceneFusionIndexProviderFactory10
                    .create( pageCache, storeDir, fs, monitor, config, operationalMode, recoveryCleanupWorkCollector );
        }
        return NativeLuceneFusionIndexProviderFactory20
                .create( pageCache, storeDir, fs, monitor, config, operationalMode, recoveryCleanupWorkCollector );
    }
}
