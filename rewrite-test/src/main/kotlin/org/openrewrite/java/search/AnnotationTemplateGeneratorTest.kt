/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.search

import com.google.common.io.CharSink
import com.google.common.io.CharSource
import com.google.googlejavaformat.java.Formatter
import com.google.googlejavaformat.java.JavaFormatterOptions
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.internal.template.AnnotationTemplateGenerator
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

interface AnnotationTemplateGeneratorTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/653")
    @Test
    fun innerClass(jp: JavaParser) {
        val cu = jp.parse(
            """
            class Outer {
                class Inner {
                    void test() {
                        assert n == 0;
                    }
                }
            }
        """)[0]

        @Language("java")
        val expected = """
            class Outer {
                class Inner {
                    void test() {
                        @${'$'}Placeholder /*__TEMPLATE__*/ String s = "Annotate me";
                    }
                }
            }
            
            @interface ${'$'}Placeholder {}
        """.trimIndent()

        assertThat(beforeAssert(cu)).isEqualTo(expected)
    }

    @Test
    fun generateTemplate(jp: JavaParser) {
        val cu = jp.parse(
            """
                package org.openrewrite;
                import java.io.FileInputStream;
                import java.util.function.Function;
                class Outer {
                    static final String ALL = "ALL";
                    void outer(int p1) {
                        int n2 = 1;
                        for(int index;;) {
                        }
                        try(FileInputStream f = new FileInputStream("")) {
                            for(int a : Arrays.asList(0, 1)) {
                                for(int index; index < 100; index++) {
                                    Function<Integer, Function<Integer, Object>> o = 
                                            (p2) -> {
                                                Function<Integer, Object> o2 = 
                                                        (Integer p3) ->
                                                            new Object() {
                                                                void inner(int p4) {
                                                                    assert n == 0;
                                                                }
                                                            };
                                                return o2;
                                            };
                                }
                                int n3;
                            }
                        }
                        int n4;
                    }
                    
                    int o;
                    int m() {
                        return 0;
                    }
                    
                    private class Inner {
                        void m2() {}
                    }
                }
            """
        )[0]

        @Language("java")
        val expected = """
            package org.openrewrite;

            import java.util.function.Function;
            import java.io.FileInputStream;
            
            class Outer {
                private class Inner {}
            
                static final String ALL;
            
                void outer(int p1) {
                    int n2;
                    new Object() {
                        void inner(int p4) {
                            @${'$'}Placeholder /*__TEMPLATE__*/ String s = "Annotate me";
                        }
                    };
                }
            }
            
            @interface ${'$'}Placeholder {}
        """.trimIndent()

        assertThat(beforeAssert(cu)).isEqualTo(expected)
    }

    private fun beforeAssert(cu: J.CompilationUnit): String {
        val s = StringBuilder()
        object : JavaVisitor<StringBuilder>() {
            override fun visitAssert(assert: J.Assert, p: StringBuilder): J {
                p.append(
                    AnnotationTemplateGenerator(emptySet())
                        .template(cursor, "String s = \"Annotate me\";")
                )
                return assert
            }
        }.visit(cu, s)
        return format(s.toString())
    }

    private fun format(s: String): String {
        val bos = ByteArrayOutputStream()
        Formatter(JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.AOSP).build())
            .formatSource(CharSource.wrap(s), object : CharSink() {
                override fun openStream() = OutputStreamWriter(bos)
            })
        return String(bos.toByteArray()).trim()
    }
}
