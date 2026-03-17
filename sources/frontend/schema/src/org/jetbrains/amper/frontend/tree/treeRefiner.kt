/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.tree

import org.jetbrains.amper.frontend.api.Trace
import org.jetbrains.amper.frontend.api.isDefault
import org.jetbrains.amper.frontend.api.withPrecedingValue
import org.jetbrains.amper.frontend.contexts.Context
import org.jetbrains.amper.frontend.contexts.Contexts
import org.jetbrains.amper.frontend.contexts.ContextsInheritance
import org.jetbrains.amper.frontend.contexts.WithContexts
import org.jetbrains.amper.frontend.contexts.asCompareResult
import org.jetbrains.amper.frontend.contexts.defaultContextsInheritance
import org.jetbrains.amper.frontend.contexts.sameOrMoreSpecific
import org.jetbrains.amper.frontend.types.SchemaType
import org.jetbrains.amper.problems.reporting.ProblemReporter
import org.jetbrains.annotations.TestOnly

/**
 * This is a class responsible for refining [TreeNode] values for a specified [Contexts].
 * Consider the following example:
 * ```yaml
 * # tree 1
 * foo:
 *   bar: myValue
 *   baz: myValue
 *
 * # tree 2
 * foo@jvm:
 *   bar@jvm: myValueJvm
 *
 * # refined tree for contexts `[jvm]`:
 * foo@jvm:
 *   bar@jvm: myValueJvm
 *   baz: myValue
 * ```
 */
class TreeRefiner(
    private val contextComparator: ContextsInheritance<Context> = defaultContextsInheritance,
) {
    context(_: ProblemReporter)
    fun refineTree(
        tree: MappingNode,
        selectedContexts: Contexts,
        resolveReferences: Boolean = true,
        withDefaults: Boolean = true,
    ): RefinedMappingNode = RefineRequest(
        selectedContexts = selectedContexts,
        withDefaults = withDefaults,
        resolveReferences = resolveReferences,
        contextComparator = contextComparator,
    ).refineMapping(tree)
}

@TestOnly
context(_: ProblemReporter)
internal fun MappingNode.refineTree(
    selectedContexts: Contexts,
    contextComparator: ContextsInheritance<Context>,
    withDefaults: Boolean = true,
    resolveReferences: Boolean = true,
): RefinedMappingNode = RefineRequest(
    selectedContexts = selectedContexts,
    withDefaults = withDefaults,
    resolveReferences = resolveReferences,
    contextComparator = contextComparator,
).refineMapping(this)

