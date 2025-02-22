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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.Issue

interface RemoveUnusedImportsTest : JavaRecipeTest {

    override val recipe
        get() = RemoveUnusedImports()

    @Test
    fun removeNamedImport(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.List;
            class A {}
        """,
        after = "class A {}"
    )

    @Test
    fun leaveImportIfRemovedTypeIsStillReferredTo(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.List;
            class A {
               List<Integer> list;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/617")
    @Test
    fun leaveImportForStaticImportEnumInAnnotation(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf("""
            package org.openrewrite.test;
            
            public @interface YesOrNo {
                Status status();
                enum Status {
                    YES, NO
                }
            }
        """),
        before = """
            package org.openrewrite.test;
            
            import static org.openrewrite.test.YesOrNo.Status.YES;
            
            @YesOrNo(status = YES)
            public class Foo {}
        """
    )

    @Test
    fun removeStarImportIfNoTypesReferredTo(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.*;
            class A {}
        """,
        after = "class A {}"
    )

    @Test
    fun replaceStarImportWithNamedImportIfOnlyOneReferencedTypeRemains(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.*;
            
            class A {
                Collection<Integer> c;
            }
        """,
        after = """
            import java.util.Collection;
            
            class A {
                Collection<Integer> c;
            }
        """
    )

    @Test
    fun leaveStarImportInPlaceIfThreeOrMoreTypesStillReferredTo(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.*;
            class A {
               Collection<Integer> c;
               Set<Integer> s = new HashSet<>();
            }
        """
    )

    @Test
    fun removeStarStaticImport(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import static java.util.Collections.*;
            class A {}
        """,
        after = "class A {}"
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/687")
    @Test
    fun leaveStarStaticImportIfReferenceStillExists(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import static java.util.Collections.*;
            class A {
               Object o = emptyList();
            }
        """,
        after = """
            import static java.util.Collections.emptyList;
            class A {
               Object o = emptyList();
            }
        """.trimIndent()
    )

    @Test
    fun removeStaticImportIfNotReferenced(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.time.DayOfWeek;
            
            import static java.time.DayOfWeek.MONDAY;
            import static java.time.DayOfWeek.TUESDAY;
            
            class WorkWeek {
                DayOfWeek shortWeekStarts(){
                    return TUESDAY;
                }
            }
        """,
        after = """
            import java.time.DayOfWeek;
            
            import static java.time.DayOfWeek.TUESDAY;
            
            class WorkWeek {
                DayOfWeek shortWeekStarts(){
                    return TUESDAY;
                }
            }
        """
    )

    @Test
    fun leaveNamedStaticImportIfReferenceStillExists(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import static java.util.Collections.emptyList;
            import static java.util.Collections.emptySet;
            
            class A {
               Object o = emptyList();
            }
        """,
        after = """
            import static java.util.Collections.emptyList;
            
            class A {
               Object o = emptyList();
            }
        """
    )

    @Test
    fun leaveNamedStaticImportOnFieldIfReferenceStillExists(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package foo;
                public class B {
                    public static final String STRING = "string";
                    public static final String STRING2 = "string2";
                }
            """,
            """
                package foo;
                public class C {
                    public static final String ANOTHER = "string";
                }
            """
        ),
        before = """
            import static foo.B.STRING;
            import static foo.B.STRING2;
            import static foo.C.*;
            
            public class A {
                String a = STRING;
            }
        """,
        after = """
            import static foo.B.STRING;
            
            public class A {
                String a = STRING;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/429")
    @Test
    fun removePackageInfoImports(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package foo;
                public @interface FooAnnotation {}
                public @interface Foo {}
                public @interface Bar {}
            """
        ),
        before = """
            @Foo
            @Bar
            package foo.bar.baz;
            
            import foo.Bar;
            import foo.Foo;
            import foo.FooAnnotation;
        """,
        after = """
            @Foo
            @Bar
            package foo.bar.baz;
            
            import foo.Bar;
            import foo.Foo;
        """
    )
    @Test
    fun removePackageInfoStarImports(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package foo;
                public @interface FooAnnotation {}
                public @interface Foo {}
                public @interface Bar {}
            """
        ),
        before = """
            @Foo
            @Bar
            package foo.bar.baz;
            
            import foo.*;
        """,
        after = """
            @Foo
            @Bar
            package foo.bar.baz;
            
            import foo.Bar;
            import foo.Foo;
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/594")
    @Test
    fun dontRemoveStaticReferenceToPrimitiveField(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
            public class A {
                int t = TYPE_FORWARD_ONLY;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/580")
    @Test
    fun resultSetType(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.sql.ResultSet;
            public class A {
                int t = ResultSet.TYPE_FORWARD_ONLY;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/701")
    @Test
    fun ensuresWhitespaceAfterPackageDeclarationNoImportsRemain(jp: JavaParser) = assertChanged(
        jp,
        before = """
            package com.example.foo;
            import java.util.List;
            public class A {
            }
        """,
        after = """
            package com.example.foo;
            
            public class A {
            }
        """
    )

    @Test
    fun doesNotAffectClassBodyFormatting(jp: JavaParser) = assertChanged(
        jp,
        before = """
            package com.example.foo;
            
            import java.util.List;
            import java.util.ArrayList;
            
            public class A {
            // Intentionally misaligned to ensure formatting is not overzealous
            ArrayList<String> foo = new ArrayList<>();
            }
        """,
        after = """
            package com.example.foo;
            
            import java.util.ArrayList;
            
            public class A {
            // Intentionally misaligned to ensure formatting is not overzealous
            ArrayList<String> foo = new ArrayList<>();
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/845")
    @Test
    fun doesNotRemoveStaticReferenceToNewClass() = assertUnchanged(
        dependsOn = arrayOf("""
            package org.openrewrite;
            public class Bar {
                public static final class Buz {
                    public Buz() {}
                }
            }
        """),
        before = """
            package foo.test;

            import static org.openrewrite.Bar.Buz;

            public class Test {
                private void method() {
                    new Buz();
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/877")
    @Test
    fun doNotUnfoldStaticValidWildCard() = assertUnchanged(
        dependsOn = arrayOf("""
            package org.openrewrite;
            public class Foo {
                public static final int FOO_CONSTANT = 10;
                public static final class Bar {
                    private Bar() {}
                    public static void helper() {}
                }
                public static void fooMethod() {}
            }
        """),
        before = """
            package foo.test;
            
            import static org.openrewrite.Foo.*;
            
            public class Test {
                int val = FOO_CONSTANT;
                private void method() {
                    fooMethod();
                    Bar.helper();
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/877")
    @Test
    fun unfoldStaticUses() = assertChanged(
        dependsOn = arrayOf("""
            package org.openrewrite;
            public class Foo {
                public static final int FOO_CONSTANT = 10;
                public static final class Bar {
                    private Bar(){}
                    public static void helper() {}
                }
                public static void fooMethod() {}
            }
        """),
        before = """
            package foo.test;
            
            import static org.openrewrite.Foo.*;
            
            public class Test {
                int val = FOO_CONSTANT;
                private void method() {
                    Bar.helper();
                }
            }
        """,
        after = """
            package foo.test;
            
            import static org.openrewrite.Foo.FOO_CONSTANT;
            import static org.openrewrite.Foo.Bar;
            
            public class Test {
                int val = FOO_CONSTANT;
                private void method() {
                    Bar.helper();
                }
            }
        """
    )
}
