/*
 * Copyright (c) 2018-2020 "Graph Foundation,"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * This file is part of ONgDB Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) as found
 * in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.ha.cluster.modeswitch;

import org.junit.Test;

import org.neo4j.kernel.ha.DelegateInvocationHandler;
import org.neo4j.kernel.ha.lock.SlaveStatementLocksFactory;
import org.neo4j.kernel.impl.locking.StatementLocksFactory;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class StatementLocksFactorySwitcherTest
{

    private StatementLocksFactory configuredLockFactory = mock( StatementLocksFactory.class );

    @Test
    public void masterStatementLocks()
    {
        StatementLocksFactorySwitcher switcher = getLocksSwitcher();
        StatementLocksFactory masterLocks = switcher.getMasterImpl();
        assertSame( masterLocks, configuredLockFactory );
    }

    @Test
    public void slaveStatementLocks()
    {
        StatementLocksFactorySwitcher switcher = getLocksSwitcher();
        StatementLocksFactory slaveLocks = switcher.getSlaveImpl();
        assertThat( slaveLocks, instanceOf( SlaveStatementLocksFactory.class ) );
    }

    private StatementLocksFactorySwitcher getLocksSwitcher()
    {
        DelegateInvocationHandler invocationHandler = mock( DelegateInvocationHandler.class );
        return new StatementLocksFactorySwitcher( invocationHandler, configuredLockFactory );
    }
}
