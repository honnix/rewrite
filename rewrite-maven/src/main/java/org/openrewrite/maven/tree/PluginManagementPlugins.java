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
package org.openrewrite.maven.tree;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;

/**
 * Plugin management sections contain a combination ...
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
// TODO: update name ...
public interface PluginManagementPlugins {
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    class Defined implements PluginManagementPlugins,
            PluginDescriptor {
        String groupId;
        String artifactId;

        @Nullable
        String version;

        String requestedVersion;

        Set<Execution> executions;

        @Nullable
        Set<Pom.Dependency> dependencies;

        // TODO: investigate dependency resolution in plugins.
        // NOTES: plugins can have dependencies, it seems like in `most` cases the dependencies may be inherited.
        // The dependencies and properties may already be processed before processing plugins, which may help.
        // Caveat - order will matter. process pluginManagement/plugins should happen after dependencies.
        // The parent structure and inherited versions may be tricky, but seems like it's been thought about through dependencyManagement.
        // However, there may be slight differences in rules.

        @Override
        public List<PluginDescriptor> getPlugins() {
            return Collections.singletonList(this);
        }

        @Override
        public Map<String, String> getProperties() {
            return emptyMap();
        }
    }

    /**
     * @return A list of managed plugins in order of precedence.
     */
    List<PluginDescriptor> getPlugins();

    /**
     * @return A map of properties inherited from import-scope BOMs defined as
     * dependencyManagement dependencies.
     */
    Map<String, String> getProperties();

    String getGroupId();

    String getArtifactId();

    String getVersion();

    String getRequestedVersion();
}
