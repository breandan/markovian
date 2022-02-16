import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  `maven-publish`
  kotlin("jvm") version "1.6.20-M1"
  id("com.github.ben-manes.versions") version "0.42.0"
  id("org.jetbrains.kotlin.jupyter.api") version "0.11.0-60"
  id("com.google.devtools.ksp") version "1.6.20-M1-1.0.2"
}

group = "com.github.breandan"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  maven("https://jitpack.io")
  maven("https://oss.sonatype.org/content/repositories/snapshots")
//  maven("https://raw.github.com/idsia/crema/mvn-repo/")
}

//java.toolchain {
//  languageVersion.set(JavaLanguageVersion.of(15))
//  vendor.set(JvmVendorSpec.ADOPTOPENJDK)
//  implementation.set(JvmImplementation.J9)
//}

dependencies {
  implementation(kotlin("stdlib"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

  implementation("ai.hypergraph:kaliningraph:0.1.9")
//  implementation("ch.idsia:crema:0.1.7.a")

//  implementation("com.github.axkr:symja_kotlin:-0164fc62ff-1")
//  implementation("org.matheclipse:matheclipse-core:2.0.0-SNAPSHOT")
//  implementation("org.matheclipse:matheclipse-gpl:2.0.0-SNAPSHOT")

  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3") // TODO: why?
  implementation("org.jetbrains.lets-plot:lets-plot-jfx:2.2.1")
  implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:3.1.1")

//  https://arxiv.org/pdf/1908.10693.pdf
//  implementation("com.datadoghq:sketches-java:0.7.0")

  // Cache PMF/CDF lookups for common queries
  implementation("com.github.ben-manes.caffeine:caffeine:3.0.5")

  implementation("org.apache.datasketches:datasketches-java:3.1.0")

//  implementation("com.github.analog-garage:dimple:master-SNAPSHOT")

//  implementation("com.github.TUK-CPS:jAADD:-SNAPSHOT")
  implementation("ca.umontreal.iro.simul:ssj:3.3.1")

  // MPJ (required for Poon's SPN)
  implementation(files("$projectDir/libs/mpj-0.44.jar"))

  val multik_version = "0.1.1"
//  val multik_version = "0.1.1" // tests fail
  implementation("org.jetbrains.kotlinx:multik-api:$multik_version")
  implementation("org.jetbrains.kotlinx:multik-jvm:$multik_version")
//  implementation("org.jetbrains.kotlinx:multik-native:$multik_version")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks {
  test {
    useJUnitPlatform()
    testLogging {
      events = setOf(FAILED, PASSED, SKIPPED, STANDARD_OUT)
      exceptionFormat = FULL
      showExceptions = true
      showCauses = true
      showStackTraces = true
      showStandardStreams = true
    }
  }

  compileKotlin {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_15.toString()
  }
}

publishing {
  publications.create<MavenPublication>("default") {
    from(components["java"])

    pom {
      name.set("Markovian")
      description.set("A Kotlin DSL for probabilistic programming")
      url.set("https://github.com/breandan/markovian")
      licenses {
        license {
          name.set("The Apache Software License, Version 1.0")
          url.set("http://www.apache.org/licenses/LICENSE-3.0.txt")
          distribution.set("repo")
        }
      }
      developers {
        developer {
          id.set("Breandan Considine")
          name.set("Breandan Considine")
          email.set("bre@ndan.co")
          organization.set("McGill University")
        }
      }
      scm {
        url.set("https://github.com/breandan/markovian")
      }
    }
  }
}