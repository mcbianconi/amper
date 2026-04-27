/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.schema.processing

import org.jetbrains.amper.plugins.schema.model.InputOutputMark
import org.jetbrains.amper.plugins.schema.model.PluginData
import org.jetbrains.amper.plugins.schema.model.diagnostics.KotlinSchemaBuildProblem
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.psi
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor

context(session: KaSession, _: DiagnosticsReporter, _: SymbolsCollector, options: ParsingOptions)
internal fun parseProperty(
    schemaDeclaration: KtClassOrObject,
    property: KtProperty,
): PluginData.ClassData.Property? {
    val name = property.name ?: return null // invalid Kotlin
    val nameIdentifier = property.nameIdentifier ?: return null // invalid Kotlin

    property.overrideModifier()?.let {
        report(it, KotlinSchemaBuildProblem::ForbiddenPropertyOverride)
    }
    property.extensionReceiver()?.let {
        report(it, KotlinSchemaBuildProblem::ForbiddenPropertyExtension)
    }
    if (property.isVar) {
        report(property.valOrVarKeyword, KotlinSchemaBuildProblem::ForbiddenPropertyMutable)
    }

    val type = with(session) {
        property.returnType
    }.parseSchemaType(origin = { property.typeReference ?: property })

    val default = with(session) { property.symbol as KaPropertySymbol }.getter?.let { getter ->
        getter.psiSafe<KtPropertyAccessor>()?.let { psi ->
            if (psi.bodyBlockExpression != null) {
                report(getter.psi(), KotlinSchemaBuildProblem::DefaultsInvalidGetterBlock); null
            } else if (type != null) psi.bodyExpression?.let { expression ->
                parseDefaultExpression(expression, type)
            } else null
        }
    }

    if (type == null) return null

    val internal = if (options.isParsingAmperApi) {
        PluginData.ClassData.InternalPropertyAttributes(
            isProvided = property.isAnnotatedWith(PROVIDED_ANNOTATION_CLASS),
            isShorthand = property.isAnnotatedWith(SHORTHAND_ANNOTATION_CLASS),
            isDependencyNotation = schemaDeclaration.getClassId() == LOCAL_DEPENDENCY_CLASS,
        )
    } else null

    return PluginData.ClassData.Property(
        name = name,
        type = type,
        default = default,
        doc = property.getDefaultDocString(),
        origin = nameIdentifier.getSourceLocation(),
        internalAttributes = internal,
        inputOutputMark = InputOutputMark.ValueOnly.takeIf {
            options.isParsingAmperApi && property.isAnnotatedWith(PATH_VALUE_ONLY_ANNOTATION_CLASS)
        },
    )
}
