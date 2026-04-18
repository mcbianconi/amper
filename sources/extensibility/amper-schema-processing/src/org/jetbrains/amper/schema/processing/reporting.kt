/*
 * Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.schema.processing

import com.intellij.psi.PsiElement
import org.jetbrains.amper.plugins.schema.model.SourceLocation
import org.jetbrains.amper.plugins.schema.model.diagnostics.KotlinSchemaBuildProblem

internal interface DiagnosticsReporter {
    fun report(diagnostic: KotlinSchemaBuildProblem)
}

context(reporter: DiagnosticsReporter)
internal fun report(
    where: PsiElement,
    factory: (source: SourceLocation) -> KotlinSchemaBuildProblem,
) = reporter.report(factory(where.getSourceLocation()))

context(reporter: DiagnosticsReporter)
internal fun report(diagnostic: KotlinSchemaBuildProblem) = reporter.report(diagnostic)

internal fun PsiElement.getSourceLocation() = SourceLocation(
    file = this.containingFile.virtualFile.toNioPath(),
    offsetRange = textRange.let { it.startOffset..it.endOffset },
)
