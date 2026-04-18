/*
 * Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.schema.processing

import org.jetbrains.amper.plugins.schema.model.PluginData
import org.jetbrains.amper.plugins.schema.model.diagnostics.KotlinSchemaBuildProblem
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtContextReceiverList
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.KtTreeVisitor
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier

context(session: KaSession, _: DiagnosticsReporter, _: SymbolsCollector)
internal fun parseVariantDeclaration(
    variantDeclaration: KtClassOrObject,
): PluginData.VariantData {
    // WARNING: This routine parses the unreleased structures that are only allowed under Amper internal API.
    val symbol = with(session) { variantDeclaration.namedClassSymbol }
        ?: error("Not a named class: $variantDeclaration")
    return PluginData.VariantData(
        name = symbol.classId?.toSchemaName() ?: error("No name: $symbol"),
        variants = with(session) { symbol.sealedClassInheritors }.map {
            PluginData.Type.ObjectType(it.classId?.toSchemaName() ?: error("No name: $it"))
        },
        origin = variantDeclaration.getSourceLocation(),
    )
}

context(session: KaSession, _: DiagnosticsReporter, _: SymbolsCollector, options: ParsingOptions)
internal fun parseSchemaDeclaration(
    schemaDeclaration: KtClassOrObject,
    name: PluginData.SchemaName,
): PluginData.ClassData? {
    if (!schemaDeclaration.isInterface()) {
        report(schemaDeclaration.getDeclarationKeyword() ?: schemaDeclaration, KotlinSchemaBuildProblem::NotInterface)
        return null  // fatal - no need to parse further
    }
    val nameIdentifier = schemaDeclaration.nameIdentifier ?: return null // invalid Kotlin
    when (with(session) { schemaDeclaration.symbol }.visibility) {
        KaSymbolVisibility.PUBLIC, KaSymbolVisibility.UNKNOWN -> Unit // okay/ignore
        KaSymbolVisibility.LOCAL -> {
            report(schemaDeclaration.visibilityModifier() ?: nameIdentifier, KotlinSchemaBuildProblem::ForbiddenLocal)
            return null  // ignore local declarations
        }
        else -> report(schemaDeclaration.visibilityModifier() ?: nameIdentifier, KotlinSchemaBuildProblem::MustBePublic)
    }
    val isPrimarySchema = name.qualifiedName == options.pluginSettingsClassName
    val properties = buildList {
        val visitor = object : KtTreeVisitor<Nothing?>() {
            override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Nothing?): Void? {
                return null  // Stop here to not go into unrelated nested classes
            }

            override fun visitNamedFunction(function: KtNamedFunction, data: Nothing?) = null.also {
                report(function, KotlinSchemaBuildProblem::ForbiddenFunction)
            }

            override fun visitSuperTypeListEntry(specifier: KtSuperTypeListEntry, data: Nothing?): Void? {
                if (!options.isParsingAmperApi) { // Ignore for unreleased - may be part of the variant
                    report(specifier, KotlinSchemaBuildProblem::ForbiddenMixins)
                }
                return super.visitSuperTypeListEntry(specifier, data)
            }

            override fun visitTypeParameterList(list: KtTypeParameterList, data: Nothing?) = null.also {
                report(list, KotlinSchemaBuildProblem::ForbiddenGenerics)
            }

            override fun visitContextReceiverList(contextReceiverList: KtContextReceiverList, data: Nothing?) =
                null.also {
                    report(contextReceiverList, KotlinSchemaBuildProblem::ForbiddenContextReceivers)
                }

            override fun visitProperty(property: KtProperty, data: Nothing?): Void? {
                if (isPrimarySchema && property.name == "enabled") {
                    report(property.nameIdentifier ?: property) { source ->
                        KotlinSchemaBuildProblem.ForbiddenPropertyEnabled(
                            source,
                            options.pluginSettingsClassName,
                        )
                    }
                } else {
                    parseProperty(property)?.let(::add)
                }
                return super.visitProperty(property, data)
            }
        }
        schemaDeclaration.acceptChildren(visitor)
    }
    return PluginData.ClassData(
        name = name,
        properties = properties,
        doc = schemaDeclaration.getDefaultDocString(),
        origin = nameIdentifier.getSourceLocation(),
    )
}