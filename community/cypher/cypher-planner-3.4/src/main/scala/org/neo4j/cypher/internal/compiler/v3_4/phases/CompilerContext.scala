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
package org.neo4j.cypher.internal.compiler.v3_4.phases

import java.time.Clock

import org.neo4j.cypher.internal.util.v3_4.{CypherException, InputPosition}
import org.neo4j.cypher.internal.compiler.v3_4._
import org.neo4j.cypher.internal.compiler.v3_4.planner.logical.{Metrics, QueryGraphSolver}
import org.neo4j.cypher.internal.frontend.v3_4.phases.{BaseContext, CompilationPhaseTracer, InternalNotificationLogger, Monitors}
import org.neo4j.cypher.internal.frontend.v3_4.semantics.SemanticErrorDef
import org.neo4j.cypher.internal.planner.v3_4.spi.PlanContext
import org.neo4j.cypher.internal.util.v3_4.attribution.IdGen

class CompilerContext(val exceptionCreator: (String, InputPosition) => CypherException,
                      val tracer: CompilationPhaseTracer,
                      val notificationLogger: InternalNotificationLogger,
                      val planContext: PlanContext,
                      val monitors: Monitors,
                      val metrics: Metrics,
                      val config: CypherCompilerConfiguration,
                      val queryGraphSolver: QueryGraphSolver,
                      val updateStrategy: UpdateStrategy,
                      val debugOptions: Set[String],
                      val clock: Clock,
                      val logicalPlanIdGen: IdGen) extends BaseContext {

  override def errorHandler =
    (errors: Seq[SemanticErrorDef]) => errors.foreach(e => throw exceptionCreator(e.msg, e.position))

}
