package com.clintoncochrane.bikecompanion

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionDependencyPolicyTest {

    @Test
    fun gradleBuildCache_isDisabledForVulnerableKotlinPlugin() {
        val gradleProperties = File("../gradle.properties").readText()

        assertTrue(gradleProperties.contains("org.gradle.caching=false"))
    }

    @Test
    fun kotlinDependencyAlignment_matchesTheKotlinGradlePlugin() {
        val rootBuild = File("../build.gradle").readText()

        assertTrue(rootBuild.contains("id(\"org.jetbrains.kotlin.android\") version \"2.0.20\""))
        assertTrue(rootBuild.contains("useVersion(\"2.0.20\")"))
    }

    @Test
    fun healthConnect_usesStableClientAndPatchedGuava() {
        val appBuild = File("build.gradle").readText()

        assertTrue(appBuild.contains("androidx.health.connect:connect-client:1.1.0"))
        assertFalse(appBuild.contains("androidx.health.connect:connect-client:1.2.0-alpha02"))
        assertTrue(appBuild.contains("com.google.guava:guava:32.0.1-android"))
    }

    @Test
    fun build_doesNotIncludeUnusedFrameworks() {
        val appBuild = File("build.gradle").readText()
        val unusedDependencies = listOf(
            "androidx.appcompat:appcompat",
            "androidx.cardview:cardview",
            "androidx.constraintlayout:constraintlayout",
            "androidx.recyclerview:recyclerview",
            "com.google.android.material:material",
            "androidx.work:work-runtime-ktx",
        )

        unusedDependencies.forEach { dependency ->
            assertFalse("Unused dependency remains: $dependency", appBuild.contains(dependency))
        }
    }
}
