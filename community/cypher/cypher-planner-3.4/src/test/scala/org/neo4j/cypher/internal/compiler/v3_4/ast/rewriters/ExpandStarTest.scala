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
package org.neo4j.cypher.internal.compiler.v3_4.ast.rewriters

import org.neo4j.cypher.internal.compiler.v3_4._
import org.neo4j.cypher.internal.frontend.v3_4.ast._
import org.neo4j.cypher.internal.frontend.v3_4.ast.rewriters.{expandStar, normalizeReturnClauses, normalizeWithClauses}
import org.neo4j.cypher.internal.frontend.v3_4.semantics.SemanticState
import org.neo4j.cypher.internal.util.v3_4.inSequence
import org.neo4j.cypher.internal.util.v3_4.test_helpers.CypherFunSuite

class ExpandStarTest extends CypherFunSuite with AstConstructionTestSupport {

  import parser.ParserFixture.parser

  test("rewrites * in return") {
    assertRewrite(
      "match (n) return *",
      "match (n) return n")

    assertRewrite(
      "match (n),(c) return *",
      "match (n),(c) return c,n")

    assertRewrite(
      "match (n)-->(c) return *",
      "match (n)-->(c) return c,n")

    assertRewrite(
      "match (n)-[r]->(c) return *",
      "match (n)-[r]->(c) return c,n,r")

    assertRewrite(
      "create (n) return *",
      "create (n) return n")

    assertRewrite(
      "match p = shortestPath((a)-[r*]->(x)) return *",
      "match p = shortestPath((a)-[r*]->(x)) return a,p,r,x")

    assertRewrite(
      "match p=(a:Start)-->(b) return *",
      "match p=(a:Start)-->(b) return a, b, p")
  }

  test("rewrites * in with") {
    assertRewrite(
      "match (n) with * return n",
      "match (n) with n return n")

    assertRewrite(
      "match (n),(c) with * return n",
      "match (n),(c) with c,n return n")

    assertRewrite(
      "match (n)-->(c) with * return n",
      "match (n)-->(c) with c,n return n")

    assertRewrite(
      "match (n)-[r]->(c) with * return n",
      "match (n)-[r]->(c) with c,n,r return n")

    assertRewrite(
      "match (n)-[r]->(c) with *, r.pi as x return n",
      "match (n)-[r]->(c) with c, n, r, r.pi as x return n")

    assertRewrite(
      "create (n) with * return n",
      "create (n) with n return n")

    assertRewrite(
      "match p = shortestPath((a)-[r*]->(x)) with * return p",
      "match p = shortestPath((a)-[r*]->(x)) with a,p,r,x return p")
  }

  test("symbol shadowing should be taken into account") {
    assertRewrite(
      "match a,x,y with a match (b) return *",
      "match a,x,y with a match (b) return a, b")
  }

  test("expands _PRAGMA WITHOUT") {
    assertRewrite(
      "MATCH a,x,y _PRAGMA WITHOUT a MATCH b RETURN *",
      "MATCH a,x,y WITH x, y MATCH b RETURN b, x, y")
  }

  test("keeps listed items during expand") {
    assertRewrite(
      "MATCH (n) WITH *, 1 AS b RETURN *",
      "MATCH (n) WITH n, 1 AS b RETURN b, n"
    )
  }

  private def assertRewrite(originalQuery: String, expectedQuery: String) {
    val original = prepRewrite(originalQuery)
    val expected = prepRewrite(expectedQuery)

    val checkResult = original.semanticCheck(SemanticState.clean)
    val rewriter = expandStar(checkResult.state)

    val result = original.rewrite(rewriter)
    assert(result === expected)
  }

  private def prepRewrite(q: String) = {
    val mkException = new SyntaxExceptionCreator(q, Some(pos))
    val rewriter = inSequence(normalizeReturnClauses(mkException), normalizeWithClauses(mkException))
    parser.parse(q).endoRewrite(rewriter)
  }
}
