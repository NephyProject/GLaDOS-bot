import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

val ktorVersion = "1.1.2"

plugins { 
    kotlin("jvm") version "1.3.20"
    id("kotlinx-serialization") version "1.3.20"

    // For testing
    id("com.adarshr.test-logger") version "1.6.0"
    id("build-time-tracker") version "0.11.0"

    // For publishing
    id("maven-publish")
    id("com.jfrog.bintray") version "1.8.4"

    // For documentation
    id("org.jetbrains.dokka") version "0.9.17"
}

fun Project.property(key: String? = null) = object: ReadOnlyProperty<Project, String?> {
    override fun getValue(thisRef: Project, property: KProperty<*>): String? {
        val name = key ?: property.name
        return (properties[name] ?: System.getProperty(name) ?: System.getenv(name))?.toString()
    }
}

allprojects {
    group = "jp.nephy"
    version = "2.0.0"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.adarshr.test-logger")
    apply(plugin = "build-time-tracker")
    
    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://kotlin.bintray.com/ktor")
        maven(url = "https://kotlin.bintray.com/kotlinx")
        maven(url = "https://kotlin.bintray.com/kotlin-eap")
        maven(url = "https://dl.bintray.com/nephyproject/penicillin")
    }
    
    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions { 
            jvmTarget = "1.8"
            // freeCompilerArgs = freeCompilerArgs + listOf("-Xuse-experimental=kotlin.Experimental", "-Xallow-result-return-type")
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

dependencies {
    implementation(project(":glados-core"))
    implementation(project(":glados-client:glados-client-chrono"))
    implementation(project(":glados-client:glados-client-loop"))
    implementation(project(":glados-client:glados-client-discord"))
    implementation(project(":glados-client:glados-client-twitter"))
    implementation(project(":glados-client:glados-client-web"))
    
    implementation("org.fusesource.jansi:jansi:1.17.1")
}

task<Jar>("farJar") {
    dependsOn("build")
    
    baseName = "glados"
    
    manifest {
        attributes("Main-Class" to "jp.nephy.glados.GLaDOS")
    }
    
    from(configurations.compileClasspath.filter {
        !it.name.endsWith("pom")
    }.map { 
        if (it.isDirectory) it else zipTree(it)
    })
}
