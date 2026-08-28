import java.time.Duration

plugins {
    application
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.jackson.yaml)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass.set("dev.whoisyourbias.financial.knowledge.KnowledgeHarnessApplication")
}

tasks.register<JavaExec>("knowledgeCheck") {
    description = "Validates the domain knowledge catalog and internal links."
    group = "verification"
    timeout.set(Duration.ofMinutes(2))
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    args("check", rootProject.projectDir.absolutePath)
}

tasks.register<JavaExec>("knowledgeExport") {
    description = "Exports deterministic knowledge chunks as JSONL."
    group = "documentation"
    timeout.set(Duration.ofMinutes(2))
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    args("export", rootProject.projectDir.absolutePath)
}
