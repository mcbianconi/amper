/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.diagnostics

import org.jetbrains.amper.frontend.SchemaBundle
import org.jetbrains.amper.frontend.types.SchemaObjectDeclaration
import org.jetbrains.amper.problems.reporting.BuildProblem
import org.jetbrains.amper.problems.reporting.BuildProblemSource
import org.jetbrains.amper.problems.reporting.BuildProblemType
import org.jetbrains.amper.problems.reporting.DiagnosticId
import org.jetbrains.amper.problems.reporting.Level
import org.jetbrains.annotations.Nls

/**
 * Reported on unknown property names in any object.
 */
class UnknownProperty(
    val invalidName: String,
    val inside: SchemaObjectDeclaration,
    override val source: BuildProblemSource,
) : BuildProblem {
    val possibleIntendedNames: List<String> = inside.properties
        .filter { invalidName in it.misnomers }
        .map { it.name }

    override val level get() = Level.Error
    override val type get() = BuildProblemType.UnknownSymbol
    override val diagnosticId: DiagnosticId = FrontendDiagnosticId.UnknownProperty
    override val message: @Nls String
        get() = if (possibleIntendedNames.isEmpty()) {
            SchemaBundle.message("unknown.property", invalidName)
        } else {
            SchemaBundle.message(
                "unknown.property.did.you.mean",
                invalidName,
                // repeated ORs are ok: we have maximum 2 props with identical misnomers in the same type for now
                possibleIntendedNames.joinToString(" or ") { "'$it'" },
            )
        }
}
