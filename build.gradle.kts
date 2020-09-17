plugins {
    kotlin("jvm") version "1.4.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jetbrains.bintray.com/lets-plot-maven")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.matheclipse:matheclipse-core:1.0.0-SNAPSHOT")
    implementation("org.jetbrains.lets-plot-kotlin:lets-plot-kotlin-api:1.0.0")
}