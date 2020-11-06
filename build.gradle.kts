plugins {
    kotlin("jvm") version "1.4.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jetbrains.bintray.com/lets-plot-maven")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://jitpack.io")
    jcenter()
    maven("https://dl.bintray.com/egor-bogomolov/astminer")
    maven("http://logicrunch.research.it.uu.se/maven/")
    maven("https://clojars.org/repo")

    maven("https://raw.github.com/idsia/crema/mvn-repo/")
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.github.breandan:kaliningraph:0.1.2")
    implementation("ch.idsia:crema:0.1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")

//    implementation("com.github.axkr:symja_kotlin:-0164fc62ff-1")
    implementation("org.matheclipse:matheclipse-core:1.0.0-SNAPSHOT")
    implementation("org.jetbrains.lets-plot-kotlin:lets-plot-kotlin-api:1.0.0")

    // MPJ (required for Poon's SPN)
    implementation(files("$projectDir/libs/mpj-0.44.jar"))
}