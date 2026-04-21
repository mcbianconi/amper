/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.diagnostics

import org.jetbrains.amper.frontend.SchemaBundle
import org.jetbrains.amper.frontend.asBuildProblemSource
import org.jetbrains.amper.frontend.tree.KeyValue
import org.jetbrains.amper.frontend.tree.resolution.renderTypeOf
import org.jetbrains.amper.frontend.types.SchemaObjectDeclaration
import org.jetbrains.amper.problems.reporting.BuildProblem
import org.jetbrains.amper.problems.reporting.BuildProblemSource
import org.jetbrains.amper.problems.reporting.BuildProblemType
import org.jetbrains.amper.problems.reporting.Level
import org.jetbrains.annotations.Nls

/**
 * Unknown property in a type that comes from a plugin.
 */
class UnknownPropertyInUserType(
    val declaration: SchemaObjectDeclaration,
    val extraProperty: KeyValue,
) : BuildProblem {
    override val level get() = Level.Error
    override val type get() = BuildProblemType.UnknownSymbol
    override val diagnosticId get() = FrontendDiagnosticId.UnknownPropertyInUserControlledType

    override val source: BuildProblemSource
        get() = extraProperty.keyTrace.asBuildProblemSource()

    override val message: @Nls String
        get() = SchemaBundle.message(
            "unknown.property.in.user.type",
            extraProperty.key,
            renderTypeOf(extraProperty.value),
            declaration.displayName,
        )
}
