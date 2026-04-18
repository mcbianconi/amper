/*
 * Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.plugins.schema.model

import kotlinx.serialization.Serializable
import org.jetbrains.amper.problems.reporting.FileWithRangesBuildProblemSource
import org.jetbrains.amper.serialization.paths.SerializablePath

@Serializable
data class SourceLocation(
    override val file: SerializablePath,
    override val offsetRange: @Serializable(with = RangeSerializer::class) IntRange,
) : FileWithRangesBuildProblemSource