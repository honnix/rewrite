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
import org.openrewrite.marker.Markers;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeValue extends Recipe {
    @Option(displayName = "Key path",
            description = "A JsonPath expression to locate a JSON entry.",
            example = "$.subjects.kind")
    String oldKeyPath;

    @Option(displayName = "New value",
            description = "The new value to set for the key identified by oldKeyPath.",
            example = "'Deployment'")
    String value;

    @Incubating(since = "7.11.0")
    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/application-*.json")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Change value";
    }

    @Override
    public String getDescription() {
        return "Change a JSON mapping entry value leaving the key intact.";
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
                if (matcher.matches(getCursor()) && (!(m.getValue() instanceof Json.Literal) || !((Json.Literal) m.getValue()).getValue().equals(value))) {
                    String source = ChangeValue.this.value;
                    if(source.startsWith("'") || source.startsWith("\"")) {
                        source = source.substring(1, source.length() - 1);
                    }
                    if(!(m.getValue() instanceof Json.Literal) || !((Json.Literal) m.getValue()).getSource().equals(ChangeValue.this.value)) {
                        m = m.withValue(new Json.Literal(randomId(), m.getValue().getPrefix(), Markers.EMPTY, ChangeValue.this.value, source));
                    }
                }
                return m;
            }
        };
    }
}
