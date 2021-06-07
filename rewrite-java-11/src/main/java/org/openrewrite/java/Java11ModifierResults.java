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
package org.openrewrite.java;

import org.openrewrite.java.tree.J;

import java.util.List;

public final class Java11ModifierResults {

    private final List<J.Annotation> leadingAnnotations;
    private final List<J.Modifier> modifiers;

    public Java11ModifierResults(List<J.Annotation> leadingAnnotations, List<J.Modifier> modifiers) {
        this.leadingAnnotations = leadingAnnotations;
        this.modifiers = modifiers;
    }

    public List<J.Annotation> getLeadingAnnotations() {
        return leadingAnnotations;
    }

    public List<J.Modifier> getModifiers() {
        return modifiers;
    }
}
