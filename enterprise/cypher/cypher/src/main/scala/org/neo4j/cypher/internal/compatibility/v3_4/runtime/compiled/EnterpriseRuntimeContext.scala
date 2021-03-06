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
package org.neo4j.cypher.internal.compatibility.v3_4.runtime.compiled

import java.time.Clock

import org.neo4j.cypher.internal.compatibility.v3_4.runtime.CommunityRuntimeContext
import org.neo4j.cypher.internal.compatibility.v3_4.runtime.compiled.codegen.spi.CodeStructure
import org.neo4j.cypher.internal.compiler.v3_4.planner.logical.{ExpressionEvaluator, Metrics, MetricsFactory, QueryGraphSolver}
import org.neo4j.cypher.internal.compiler.v3_4.{ContextCreator, CypherCompilerConfiguration, SyntaxExceptionCreator, UpdateStrategy}
import org.neo4j.cypher.internal.frontend.v3_4.phases.{CompilationPhaseTracer, InternalNotificationLogger, Monitors}
import org.neo4j.cypher.internal.planner.v3_4.spi.PlanContext
import org.neo4j.cypher.internal.runtime.vectorized.dispatcher.Dispatcher
import org.neo4j.cypher.internal.util.v3_4.attribution.IdGen
import org.neo4j.cypher.internal.util.v3_4.{CypherException, InputPosition}
import org.neo4j.cypher.internal.v3_4.executionplan.GeneratedQuery

class EnterpriseRuntimeContext(override val exceptionCreator: (String, InputPosition) => CypherException,
                               override val tracer: CompilationPhaseTracer,
                               override val notificationLogger: InternalNotificationLogger,
                               override val planContext: PlanContext,
                               override val monitors: Monitors,
                               override val metrics: Metrics,
                               override val config: CypherCompilerConfiguration,
                               override val queryGraphSolver: QueryGraphSolver,
                               override val updateStrategy: UpdateStrategy,
                               override val debugOptions: Set[String],
                               override val clock: Clock,
                               override val logicalPlanIdGen: IdGen,
                               val codeStructure: CodeStructure[GeneratedQuery],
                               val dispatcher: Dispatcher)
  extends CommunityRuntimeContext(exceptionCreator, tracer,
                                  notificationLogger, planContext, monitors, metrics,
                                  config, queryGraphSolver, updateStrategy, debugOptions, clock, logicalPlanIdGen)

case class EnterpriseRuntimeContextCreator(codeStructure: CodeStructure[GeneratedQuery], dispatcher: Dispatcher) extends ContextCreator[EnterpriseRuntimeContext] {

  override def create(tracer: CompilationPhaseTracer,
                      notificationLogger: InternalNotificationLogger,
                      planContext: PlanContext,
                      queryText: String,
                      debugOptions: Set[String],
                      offset: Option[InputPosition],
                      monitors: Monitors,
                      metricsFactory: MetricsFactory,
                      queryGraphSolver: QueryGraphSolver,
                      config: CypherCompilerConfiguration,
                      updateStrategy: UpdateStrategy,
                      clock: Clock,
                      logicalPlanIdGen: IdGen,
                      evaluator: ExpressionEvaluator): EnterpriseRuntimeContext = {
    val exceptionCreator = new SyntaxExceptionCreator(queryText, offset)

    val metrics: Metrics = if (planContext == null)
      null
    else
      metricsFactory.newMetrics(planContext.statistics, evaluator, config)

    new EnterpriseRuntimeContext(exceptionCreator, tracer, notificationLogger, planContext,
                                monitors, metrics, config, queryGraphSolver, updateStrategy, debugOptions, clock, logicalPlanIdGen, codeStructure, dispatcher)
  }
}
