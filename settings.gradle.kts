@file:Suppress("KDocMissingDocumentation")

import java.nio.file.Paths

pluginManagement {
    repositories {
        mavenCentral()
        jcenter()
        gradlePluginPortal()
        maven(url = "https://kotlin.bintray.com/kotlin-eap")
    }

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "org.jetbrains.kotlin.jvm" -> {
                    useVersion(gradle.rootProject.extra["kotlin.version"] as String)
                }
                "com.jfrog.bintray" -> {
                    useModule("com.jfrog.bintray.gradle:gradle-bintray-plugin:${requested.version}")
                }
                "org.jetbrains.dokka" -> {
                    useModule("org.jetbrains.dokka:dokka-gradle-plugin:${requested.version}")
                }
                "com.adarshr.test-logger" -> {
                    useModule("com.adarshr:gradle-test-logger-plugin:${requested.version}")
                }
                "build-time-tracker" -> {
                    useModule("net.rdrei.android.buildtimetracker:gradle-plugin:${requested.version}")
                }
            }
        }
    }
}

rootProject.name = "glados"

fun includeEx(syntax: String) {
    include(syntax)
    with(project(syntax)) {
        name = syntax.split(":").last()
        projectDir = Paths.get("$rootDir${syntax.replace(':', '/')}").toFile()
    }
}

includeEx(":glados-api")
includeEx(":glados-runtime")

includeEx(":glados-client")
includeEx(":glados-client:glados-client-system")
includeEx(":glados-client:glados-client-chrono")
includeEx(":glados-client:glados-client-loop")
includeEx(":glados-client:glados-client-discord")
includeEx(":glados-client:glados-client-twitter")
includeEx(":glados-client:glados-client-web")
