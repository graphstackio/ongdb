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
package org.neo4j.cypher.internal.runtime.interpreted.commands

import org.neo4j.cypher.internal.runtime.interpreted.ExecutionContext
import org.neo4j.cypher.internal.runtime.interpreted.commands.expressions.Expression
import org.neo4j.cypher.internal.runtime.interpreted.pipes.QueryState
import org.neo4j.values.AnyValue
import org.neo4j.values.virtual.{RelationshipValue, NodeValue, PathValue, VirtualValues}

import scala.collection.mutable.ArrayBuffer

case class PathExtractorExpression(pathPattern: Seq[Pattern]) extends Expression {

  override def apply(ctx: ExecutionContext, queryState: QueryState): AnyValue = {
    def getNode(x: String): NodeValue = ctx(x).asInstanceOf[NodeValue]

    def getRel(x: String): RelationshipValue = ctx(x).asInstanceOf[RelationshipValue]

    def getPath(x: String): PathValue = ctx(x).asInstanceOf[PathValue]

    val firstNode = getFirstNode(pathPattern)

    val nodes = ArrayBuffer.empty[NodeValue]
    val rels = ArrayBuffer.empty[RelationshipValue]
    nodes.append(getNode(firstNode))
    for (path <- pathPattern) {
      path match {
        case SingleNode(name, _, _) =>
          nodes.append(getNode(name))
        case RelatedTo(_, right, relName, _, _, _) =>
          nodes.append(getNode(right.name))
          rels.append(getRel(relName))
        case path: PathPattern =>
          val p = getPath(path.pathName)
          val n = p.nodes()
          if (n.head == nodes.last) {
            nodes.append(n: _*)
            rels.append(p.relationships(): _*)
          } else {
            nodes.append(n.reverse: _*)
            rels.append(p.relationships().reverse: _*)
          }
      }
    }
    VirtualValues.path(nodes.toArray, rels.toArray)
  }

  private def getFirstNode(pathPattern: Seq[Pattern]): String =
    pathPattern.head match {
      case RelatedTo(left, _, _, _, _, _) => left.name
      case SingleNode(name, _, _) => name
      case path: PathPattern => path.left.name
    }

  override def rewrite(f: (Expression) => Expression) = f(this)

  override def arguments = Seq.empty

  override def symbolTableDependencies =
    pathPattern.flatMap(_.possibleStartPoints).map(_._1).toSet
}
