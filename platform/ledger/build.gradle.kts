dependencies {
    api(project(":platform:shared-kernel"))

    implementation(enforcedPlatform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.data.jpa)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
