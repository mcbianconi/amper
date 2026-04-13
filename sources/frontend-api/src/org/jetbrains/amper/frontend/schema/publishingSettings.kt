/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.schema

import org.jetbrains.amper.frontend.api.Misnomers
import org.jetbrains.amper.frontend.api.PlatformAgnostic
import org.jetbrains.amper.frontend.api.SchemaDoc
import org.jetbrains.amper.frontend.api.SchemaNode

class PublishingSettings : SchemaNode() {

    @PlatformAgnostic
    @SchemaDoc("Enables the publication of the module to Maven repositories (via `./amper publish`)")
    val enabled by value(default = false)

    @PlatformAgnostic
    @SchemaDoc("Group ID of the published Maven artifact")
    val group by nullableValue<String>()

    @PlatformAgnostic
    @SchemaDoc("Version of the published Maven artifact")
    val version by nullableValue<String>()

    @Misnomers("artifact", "artifactId")
    @SchemaDoc("Artifact ID of the published Maven artifact")
    val name by nullableValue<String>()
}
