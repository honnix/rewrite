/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.Parser
import org.openrewrite.firstMethodStatement

open class EmptyTest : Parser() {
    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                public void test() {
                    ;
                }
            }
        """)
    }

    @Test
    fun empty() {
        assertTrue(a.firstMethodStatement() is J.Empty)
    }

    @Test
    fun format() {
        assertEquals("""
            |public void test() {
            |    ;
            |}
        """.trimMargin(), a.classes[0].methods[0].printTrimmed())
    }
}