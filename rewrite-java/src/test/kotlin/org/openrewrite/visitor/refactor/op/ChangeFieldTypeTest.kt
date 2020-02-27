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
package org.openrewrite.visitor.refactor.op

import org.openrewrite.assertRefactored
import org.junit.jupiter.api.Test
import org.openrewrite.Parser

open class ChangeFieldTypeTest : Parser() {

    @Test
    fun changeFieldType() {
        val a = parse("""
            import java.util.List;
            public class A {
               List collection;
            }
        """.trimIndent())

        val fixed = a.refactor()
                .changeFieldType(a.classes[0].findFields("java.util.List"), "java.util.Collection")
                .fix().fixed

        assertRefactored(fixed, """
            import java.util.Collection;
            
            public class A {
               Collection collection;
            }
        """)
    }
}