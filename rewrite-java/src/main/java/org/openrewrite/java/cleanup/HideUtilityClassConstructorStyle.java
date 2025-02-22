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
package org.openrewrite.java.cleanup;

import lombok.Value;
import lombok.With;
import org.openrewrite.Incubating;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaStyle;

import java.util.Collection;

@Incubating(since = "7.0.0")
@Value
@With
public class HideUtilityClassConstructorStyle implements JavaStyle {
    /**
     * If any of the annotation signatures are present on the utility class, the visitor will ignore operating on the class.
     * These should be {@link AnnotationMatcher}-compatible, fully-qualified annotation signature strings.
     */
    Collection<String> ignoreIfAnnotatedBy;
}
