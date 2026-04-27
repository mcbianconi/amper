/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package com.example

import org.jetbrains.amper.plugins.*
import kotlin.concurrent.thread
import kotlin.io.path.*
import java.nio.file.Path

@Configurable
interface PluginSettings {
    val extraClasspath: Classpath
    val version: String
}

@TaskAction
fun execute(
    @Input settings: PluginSettings,
    @Input parametrizedClasspath: Classpath,
) {
    printClasspath("extraClasspath", settings.extraClasspath)
    printClasspath("parametrizedClasspath", parametrizedClasspath)
}

private fun printClasspath(prefix: String, classpath: Classpath) {
    classpath.dependencies.forEachIndexed { index, it ->
        when (it) {
            is Dependency.Maven -> println("$prefix dependencies[$index].coordinates = ${it.groupId}:${it.artifactId}:${it.version}")
            is Dependency.Local -> println("$prefix dependencies[$index].modulePath = ${it.modulePath}")
        }
    }
}