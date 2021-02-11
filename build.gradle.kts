import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  application
  kotlin("jvm") version "1.4.30"
  id("com.github.ben-manes.versions") version "0.36.0"
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

  maven("https://dl.bintray.com/egor-bogomolov/astminer")
  maven("https://clojars.org/repo")

  maven("https://raw.github.com/idsia/crema/mvn-repo/")
  maven("https://dl.bintray.com/kotlin/kotlin-datascience")
}

dependencies {
  implementation(kotlin("stdlib"))

  implementation("com.github.breandan:kaliningraph:0.1.4")
  implementation("ch.idsia:crema:0.1.5")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

//    implementation("com.github.axkr:symja_kotlin:-0164fc62ff-1")
  implementation("org.matheclipse:matheclipse-core:1.0.0-SNAPSHOT")
  implementation("org.jetbrains.lets-plot-kotlin:lets-plot-kotlin-api:1.2.0")
  implementation("com.github.analog-garage:dimple:master-SNAPSHOT")

  implementation("com.github.TUK-CPS:jAADD:-SNAPSHOT")
  implementation("ca.umontreal.iro.simul:ssj:3.3.1")

  // MPJ (required for Poon's SPN)
  implementation(files("$projectDir/libs/mpj-0.44.jar"))

  val multik_version = "0.0.1-dev-13"
  implementation("org.jetbrains.kotlinx.multik:multik-api:$multik_version")
  implementation("org.jetbrains.kotlinx.multik:multik-default:$multik_version")
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions {
      languageVersion = "1.5"
      apiVersion = "1.5"
      jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
  }

  val codeSynth by creating(JavaExec::class) {
    main = "edu.mcgill.markovian.mcmc.MarkovChainKt"
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf(findProperty("train")?.toString() ?: projectDir.path)
  }
}

application {
  mainClass.set("DSLKt")
}