private class RefineRequest(
    private val selectedContexts: Contexts,
    private val withDefaults: Boolean,
    private val resolveReferences: Boolean,
    contextComparator: ContextsInheritance<Context>,
) : ContextsInheritance<Context> by contextComparator {

    /**
     * Creates a copy out of the subtree of the initial tree, selected by [selectedContexts]
     * with merged nodes.
     */
    context(_: ProblemReporter)
    fun refineMapping(node: MappingNode): RefinedMappingNode {
        var refined = refine(node) as RefinedMappingNode
        if (resolveReferences) {
            // TODO: Incorporate reference resolution routine tightly into refining to enable merging resolved values.
            refined = refined.resolveReferences()
        }
        return refined
    }

    context(_: ProblemReporter)
    private fun refine(node: TreeNode): RefinedTreeNode {
        return when (node) {
            is RefinedTreeNode -> node
            is ListNode -> RefinedListNode(
                children = node.children.filterByContexts().map { refine(it) },
                type = node.type,
                trace = node.trace,
                contexts = node.contexts,
            )
            is MappingNode -> refinedMappingNodeWithDefaults(
                node.children.refineProperties(),
                type = node.type,
                trace = node.trace,
                contexts = node.contexts,
            )
        }
    }

    /**
     * Merge named properties, comparing them by their contexts and by their name.
     */
    context(_: ProblemReporter)
    private fun List<KeyValue>.refineProperties(): Map<String, RefinedKeyValue> =
        filterByContexts().run {
            // Do actual overriding for key-value pairs.
            val refinedProperties = refineOrReduceByKeys { props ->
                val groupedKeyValues = groupMostSpecificKeyValues(props)
                check(groupedKeyValues.isNotEmpty()) { "Key values grouped by value shouldn't be empty" }

                // TODO: This is incorrect as we should build graph of props and traverse them in
                //  the topological order.
                val partiallySortedProps = props.sortedWith { first, second ->
                    first.contexts.compareContexts(second.contexts).asCompareResult ?: 0
                }

                // TODO: This is completely incorrect as we should build graph of traces instead of a chain.
                val newTrace = reduceTrace(partiallySortedProps)

                if (groupedKeyValues.size > 1) {
                    // We have conflicts, let's report an error node
                    val anyConflictingValueList = groupedKeyValues.values.first()
                    val anyKeyValue = anyConflictingValueList.first()
                    return@refineOrReduceByKeys anyKeyValue.copyWithValue(ErrorNode(newTrace))
                }

                val actualKeyValues = groupedKeyValues.values.single()
                val valueToUse = actualKeyValues.first()
                // We consider the most specific key value as the source of truth for the type of the node and contexts.
                val refinedValue = when (val value = valueToUse.value) {
                    is LeafTreeNode -> value.copyWithTrace(newTrace)
                    is ListNode -> {
                        val children = partiallySortedProps.flatMap { (it.value as? ListNode)?.children.orEmpty() }
                        RefinedListNode(
                            children = children.filterByContexts().map { refine(it) },
                            type = value.type,
                            trace = newTrace,
                            contexts = value.contexts,
                        )
                    }
                    is MappingNode -> {
                        val children = partiallySortedProps.flatMap { (it.value as? MappingNode)?.children.orEmpty() }
                        refinedMappingNodeWithDefaults(
                            refinedChildren = children.refineProperties(),
                            type = value.type,
                            trace = newTrace,
                            contexts = value.contexts,
                        )
                    }
                }
                valueToUse.copyWithValue(refinedValue)
            }

            // Restore order. Also, ignore NoValues if anything is overwriting them.
            val unordered = refinedProperties.associateBy { it.key }
            return mapTo(mutableSetOf()) { it.key }.associateWith { unordered[it]!! }
        }

    /**
     * Groups most specific [keyValues] by unique values.
     *
     * The returned map contains node values as keys (where containers don't have separation between contents and
     * are identical by type) and lists of incomparable key values as values of the map.
     *
     * NB: the presence of multiple incomparable key values in the list doesn't mean that there is a conflict,
     * because they are all providing the same value. However, the presence of multiple keys in the map definitely
     * means that we don't know what the final value should be.
     */
    context(problemReporter: ProblemReporter)
    private fun groupMostSpecificKeyValues(keyValues: List<KeyValue>): Map<Any?, List<KeyValue>> {
        val indeterminateValues = buildList<KeyValue> {
            for (keyValue in keyValues) {
                // For each of the key values in the list of indeterminate ones, we want to be sure in the final list
                // there are no values that are more specific than this one. Thus, on each step we delete less specific
                // values from the list and then add an item if no values are more specific as the current.
                // This way we end up with the list of values that are either indeterminate or same with any other
                // value in the list.
                removeAll { it.contexts.compareContexts(keyValue.value.contexts) == ContextsInheritance.Result.IS_LESS_SPECIFIC }
                if (none { it.contexts.compareContexts(keyValue.value.contexts) == ContextsInheritance.Result.IS_MORE_SPECIFIC }) add(keyValue)
            }
        }.distinct() // A key value might have come from the same tree read multiple times; we deduplicate them here.

        val keyValuesGroupedByNodeValue = indeterminateValues.groupBy { keyValue ->
            when (val value = keyValue.value) {
                is MappingNode, is ListNode, is ErrorNode -> CanMergeWithoutConflicts
                is ReferenceNode -> value.referencedPath
                is StringInterpolationNode -> value.parts
                is NullLiteralNode -> null
                is BooleanNode -> value.value
                is EnumNode -> value.entryName
                is IntNode -> value.value
                is PathNode -> value.value
                is StringNode -> value.value
            }
        }

        if (keyValuesGroupedByNodeValue.size > 1) {
            problemReporter.reportMessage(
                ConflictingProperties(selectedContexts, indeterminateValues)
            )
        }

        return keyValuesGroupedByNodeValue
    }

    /**
     * Helper object to represent the equality for containers when detecting conflicts as we can meaningfully merge
     * mappings, lists, and errors.
     */
    private object CanMergeWithoutConflicts

    /**
     * Reduces the traces of sorted list of key-value pairs by merging them into a single trace
     * with the chain of preceding values.
     */
    private fun reduceTrace(sorted: List<KeyValue>): Trace {
        var trace = sorted[0].value.trace
        for (i in 1 until sorted.size) {
            val newTrace = sorted[i].value.trace
            // Defaults with higher priority just replace each other without saving preceding value
            trace = if (newTrace.isDefault) newTrace else newTrace.withPrecedingValue(sorted[i - 1].value)
        }
        return trace
    }

    context(_: ProblemReporter)
    private fun refinedMappingNodeWithDefaults(
        refinedChildren: Map<String, RefinedKeyValue>,
        type: SchemaType.MapLikeType,
        trace: Trace,
        contexts: Contexts,
    ): RefinedMappingNode {
        val refinedChildrenWithDefaults = if (withDefaults && type is SchemaType.ObjectType) {
            val defaults = buildMap {
                for (property in type.declaration.properties) {
                    val existingValue = refinedChildren[property.name]
                    if (existingValue == null || existingValue.value is ErrorNode) {
                        type.declaration.getDefaultFor(property)?.let { default ->
                            put(property.name, default)
                        }
                    }
                }
            }
            if (defaults.isEmpty()) refinedChildren else refinedChildren + defaults
        } else refinedChildren

        // TODO: We could report missing properties here and pull this logic from the `schemaInstantiator.kt`.
        return RefinedMappingNode(
            refinedChildren = refinedChildrenWithDefaults,
            type = type,
            trace = trace,
            contexts = contexts,
        )
    }

    /**
     * Refines the element if it is single or applies [reduce] to a collection of properties grouped by keys.
     */
    context(_: ProblemReporter)
    private fun List<KeyValue>.refineOrReduceByKeys(reduce: (List<KeyValue>) -> RefinedKeyValue) =
        groupBy { it.key }.values.map { props ->
            props.singleOrNull()?.let { it.copyWithValue(refine(it.value)) }
                ?: props.filterByContexts().let(reduce)
        }

    private fun <T : WithContexts> List<T>.filterByContexts() =
        filter { selectedContexts.compareContexts(it.contexts).sameOrMoreSpecific }
}