/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.cli

/**
 * Simple trait for entities that potentially come from plugins
 * and need to be identified by their qualified/simple names
 */
interface QualifiedEntity {
    val name: String
    val pluginId: String?
}

fun <T : QualifiedEntity> resolveMatchingEntities(
    userProvidedName: String,
    entities: List<T>,
    entityDisplayName: String,
    context: String = "",
): List<T> {
    val allNames = entities.map(QualifiedEntity::qualifiedName)

    fun formatAllAvailable() = buildList {
        allNames.groupBy(
            keySelector = QualifiedName::name,
            valueTransform = QualifiedName::toString,
        ).forEach { (name, qualifiedNames) ->
            val singleQualifiedName = qualifiedNames.singleOrNull()
            if (singleQualifiedName != null) {
                if (name != singleQualifiedName) {
                    add("'$name' (or '$singleQualifiedName')")
                } else {
                    add("'$name'")
                }
            } else {
                qualifiedNames.forEach {
                    add("'$it'")
                }
            }
        }
    }.joinToString(prefix = "Available ${entityDisplayName}s: ")

    val resolvedNames = if (':' in userProvidedName) {
        allNames.filter { it.qualifiedName == userProvidedName }
    } else {
        allNames.filter { it.name == userProvidedName }
    }.toSet()

    if (resolvedNames.isEmpty()) {
        userReadableError("Unknown $entityDisplayName '$userProvidedName'$context. ${formatAllAvailable()}")
    }
    if (resolvedNames.size > 1) {
        userReadableError(
            "Ambiguous $entityDisplayName name '$userProvidedName'$context. " +
                    "Multiple plugins provide a $entityDisplayName with this name. " +
                    "Please use a qualified name: ${resolvedNames.joinToString()}"
        )
    }
    return entities.filter { it.qualifiedName in resolvedNames }
}

private data class QualifiedName(
    val name: String,
    val qualifier: String?
) {
    val qualifiedName = qualifier?.let { "$it:$name" } ?: name
    override fun toString() = qualifiedName
}

private val QualifiedEntity.qualifiedName
    get() = QualifiedName(name, pluginId)
