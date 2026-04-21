/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.tree

import org.jetbrains.amper.frontend.asBuildProblemSource
import org.jetbrains.amper.frontend.diagnostics.UnknownPluginId
import org.jetbrains.amper.frontend.diagnostics.UnknownProperty
import org.jetbrains.amper.frontend.diagnostics.UnknownPropertyInUserType
import org.jetbrains.amper.frontend.types.PluginsSettingsBlockDeclaration
import org.jetbrains.amper.frontend.types.SchemaOrigin
import org.jetbrains.amper.problems.reporting.ProblemReporter

/**
 * Automatically called in [org.jetbrains.amper.frontend.tree.reading.readTree], unless
 * [org.jetbrains.amper.frontend.tree.reading.UnknownPropertiesParsingMode] is set to disable it.
 */
context(reporter: ProblemReporter)
fun diagnoseUnknownProperties(tree: TreeNode) {
    object : RecurringTreeVisitorUnit() {
        override fun visitMap(node: MappingNode) {
            super.visitMap(node)
            val declaration = node.declaration ?: return

            node.children.filter { it.propertyDeclaration == null }.map { keyValue ->
                when {
                    declaration.origin is SchemaOrigin.LocalPlugin -> {
                        UnknownPropertyInUserType(
                            declaration = declaration,
                            extraProperty = keyValue,
                        )
                    }
                    // Maybe: if the local plugin is present in the project but not enabled,
                    //  issue a more concrete message.
                    declaration is PluginsSettingsBlockDeclaration -> UnknownPluginId(
                        pluginIdKeyValue = keyValue,
                    )
                    else -> UnknownProperty(
                        invalidName = keyValue.key,
                        inside = declaration,
                        source = keyValue.keyTrace.asBuildProblemSource(),
                    )
                }
            }.forEach(reporter::reportMessage)
        }
    }.visit(tree)
}
