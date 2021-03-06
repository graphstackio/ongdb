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
package org.neo4j.cypher.internal.javacompat;

import org.neo4j.helpers.MathUtil;

/**
 * Profiler statistics for a single execution step of a Cypher query execution plan
 */
public interface ProfilerStatistics
{
    /**
     * @return number of rows processed by the associated execution step
     */
    long getRows();

    /**
     * @return number of database hits (potential disk accesses) caused by executing the associated execution step
     */
    long getDbHits();

    /**
     * @return number of page cache hits caused by executing the associated execution step
     */
    default long getPageCacheHits()
    {
        return 0;
    }

    /**
     * @return number of page cache misses caused by executing the associated execution step
     */
    default long getPageCacheMisses()
    {
        return 0;
    }

    /**
     * @return the ratio of page cache hits to total number of lookups or {@link Double#NaN} if no data is available
     */
    default double getPageCacheHitRatio()
    {
        return MathUtil.portion( getPageCacheHits(), getPageCacheMisses() );
    }
}
