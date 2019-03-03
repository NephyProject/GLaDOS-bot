import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins { 
    kotlin("jvm") apply false

    // For testing
    id("com.adarshr.test-logger") version "1.6.0" apply false
    id("build-time-tracker") version "0.11.0" apply false

    // For publishing
    id("maven-publish")
    id("com.jfrog.bintray") version "1.8.4"

    // For documentation
    id("org.jetbrains.dokka") version "0.9.17"
}

subprojects {
    group = "jp.nephy"
    version = "3.0.0"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.adarshr.test-logger")
    apply(plugin = "build-time-tracker")
    
    repositories {
        mavenCentral()
        jcenter()
        
        maven(url = "https://kotlin.bintray.com/ktor")
        maven(url = "https://kotlin.bintray.com/kotlinx")
        maven(url = "https://kotlin.bintray.com/kotlin-eap")
    }
    
    tasks.withType<KotlinCompile> {
        kotlinOptions { 
            jvmTarget = "1.8"
            freeCompilerArgs = freeCompilerArgs + listOf("-Xuse-experimental=kotlin.Experimental")
        }
    }
}
