import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import java.lang.management.ManagementFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

val postgresImage =
    "postgres:18.6@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280"
val harnessProject = project
val harnessSubprojects = subprojects.toList()
val harnessInvocationStartedAt = Instant.now()
val wrapperJarSha256 = "7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d"

data class CommandResult(
    val exitCode: Int,
    val output: String,
)

fun Project.capture(vararg command: String): CommandResult =
    try {
        val process =
            ProcessBuilder(*command)
                .directory(rootDir)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        CommandResult(process.waitFor(), output.trim())
    } catch (exception: Exception) {
        CommandResult(127, exception.message ?: exception.javaClass.simpleName)
    }

fun sha256(bytes: ByteArray): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

fun sha256(file: File): String = sha256(Files.readAllBytes(file.toPath()))

fun Project.gitTrackedInputsDigest(): String {
    val process =
        ProcessBuilder("git", "ls-files", "-z")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
    val output = process.inputStream.readAllBytes()
    if (process.waitFor() != 0) {
        throw GradleException("Unable to enumerate tracked files for inputsDigest")
    }
    val digest = MessageDigest.getInstance("SHA-256")
    String(output, StandardCharsets.UTF_8)
        .split('\u0000')
        .filter { it.isNotBlank() }
        .sorted()
        .forEach { relativePath ->
            val path = rootDir.toPath().resolve(relativePath).normalize()
            digest.update(relativePath.toByteArray(StandardCharsets.UTF_8))
            digest.update(0)
            digest.update(Files.readAllBytes(path))
            digest.update(0)
        }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun Task.outcome(): String =
    when {
        state.failure != null -> "FAILED"
        state.skipped -> "SKIPPED"
        state.upToDate -> "UP_TO_DATE"
        state.noSource -> "NO_SOURCE"
        state.didWork -> "SUCCESS"
        state.executed -> "SUCCESS"
        else -> "NOT_EXECUTED"
    }

val formatCheck =
    tasks.register("formatCheck") {
        description = "Checks deterministic source and repository formatting."
        group = "verification"
        dependsOn(tasks.named("spotlessCheck"))
    }

val staticAnalysis =
    tasks.register("staticAnalysis") {
        description = "Compiles with strict warnings and runs Checkstyle."
        group = "verification"
        dependsOn(
            subprojects.flatMap { candidate ->
                listOf(
                    "${candidate.path}:compileJava",
                    "${candidate.path}:compileTestJava",
                    "${candidate.path}:checkstyleMain",
                    "${candidate.path}:checkstyleTest",
                )
            },
        )
    }

val unitTest =
    tasks.register("unitTest") {
        description = "Runs unit tests in every module."
        group = "verification"
        dependsOn(subprojects.map { "${it.path}:test" })
    }

val allowedProjectDependencies =
    mapOf(
        ":platform:bootstrap" to setOf(":platform:ledger", ":platform:shared-kernel"),
        ":platform:ledger" to setOf(":platform:shared-kernel"),
        ":platform:shared-kernel" to emptySet(),
        ":platform" to emptySet(),
        ":tools:knowledge-harness" to emptySet(),
        ":tools" to emptySet(),
    )

val moduleBoundaryCheck =
    tasks.register("moduleBoundaryCheck") {
        description = "Rejects project dependencies outside the declared module direction."
        group = "verification"
        doLast {
            val violations =
                harnessSubprojects
                    .flatMap { candidate ->
                        val allowed = allowedProjectDependencies[candidate.path].orEmpty()
                        candidate.configurations.flatMap { configuration ->
                            configuration.dependencies
                                .withType(ProjectDependency::class.java)
                                .mapNotNull { dependency ->
                                    val target = dependency.path
                                    if (target in allowed) {
                                        null
                                    } else {
                                        "${candidate.path} -> $target (${configuration.name})"
                                    }
                                }
                        }
                    }.distinct()
            if (violations.isNotEmpty()) {
                throw GradleException(
                    "Forbidden project dependency direction:\n" +
                        violations.joinToString("\n") { " - $it" },
                )
            }
            logger.lifecycle("PASS module dependency direction")
        }
    }

val architectureTest =
    tasks.register("architectureTest") {
        description = "Runs architecture boundary tests."
        group = "verification"
        dependsOn(
            moduleBoundaryCheck,
            ":moduleBoundaryFunctionalTest",
            ":platform:bootstrap:architectureTest",
        )
    }

val knowledgeCheck =
    tasks.register("knowledgeCheck") {
        description = "Validates the knowledge catalog and repository-local links."
        group = "verification"
        dependsOn(":tools:knowledge-harness:knowledgeCheck")
    }

tasks.register("knowledgeExport") {
    description = "Writes deterministic RAG input JSONL under build/knowledge."
    group = "documentation"
    dependsOn(":tools:knowledge-harness:knowledgeExport")
}

val toolchains = extensions.getByType<JavaToolchainService>()
val java21Launcher =
    toolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }

