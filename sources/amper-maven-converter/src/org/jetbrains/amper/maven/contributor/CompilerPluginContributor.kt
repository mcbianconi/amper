/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.maven.contributor

import org.apache.maven.model.ConfigurationContainer
import org.apache.maven.model.Plugin
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.xml.Xpp3Dom
import org.jetbrains.amper.frontend.tree.ObjectBuilderContext
import org.jetbrains.amper.frontend.tree.add
import org.jetbrains.amper.frontend.tree.invoke
import org.jetbrains.amper.frontend.types.generated.*
import org.jetbrains.amper.maven.ProjectTreeBuilder

internal fun ProjectTreeBuilder.contributeCompilerPlugin(jarProjects: Set<MavenProject>) {
    for (project in jarProjects) {
        module(project) {
            val compilerPlugin = project.getEffectivePlugin(
                "org.apache.maven.plugins",
                "maven-compiler-plugin"
            )
            compilerPlugin?.let { contributeCompilerPlugin(it) }
        }
    }
}

private fun ProjectTreeBuilder.ModuleTreeBuilder.contributeCompilerPlugin(plugin: Plugin) {
    plugin.executions.forEach { execution ->
        when (execution.id) {
            "default-compile" -> {
                withDefaultContext {
                    settings {
                        configureCompilerExecution(execution)
                    }
                }
            }
        }
    }

    if (plugin.configuration != null) {
        withDefaultContext {
            settings {
                configureCompilerExecution(plugin)
            }
        }
    }
}

private fun ObjectBuilderContext<DeclarationOfSettings>.configureCompilerExecution(container: ConfigurationContainer) {
    val config = container.configuration
    if (config is Xpp3Dom) {
        config.children.filterNotNull().forEach { child ->
            when (child.name) {
                "compilerArgs" -> {
                    java {
                        freeCompilerArgs {
                            child.children.forEach { arg -> add(arg.value) }
                        }
                    }
                }
                "annotationProcessorPaths" -> {
                    child.children.forEach { annotationProcessorPath ->
                        java {
                            annotationProcessing {
                                processors {
                                    if (annotationProcessorPath?.name == "path") {
                                        add(DeclarationOfUnscopedExternalMavenDependency) {
                                            annotationProcessorPath.children.forEach {
                                                when (it?.name) {
                                                    "groupId" -> groupId(it.value)
                                                    "artifactId" -> artifactId(it.value)
                                                    "version" -> version(it.value)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "parameters" -> {
                    jvm {
                        storeParameterNames(child.value.toBoolean())
                    }
                }
                "release" -> {
                    jvm {
                        release(child.value.toInt())
                    }
                }
            }
        }
    }
}