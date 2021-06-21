import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.5.20-RC"
  id("com.github.ben-manes.versions") version "0.39.0"
  id("org.jetbrains.kotlin.jupyter.api") version "0.10.0-53"
}

group = "com.github.breandan"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  maven("https://jitpack.io")
  maven("https://oss.sonatype.org/content/repositories/snapshots")
//  maven("https://raw.github.com/idsia/crema/mvn-repo/")
}

dependencies {
  implementation(platform(kotlin("bom")))
  implementation(kotlin("stdlib"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")

  implementation("com.github.breandan:kaliningraph:0.1.7")
//  implementation("ch.idsia:crema:0.1.7.a")

//    implementation("com.github.axkr:symja_kotlin:-0164fc62ff-1")
//  implementation("org.matheclipse:matheclipse-core:2.0.0-SNAPSHOT")
//  implementation("org.matheclipse:matheclipse-gpl:2.0.0-SNAPSHOT")

  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3") // TODO: why?
  implementation("org.jetbrains.lets-plot:lets-plot-jfx:2.0.4")
  implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:3.0.1")

  implementation("com.github.analog-garage:dimple:master-SNAPSHOT")

//  implementation("com.github.TUK-CPS:jAADD:-SNAPSHOT")
  implementation("ca.umontreal.iro.simul:ssj:3.3.1")

  // MPJ (required for Poon's SPN)
  implementation(files("$projectDir/libs/mpj-0.44.jar"))

  val multik_version = "0.0.1"
  implementation("org.jetbrains.kotlinx:multik-api:$multik_version")
  implementation("org.jetbrains.kotlinx:multik-default:$multik_version")
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
  }
}