val harnessDoctor =
    tasks.register("harnessDoctor") {
        description = "Diagnoses Java, wrapper, supply-chain files, Docker, and repository setup."
        group = "verification"
        notCompatibleWithConfigurationCache("Reads live Git and Docker state.")
        doLast {
            val requiredFiles =
                listOf(
                    "gradlew",
                    "gradlew.bat",
                    "gradle/wrapper/gradle-wrapper.jar",
                    "gradle/wrapper/gradle-wrapper.properties",
                    "gradle/verification-metadata.xml",
                    "gradle/verification-keyring.gpg",
                    "settings.gradle.kts",
                    "compose.yaml",
                    "docs/domain/catalog.yaml",
                )
            val missing = requiredFiles.filterNot { rootProject.file(it).isFile }
            if (missing.isNotEmpty()) {
                throw GradleException("Missing required harness files: ${missing.joinToString()}")
            }

            val wrapperProperties =
                rootProject.file("gradle/wrapper/gradle-wrapper.properties").readText()
            if (!wrapperProperties.contains("distributionSha256Sum=")) {
                throw GradleException("Gradle wrapper distributionSha256Sum is required")
            }
            val verificationMetadata =
                rootProject.file("gradle/verification-metadata.xml").readText(StandardCharsets.UTF_8)
            if (!verificationMetadata.contains("<verify-signatures>true</verify-signatures>")) {
                throw GradleException("Dependency signature verification must be enabled")
            }
            val wrapperJar = rootProject.file("gradle/wrapper/gradle-wrapper.jar")
            val actualWrapperJarSha256 = sha256(wrapperJar)
            if (actualWrapperJarSha256 != wrapperJarSha256) {
                throw GradleException(
                    "Gradle wrapper JAR checksum mismatch: expected $wrapperJarSha256, " +
                        "found $actualWrapperJarSha256",
                )
            }
            val lockFiles =
                rootProject.fileTree(rootProject.rootDir) {
                    include("**/gradle.lockfile")
                    exclude(".worktrees/**", "**/build/**")
                }
            if (lockFiles.isEmpty) {
                throw GradleException("No dependency lockfile exists; generate and review locks first")
            }

            val launcher = java21Launcher.get()
            logger.lifecycle(
                "PASS Java toolchain: {} {} ({})",
                launcher.metadata.vendor,
                launcher.metadata.languageVersion,
                launcher.metadata.jvmVersion,
            )

            val docker = harnessProject.capture("docker", "version", "--format", "{{.Client.Version}}|{{.Server.Version}}")
            if (docker.exitCode == 0) {
                logger.lifecycle("PASS Docker client|server: {}", docker.output)
            } else {
                logger.warn("WARN Docker daemon unavailable; harnessFast remains usable: {}", docker.output)
            }

            val cacheReady = gradle.gradleUserHomeDir.resolve("caches").isDirectory
            logger.lifecycle("Dependency cache present: {}", cacheReady)
            logger.lifecycle("PASS required harness files and supply-chain metadata")
        }
    }

val dockerPreflight =
    tasks.register("dockerPreflight") {
        description = "Fails before integration tests when Docker is unavailable."
        group = "verification"
        notCompatibleWithConfigurationCache("Reads live Docker daemon state.")
        doLast {
            val docker = harnessProject.capture("docker", "info", "--format", "{{.ServerVersion}}")
            if (docker.exitCode != 0) {
                throw GradleException(
                    "Docker daemon is not available. Start Docker Desktop (or another compatible daemon), " +
                        "verify `docker info`, then rerun `./gradlew harnessFull`. Details: ${docker.output}",
                )
            }
            logger.lifecycle("PASS Docker daemon: {}", docker.output)
        }
    }

