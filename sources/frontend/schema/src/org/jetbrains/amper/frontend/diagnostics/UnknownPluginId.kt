/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.diagnostics

import org.jetbrains.amper.frontend.SchemaBundle
import org.jetbrains.amper.frontend.asBuildProblemSource
import org.jetbrains.amper.frontend.tree.KeyValue
import org.jetbrains.amper.problems.reporting.BuildProblem
import org.jetbrains.amper.problems.reporting.BuildProblemSource
import org.jetbrains.amper.problems.reporting.BuildProblemType
import org.jetbrains.amper.problems.reporting.Level
import org.jetbrains.annotations.Nls

/**
 * Unknown plugin id in `plugins:` block in `module.yaml`/template.
 */
class UnknownPluginId(
    val pluginIdKeyValue: KeyValue,
) : BuildProblem {
    val unknownPluginId: String
        get() = pluginIdKeyValue.key

    override val source: BuildProblemSource
        get() = pluginIdKeyValue.keyTrace.asBuildProblemSource()

    override val level get() = Level.Error
    override val type get() = BuildProblemType.UnknownSymbol
    override val diagnosticId get() = FrontendDiagnosticId.UnknownPluginId
    override val message: @Nls String
        get() = SchemaBundle.message("unknown.plugin.id", unknownPluginId)
}
