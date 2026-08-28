import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import java.time.Duration

plugins {
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spotless)
}

group = "dev.whoisyourbias.financial"
version = "0.1.0-SNAPSHOT"

val checkstyleToolVersion = libs.versions.checkstyle.get()

dependencyLocking {
    lockAllConfigurations()
}

configurations.configureEach {
    resolutionStrategy {
        failOnVersionConflict()
        eachDependency {
            val selected = requested.version ?: return@eachDependency
            if (
                selected.contains("+") ||
                selected.startsWith("latest.", ignoreCase = true) ||
                selected.endsWith("-SNAPSHOT", ignoreCase = true)
            ) {
                throw GradleException(
                    "Dynamic and snapshot dependency versions are forbidden: ${requested.group}:${requested.name}:$selected",
                )
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror", "-parameters"))
}

tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(10))
    useJUnitPlatform()
    systemProperty("file.encoding", "UTF-8")
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
    systemProperty("user.timezone", "UTC")
    systemProperty("financial.repository.root", rootProject.projectDir.absolutePath)
    reports.junitXml.required.set(true)
    reports.html.required.set(true)
}

tasks.test {
    useJUnitPlatform {
        excludeTags("module-boundary-functional")
    }
}

val moduleBoundaryFunctionalTest =
    tasks.register<Test>("moduleBoundaryFunctionalTest") {
        description = "Proves forbidden module dependencies and persistence access fail."
        group = "verification"
        testClassesDirs =
            sourceSets.test
                .get()
                .output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        useJUnitPlatform {
            includeTags("module-boundary-functional")
        }
        dependsOn(":platform:ledger:jar")
        shouldRunAfter(tasks.test)
    }

allprojects {
    group = rootProject.group
    version = rootProject.version
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "checkstyle")

    dependencyLocking {
        lockAllConfigurations()
    }

    configurations.configureEach {
        resolutionStrategy {
            failOnVersionConflict()
            eachDependency {
                val selected = requested.version ?: return@eachDependency
                if (requested.group == rootProject.group.toString()) {
                    return@eachDependency
                }
                if (
                    selected.contains("+") ||
                    selected.startsWith("latest.", ignoreCase = true) ||
                    selected.endsWith("-SNAPSHOT", ignoreCase = true)
                ) {
                    throw GradleException(
                        "Dynamic and snapshot dependency versions are forbidden: ${requested.group}:${requested.name}:$selected",
                    )
                }
            }
        }
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
            vendor.set(JvmVendorSpec.ADOPTIUM)
        }
    }

    extensions.configure<CheckstyleExtension> {
        toolVersion = checkstyleToolVersion
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isShowViolations = true
        maxWarnings = 0
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror", "-parameters"))
    }

    tasks.withType<Test>().configureEach {
        timeout.set(Duration.ofMinutes(10))
        useJUnitPlatform()
        systemProperty("file.encoding", "UTF-8")
        systemProperty("user.language", "en")
        systemProperty("user.country", "US")
        systemProperty("user.timezone", "UTC")
        reports.junitXml.required.set(true)
        reports.html.required.set(true)
    }
}

extensions.configure<SpotlessExtension> {
    lineEndings = LineEnding.UNIX
    java {
        target("**/src/**/*.java")
        googleJavaFormat(
            libs.versions.google.java.format
                .get(),
        )
        formatAnnotations()
    }
    kotlinGradle {
        target("*.gradle.kts", "gradle/*.gradle.kts", "**/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
    format("repositoryText") {
        target(
            ".gitattributes",
            ".gitignore",
            ".editorconfig",
            "**/*.md",
            "**/*.yaml",
            "**/*.yml",
            "**/*.json",
            "**/*.properties",
            "**/*.toml",
        )
        targetExclude("**/build/**", ".worktrees/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

apply(from = "gradle/harness.gradle.kts")
