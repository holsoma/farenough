plugins { kotlin("jvm"); kotlin("plugin.serialization") }
kotlin { jvmToolchain(21) }
tasks.test {
    val railData = rootProject.file("app/src/main/assets")
    systemProperty("rail.data.directory", railData.absolutePath)
    inputs.files(railData.resolve("stations.json"), railData.resolve("rail-fares.json"))
        .withPropertyName("bundledRailData").withPathSensitivity(PathSensitivity.RELATIVE)
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation(kotlin("test-junit"))
}
