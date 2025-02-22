/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.json;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.json.tree.Json;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeKey extends Recipe {
    @Option(displayName = "Old key path",
            description = "A JsonPath expression to locate a JSON entry.",
            example = "subjects/kind")
    String oldKeyPath;

    @Option(displayName = "New key",
            description = "The new name for the key selected by oldKeyPath.",
            example = "kind")
    String newKey;

    @Incubating(since = "7.11.0")
    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/application-*.json")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Change key";
    }

    @Override
    public String getDescription() {
        return "Change a JSON mapping entry key leaving the value intact.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    public JsonVisitor<ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(oldKeyPath);
        return new JsonIsoVisitor<ExecutionContext>() {
            @Override
            public Json.Member visitMember(Json.Member member, ExecutionContext executionContext) {
                Json.Member m = super.visitMember(member, executionContext);
                if (matcher.matches(getCursor())) {
                    String value = ChangeKey.this.newKey;
                    if(value.startsWith("'") || value.startsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    m = m.withKey(
                            m.getKey() instanceof Json.Literal ?
                                    ((Json.Literal) m.getKey())
                                            .withSource(newKey)
                                            .withValue(value) :
                                    new Json.Literal(Tree.randomId(), m.getKey().getPrefix(), m.getKey().getMarkers(),
                                            newKey, value)
                    );
                }
                return m;
            }
        };
    }
}
