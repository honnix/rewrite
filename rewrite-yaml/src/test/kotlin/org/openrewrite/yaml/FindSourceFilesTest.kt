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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.FindSourceFiles
import org.openrewrite.Recipe
import org.openrewrite.text.PlainText
import org.openrewrite.text.PlainTextParser
import java.nio.file.Path

class FindSourceFilesTest : YamlRecipeTest {
    override val recipe: Recipe
        get() = FindSourceFiles("**/hello.yml")

    @Test
    fun findMatchingFile(@TempDir tempDir: Path) = assertChangedBase(
        before = tempDir.resolve("a/b/hello.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("key: value")
        }.toFile(),
        after = "~~>key: value"
    )
}
