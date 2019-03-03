/*
 * The MIT License (MIT)
 *
 *     Copyright (c) 2017-2019 Nephy Project Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.nio.file.Paths

rootProject.name = "workspace"

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

fun includeEx(syntax: String) {
    include(syntax)
    with(project(syntax)) {
        name = syntax.split(":").last()
        projectDir = Paths.get("$rootDir${syntax.replace(':', '/')}").toFile()
    }
}

fun includeParent(syntax: String) {
    include(syntax)
    with(project(syntax)) {
        name = syntax.split(":").last()
        projectDir = Paths.get("${rootDir.parentFile}${syntax.replace(':', '/')}").toFile()
    }
}

includeParent(":glados-api")
includeParent(":glados-runtime")

includeParent(":glados-client")
includeParent(":glados-client:glados-client-system")
includeParent(":glados-client:glados-client-chrono")
includeParent(":glados-client:glados-client-loop")
includeParent(":glados-client:glados-client-discord")
includeParent(":glados-client:glados-client-twitter")
includeParent(":glados-client:glados-client-web")

includeEx(":projects")

rootProject.projectDir.resolve("projects").listFiles { file ->
    file.isDirectory && file.name != "src"
}.forEach {
    includeEx(":projects:${it.name}")
}
