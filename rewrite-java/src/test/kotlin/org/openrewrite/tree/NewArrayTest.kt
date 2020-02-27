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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.Parser
import org.openrewrite.asArray

open class NewArrayTest : Parser() {
    
    @Test
    fun newArray() {
        val a = parse("""
            public class A {
                int[] n = new int[0];
            }
        """)
        
        val newArr = a.classes[0].fields[0].vars[0].initializer as J.NewArray
        assertNull(newArr.initializer)
        assertTrue(newArr.type is Type.Array)
        assertTrue(newArr.type.asArray()?.elemType is Type.Primitive)
        assertEquals(1, newArr.dimensions.size)
        assertTrue(newArr.dimensions[0].size is J.Literal)
    }

    @Test
    fun newArrayWithInitializers() {
        val a = parse("""
            public class A {
                int[] n = new int[] { 0, 1, 2 };
            }
        """)

        val newArr = a.classes[0].fields[0].vars[0].initializer as J.NewArray
        assertTrue(newArr.dimensions[0].size is J.Empty)
        assertTrue(newArr.type is Type.Array)
        assertTrue(newArr.type.asArray()?.elemType is Type.Primitive)
        assertEquals(3, newArr.initializer?.elements?.size)
    }

    @Test
    fun formatWithDimensions() {
        val a = parse("""
            public class A {
                int[][] n = new int [ 0 ] [ 1 ];
            }
        """)

        val newArr = a.classes[0].fields[0].vars[0].initializer as J.NewArray
        assertEquals("new int [ 0 ] [ 1 ]", newArr.printTrimmed())
    }

    @Test
    fun formatWithEmptyDimension() {
        val a = parse("""
            public class A {
                int[][] n = new int [ 0 ] [ ];
            }
        """)

        val newArr = a.classes[0].fields[0].vars[0].initializer as J.NewArray
        assertEquals("new int [ 0 ] [ ]", newArr.printTrimmed())
    }

    @Test
    fun formatWithInitializers() {
        val a = parse("""
            public class A {
                int[] m = new int[] { 0 };
                int[][] n = new int [ ] [ ] { m, m, m };
            }
        """)

        val newArr = a.classes[0].fields[1].vars[0].initializer as J.NewArray
        assertEquals("new int [ ] [ ] { m, m, m }", newArr.printTrimmed())
    }

    @Test
    fun newArrayShortcut() {
        val produces = """
            import java.lang.annotation.*;
            @Target({ElementType.TYPE})
            public @interface Produces {
                String[] value() default "*/*";
            }
        """

        val a = parse("""@Produces({"something"}) class A {}""", produces)
        val arr = a.classes[0].annotations[0].args!!.args[0] as J.NewArray

        assertNull(arr.typeExpr)
        assertEquals("""{"something"}""", arr.printTrimmed())
    }
}