tasks.register<Exec>("localUp") {
    description = "Starts the local PostgreSQL dependency and waits for health."
    group = "application"
    timeout.set(Duration.ofMinutes(3))
    commandLine(
        "docker",
        "compose",
        "--project-name",
        "financial-harness",
        "--file",
        rootProject.file("compose.yaml"),
        "up",
        "--detach",
        "--wait",
        "postgres",
    )
}

tasks.register<Exec>("localDown") {
    description = "Stops local dependencies while preserving the development volume."
    group = "application"
    timeout.set(Duration.ofMinutes(2))
    commandLine(
        "docker",
        "compose",
        "--project-name",
        "financial-harness",
        "--file",
        rootProject.file("compose.yaml"),
        "down",
        "--remove-orphans",
    )
}

tasks.register<Exec>("localReset") {
    description = "Deletes the explicit financial-harness Compose services and volumes."
    group = "application"
    timeout.set(Duration.ofMinutes(2))
    doFirst {
        logger.lifecycle("DESTRUCTIVE target: Compose project financial-harness")
        logger.lifecycle("DESTRUCTIVE volume: financial-harness_postgres-data")
    }
    commandLine(
        "docker",
        "compose",
        "--project-name",
        "financial-harness",
        "--file",
        rootProject.file("compose.yaml"),
        "down",
        "--volumes",
        "--remove-orphans",
    )
}

val integrationTaskPath = ":platform:bootstrap:integrationTest"

val migrationFreshTest =
    tasks.register("migrationFreshTest") {
        description = "Verifies all migrations from an empty PostgreSQL database."
        group = "verification"
        dependsOn(dockerPreflight, integrationTaskPath)
    }

val migrationUpgradeTest =
    tasks.register("migrationUpgradeTest") {
        description = "Verifies migration upgrade or records first-migration NOT_APPLICABLE."
        group = "verification"
        dependsOn(dockerPreflight, integrationTaskPath)
        doLast {
            val migrations =
                rootProject
                    .fileTree("platform/ledger/src/main/resources/db/migration") {
                        include("V*__*.sql")
                    }.files
                    .sortedBy { it.name }
            val report =
                rootProject.layout.buildDirectory
                    .file("reports/harness/migration-upgrade-status.txt")
                    .get()
                    .asFile
            report.parentFile.mkdirs()
            if (migrations.size <= 1) {
                report.writeText("NOT_APPLICABLE: no previous migration\n")
                logger.lifecycle("NOT_APPLICABLE: no previous migration")
            } else {
                report.writeText("PASS: ${migrations[migrations.size - 2].name} -> ${migrations.last().name}\n")
            }
        }
    }

val postgresIntegrationTest =
    tasks.register("postgresIntegrationTest") {
        description = "Verifies PostgreSQL account persistence and schema constraints."
        group = "verification"
        dependsOn(dockerPreflight, integrationTaskPath)
    }

val bootContextTest =
    tasks.register("bootContextTest") {
        description = "Verifies the Spring Boot context against migrated PostgreSQL."
        group = "verification"
        dependsOn(dockerPreflight, integrationTaskPath)
    }

val packaging =
    tasks.register("packaging") {
        description = "Builds the executable Spring Boot artifact."
        group = "build"
        dependsOn(":platform:bootstrap:bootJar")
    }

val harnessFast =
    tasks.register("harnessFast") {
        description = "Runs deterministic Docker-free repository verification."
        group = "verification"
        dependsOn(
            harnessDoctor,
            formatCheck,
            staticAnalysis,
            unitTest,
            architectureTest,
            knowledgeCheck,
        )
    }

listOf(
    migrationFreshTest,
    migrationUpgradeTest,
    postgresIntegrationTest,
    bootContextTest,
    packaging,
).forEach { provider -> provider.configure { mustRunAfter(harnessFast) } }

project(":platform:bootstrap").tasks.matching { it.name == "integrationTest" }.configureEach {
    dependsOn(dockerPreflight)
    mustRunAfter(harnessFast)
}

