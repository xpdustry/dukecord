import com.diffplug.gradle.spotless.SpotlessExtension
import net.kyori.indra.IndraExtension
import net.kyori.indra.git.task.RequireClean
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("com.diffplug.spotless") version "8.7.0"
    id("net.kyori.indra") version "4.0.0"
    id("net.kyori.indra.publishing") version "4.0.0"
    id("net.ltgt.errorprone") version "5.1.0"
}

group = "dev.phinner"
version = file("VERSION.txt").readText().trim() + if (findProperty("is_release").toString().toBoolean()) "" else "-SNAPSHOT"
description = "Lightweight discord API for java."

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.grack:nanojson:1.10")

    compileOnly("org.immutables:value-annotations:2.11.6")
    annotationProcessor("org.immutables:value:2.11.6")

    compileOnlyApi("org.slf4j:slf4j-api:2.0.18")
    implementation("org.slf4j:slf4j-simple:2.0.18") // TODO Replace with System.Logger?
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.18")

    compileOnlyApi("org.jspecify:jspecify:1.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testImplementation("org.junit.vintage:junit-vintage-engine:6.0.1")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    errorprone("com.google.errorprone:error_prone_core:2.49.0")
    errorprone("com.uber.nullaway:nullaway:0.13.4")
}

configurations.runtimeClasspath {
    exclude(group = "com.google.errorprone")
}

configure<IndraExtension> {
    javaVersions {
        target(25)
        minimumToolchain(25)
    }

    publishSnapshotsTo("xpdustry", "https://maven.xpdustry.com/snapshots")
    publishReleasesTo("xpdustry", "https://maven.xpdustry.com/releases")

    apache2License()

    github("xpdustry", "dukecord") {
        ci(true)
        issues(true)
        scm(true)
    }

    configurePublications {
        pom {
            organization {
                name = "xpdustry"
                url = "https://www.xpdustry.com"
            }

            developers {
                developer {
                    id = "phinner"
                    email = "contact@phinner.dev"
                    url = "https://phinner.dev"
                    organization = "xpdustry"
                    organizationUrl = "https://xpdustry.com"
                    timezone = "Europe/Brussels"
                }
            }
        }
    }
}

configure<SpotlessExtension> {
    java {
        palantirJavaFormat()
        formatAnnotations()
        importOrder("", "\\#")
        forbidModuleImports()
        forbidWildcardImports()
        licenseHeader("// SPDX-License-Identifier: Apache-2.0\n// SPDX-FileCopyrightText: 2026 phinner")
    }
    kotlinGradle {
        ktlint()
    }
}

signing {
    val signingKey = findProperty("signingKey") as String?
    val signingPassword = findProperty("signingPassword") as String?
    useInMemoryPgpKeys(signingKey, signingPassword)
}

tasks.jar {
    archiveFileName = "dukecord.jar"
}

tasks.withType<RequireClean> {
    enabled = false
}

tasks.withType<JavaCompile> {
    options.errorprone {
        disableWarningsInGeneratedCode = true
        disable("MissingSummary", "InlineMeSuggester")
        option("NullAway:OnlyNullMarked")
        option("NullAway:JSpecifyMode", "true")
        check("NullAway", CheckSeverity.ERROR)
    }
}