val harnessFull =
    tasks.register("harnessFull") {
        description = "Runs Fast plus PostgreSQL integration, migration, context, and packaging checks."
        group = "verification"
        dependsOn(
            harnessFast,
            migrationFreshTest,
            migrationUpgradeTest,
            postgresIntegrationTest,
            bootContextTest,
            packaging,
        )
    }

fun registerManifestTask(command: String): TaskProvider<Task> {
    val title = command.replaceFirstChar { it.uppercase() }
    return tasks.register("write${title}Manifest") {
        description = "Writes the $command execution manifest."
        group = "verification"
        notCompatibleWithConfigurationCache("Captures task outcomes and live environment state.")
        doLast {
            val now = Instant.now()
            val commit = harnessProject.capture("git", "rev-parse", "HEAD").output
            val branch = harnessProject.capture("git", "branch", "--show-current").output
            val dirty = harnessProject.capture("git", "status", "--porcelain").output.isNotBlank()
            val shortSha = commit.take(12).ifBlank { "unknown" }
            val runId = "${now.toString().replace(':', '-')}-$shortSha"
            val reportDirectory =
                rootProject.layout.buildDirectory
                    .dir("reports/harness/$runId")
                    .get()
                    .asFile
            reportDirectory.mkdirs()

            val taskOutcomes =
                gradle.taskGraph.allTasks.map { task ->
                    linkedMapOf(
                        "path" to task.path,
                        "outcome" to task.outcome(),
                        "skipMessage" to task.state.skipMessage,
                    )
                }
            val status =
                when (tasks.named(command).get().outcome()) {
                    "SUCCESS", "UP_TO_DATE" -> "PASS"
                    else -> "FAILED"
                }

            val scheduledTaskPaths =
                gradle.taskGraph.allTasks
                    .map { it.path }
                    .toSet()
            val testArtifactFiles =
                gradle.taskGraph.allTasks
                    .filterIsInstance<Test>()
                    .flatMap { testTask ->
                        val resultDirectory =
                            testTask.reports.junitXml.outputLocation
                                .get()
                                .asFile
                        if (resultDirectory.isDirectory) {
                            resultDirectory
                                .walkTopDown()
                                .filter { it.isFile && it.extension == "xml" }
                                .toList()
                        } else {
                            emptyList()
                        }
                    }
            val additionalArtifactFiles =
                buildList {
                    if (":platform:bootstrap:bootJar" in scheduledTaskPaths) {
                        addAll(
                            rootProject
                                .fileTree("platform/bootstrap/build/libs") {
                                    include("*.jar")
                                }.files,
                        )
                    }
                    if (":tools:knowledge-harness:knowledgeExport" in scheduledTaskPaths) {
                        add(rootProject.file("build/knowledge/knowledge.jsonl"))
                    }
                    if (":migrationUpgradeTest" in scheduledTaskPaths) {
                        add(rootProject.file("build/reports/harness/migration-upgrade-status.txt"))
                    }
                }
            val artifactFiles =
                (testArtifactFiles + additionalArtifactFiles)
                    .filter { it.isFile }
                    .distinct()
                    .sortedBy {
                        rootProject.rootDir
                            .toPath()
                            .relativize(it.toPath())
                            .toString()
                    }
            val artifacts =
                artifactFiles.map { artifact ->
                    linkedMapOf(
                        "path" to
                            rootProject.rootDir
                                .toPath()
                                .relativize(artifact.toPath())
                                .toString()
                                .replace(File.separatorChar, '/'),
                        "sha256" to sha256(artifact),
                        "size" to artifact.length(),
                    )
                }

            val testTotals = linkedMapOf("tests" to 0L, "failures" to 0L, "skipped" to 0L)
            val attributePatterns =
                mapOf(
                    "tests" to Regex("""\btests="(\d+)""""),
                    "failures" to Regex("""\bfailures="(\d+)""""),
                    "skipped" to Regex("""\bskipped="(\d+)""""),
                )
            artifactFiles.filter { it.extension == "xml" }.forEach { report ->
                val header = report.useLines { lines -> lines.take(2).joinToString(" ") }
                attributePatterns.forEach { (name, pattern) ->
                    val count =
                        pattern
                            .find(header)
                            ?.groupValues
                            ?.get(1)
                            ?.toLongOrNull() ?: 0L
                    testTotals[name] = testTotals.getValue(name) + count
                }
            }

            val docker = harnessProject.capture("docker", "version", "--format", "{{.Client.Version}}|{{.Server.Version}}")
            val imageInspection =
                harnessProject.capture(
                    "docker",
                    "image",
                    "inspect",
                    postgresImage,
                    "--format",
                    "{{.Architecture}}|{{index .RepoDigests 0}}",
                )
            val inspectedImageParts =
                if (imageInspection.exitCode == 0) {
                    imageInspection.output.split("|", limit = 2)
                } else {
                    emptyList()
                }
            val migrations =
                rootProject
                    .fileTree("platform/ledger/src/main/resources/db/migration") {
                        include("V*__*.sql")
                    }.files
                    .sortedBy { it.name }
            val migrationUpgrade =
                if (migrations.size <= 1) {
                    "NOT_APPLICABLE: no previous migration"
                } else {
                    "PASS"
                }
            val knowledgeExport = rootProject.file("build/knowledge/knowledge.jsonl")
            val catalog = rootProject.file("docs/domain/catalog.yaml")
            val runtime = Runtime.getRuntime()
            val operatingSystem = ManagementFactory.getOperatingSystemMXBean()

            val manifest =
                linkedMapOf(
                    "schemaVersion" to 1,
                    "runId" to runId,
                    "command" to command,
                    "status" to status,
                    "startedAt" to harnessInvocationStartedAt.toString(),
                    "finishedAt" to Instant.now().toString(),
                    "git" to
                        linkedMapOf(
                            "commit" to commit,
                            "branch" to branch,
                            "dirty" to dirty,
                        ),
                    "environment" to
                        linkedMapOf(
                            "os" to System.getProperty("os.name"),
                            "osVersion" to System.getProperty("os.version"),
                            "architecture" to System.getProperty("os.arch"),
                            "availableProcessors" to runtime.availableProcessors(),
                            "jvmMaxMemoryBytes" to runtime.maxMemory(),
                            "locale" to "en-US",
                            "timezone" to "UTC",
                            "javaVendor" to System.getProperty("java.vendor"),
                            "javaVersion" to System.getProperty("java.version"),
                            "gradleVersion" to gradle.gradleVersion,
                            "operatingSystem" to operatingSystem.name,
                        ),
                    "docker" to
                        linkedMapOf(
                            "available" to (docker.exitCode == 0),
                            "clientServerVersion" to
                                if (docker.exitCode == 0) docker.output else null,
                            "postgresImage" to postgresImage.substringBefore("@"),
                            "postgresImageDigest" to postgresImage.substringAfter("@"),
                            "resolvedArchitecture" to inspectedImageParts.getOrNull(0),
                            "resolvedImageDigest" to
                                inspectedImageParts
                                    .getOrNull(1)
                                    ?.substringAfter("@", missingDelimiterValue = ""),
                        ),
                    "migration" to
                        linkedMapOf(
                            "startVersion" to if (migrations.size > 1) migrations[migrations.size - 2].name else null,
                            "endVersion" to migrations.lastOrNull()?.name,
                            "fresh" to if (command == "harnessFull" && status == "PASS") "PASS" else "NOT_RUN",
                            "upgrade" to migrationUpgrade,
                        ),
                    "tests" to testTotals,
                    "generativeTests" to
                        linkedMapOf(
                            "seed" to null,
                            "caseCount" to 0,
                            "reason" to "NOT_APPLICABLE: no generative tests are registered",
                        ),
                    "taskOutcomes" to taskOutcomes,
                    "inputsDigest" to harnessProject.gitTrackedInputsDigest(),
                    "knowledge" to
                        linkedMapOf(
                            "catalogSha256" to if (catalog.isFile) sha256(catalog) else null,
                            "exportSha256" to
                                if (
                                    ":tools:knowledge-harness:knowledgeExport" in scheduledTaskPaths &&
                                    knowledgeExport.isFile
                                ) {
                                    sha256(knowledgeExport)
                                } else {
                                    null
                                },
                            "exportStatus" to
                                if (":tools:knowledge-harness:knowledgeExport" in scheduledTaskPaths) {
                                    "GENERATED"
                                } else {
                                    "NOT_RUN"
                                },
                        ),
                    "artifacts" to artifacts,
                    "warnings" to
                        buildList {
                            if (docker.exitCode != 0) {
                                add("Docker daemon unavailable")
                            }
                            if (dirty) {
                                add("Worktree contains uncommitted tracked or untracked changes")
                            }
                        },
                    "knownLimitations" to
                        listOf(
                            "Execution manifest timestamps are intentionally non-deterministic.",
                            "Linux Full does not prove the Windows Docker path.",
                        ),
                )
            val manifestFile = reportDirectory.resolve("manifest.json")
            val manifestJson =
                JsonOutput
                    .prettyPrint(JsonOutput.toJson(manifest))
                    .lineSequence()
                    .joinToString("\n") { line -> line.trimEnd() }
            manifestFile.writeText(
                manifestJson + "\n",
                StandardCharsets.UTF_8,
            )
            logger.lifecycle("Harness manifest: {}", manifestFile.relativeTo(rootProject.rootDir))
        }
    }
}

val fastManifest = registerManifestTask("harnessFast")
val fullManifest = registerManifestTask("harnessFull")

val requestedHarnessCommands =
    gradle.startParameter.taskNames
        .map { it.substringAfterLast(':') }
        .toSet()
val failureSafeManifests =
    buildList {
        if ("harnessFast" in requestedHarnessCommands) {
            add(fastManifest)
        }
        if ("harnessFull" in requestedHarnessCommands) {
            add(fullManifest)
        }
    }
if (failureSafeManifests.isNotEmpty()) {
    gradle.projectsEvaluated {
        val manifestTasks = failureSafeManifests.map { it.get() }.toSet()
        allprojects
            .flatMap { candidate -> candidate.tasks.toList() }
            .filterNot { it in manifestTasks }
            .forEach { candidate ->
                failureSafeManifests.forEach { manifest ->
                    candidate.finalizedBy(manifest)
                    manifest.configure { mustRunAfter(candidate) }
                }
            }
    }
}

val dependencyResolutionTasks =
    allprojects.map { candidate ->
        candidate.tasks.register("resolveDependencyConfigurations") {
            description = "Resolves this project's configurations for supply-chain metadata updates."
            group = "build setup"
            notCompatibleWithConfigurationCache("Explicit supply-chain metadata maintenance task.")
            doLast {
                candidate.configurations
                    .filter { it.isCanBeResolved }
                    .sortedBy { it.name }
                    .forEach { configuration ->
                        logger.lifecycle("Resolving {}:{}", candidate.path, configuration.name)
                        configuration.resolve()
                    }
            }
        }
    }

tasks.register("resolveAllDependencies") {
    description = "Resolves every resolvable configuration for reviewed lock and verification metadata updates."
    group = "build setup"
    dependsOn(dependencyResolutionTasks)
    notCompatibleWithConfigurationCache("Explicit supply-chain metadata maintenance task.")
}

tasks.register("promoteHarnessEvidence") {
    description = "Promotes a clean successful Full manifest into a project evidence directory."
    group = "documentation"
    notCompatibleWithConfigurationCache("Intentionally writes reviewed tracked evidence.")
    doLast {
        val targetProject =
            providers.gradleProperty("targetProject").orNull
                ?: throw GradleException("Provide -PtargetProject=<id>, for example 01-ledger-core")
        if (!Regex("""\d{2}-[a-z0-9-]+""").matches(targetProject)) {
            throw GradleException("Invalid targetProject: $targetProject")
        }
        val projectDirectory = rootProject.file("projects/$targetProject").canonicalFile
        if (!projectDirectory.isDirectory || projectDirectory.parentFile != rootProject.file("projects").canonicalFile) {
            throw GradleException("Unknown target project directory: projects/$targetProject")
        }
        val status = harnessProject.capture("git", "status", "--porcelain")
        if (status.exitCode != 0 || status.output.isNotBlank()) {
            throw GradleException("Evidence promotion requires a clean worktree")
        }
        val head = harnessProject.capture("git", "rev-parse", "HEAD").output
        val manifests =
            rootProject
                .fileTree(rootProject.layout.buildDirectory.dir("reports/harness")) {
                    include("*/manifest.json")
                }.files
                .sortedByDescending { it.lastModified() }
        val selected =
            manifests.firstOrNull { file ->
                val parsed = JsonSlurper().parse(file) as Map<*, *>
                parsed["command"] == "harnessFull" &&
                    parsed["status"] == "PASS" &&
                    (parsed["git"] as? Map<*, *>)?.get("commit") == head
            } ?: throw GradleException("No successful clean harnessFull manifest matches HEAD $head")

        val parsed = JsonSlurper().parse(selected) as Map<*, *>
        val artifacts = parsed["artifacts"] as? List<*> ?: emptyList<Any>()
        artifacts.forEach { item ->
            val artifact = item as Map<*, *>
            val relativePath = artifact["path"].toString()
            val source = rootProject.file(relativePath)
            if (!source.isFile || sha256(source) != artifact["sha256"]) {
                throw GradleException("Artifact checksum mismatch: $relativePath")
            }
        }

        val evidenceDirectory =
            projectDirectory.resolve("evidence/raw/harness/$head").canonicalFile
        val expectedParent = projectDirectory.resolve("evidence/raw/harness").canonicalFile
        if (!evidenceDirectory.toPath().startsWith(expectedParent.toPath())) {
            throw GradleException("Resolved evidence path escaped the target project")
        }
        evidenceDirectory.mkdirs()
        val destinationManifest = evidenceDirectory.resolve("manifest.json")
        if (destinationManifest.exists()) {
            if (sha256(destinationManifest) != sha256(selected)) {
                throw GradleException("Conflicting promoted manifest already exists for $head")
            }
        } else {
            Files.copy(selected.toPath(), destinationManifest.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        }

        val promotable =
            artifacts
                .map { it as Map<*, *> }
                .filter { artifact ->
                    val relativePath = artifact["path"].toString()
                    relativePath.contains("build/test-results/") ||
                        relativePath == "build/reports/harness/migration-upgrade-status.txt"
                }
        promotable.forEach { artifact ->
            val relativePath = artifact["path"].toString()
            val source = rootProject.file(relativePath)
            val destination = evidenceDirectory.resolve("artifacts").resolve(relativePath)
            destination.parentFile.mkdirs()
            if (!destination.exists()) {
                Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
            } else if (sha256(destination) != sha256(source)) {
                throw GradleException("Conflicting promoted artifact: $relativePath")
            }
        }

        val sensitivePattern =
            Regex(
                """(?i)(AKIA[0-9A-Z]{16}|gh[opusr]_[A-Za-z0-9_]{20,}|-----BEGIN [A-Z ]*PRIVATE KEY-----|/(?:Users|home)/[A-Za-z0-9._-]+|[A-Z]:\\Users\\[A-Za-z0-9._-]+)""",
            )
        evidenceDirectory.walkTopDown().filter { it.isFile }.forEach { file ->
            if (sensitivePattern.containsMatchIn(file.readText(StandardCharsets.UTF_8))) {
                throw GradleException("Potential secret detected in promoted evidence: ${file.name}")
            }
        }

        val projectManifest = projectDirectory.resolve("evidence/MANIFEST.md")
        projectManifest.parentFile.mkdirs()
        val heading = "## Harness $head"
        val existing =
            if (projectManifest.exists()) {
                projectManifest.readText(StandardCharsets.UTF_8)
            } else {
                "# 프로젝트 증거 Manifest\n\n"
            }
        if (!existing.contains(heading)) {
            val rows =
                artifacts.joinToString("\n|") { item ->
                    val artifact = item as Map<*, *>
                    "| `${artifact["path"]}` | `${artifact["sha256"]}` | ${artifact["size"]} |"
                }
            val section =
                """
                |$heading
                |
                |- 검증 대상 commit: `$head`
                |- 명령: `./gradlew harnessFull`
                |- raw manifest: `raw/harness/$head/manifest.json`
                |- 알려진 한계: manifest의 `knownLimitations` 참조
                |
                || Artifact | SHA-256 | Bytes |
                || --- | --- | ---: |
                |$rows
                |
                """.trimMargin()
            projectManifest.writeText(existing.trimEnd() + "\n\n" + section, StandardCharsets.UTF_8)
        }
        logger.lifecycle("Promoted harness evidence for {} into projects/{}/evidence", head, targetProject)
    }
}
