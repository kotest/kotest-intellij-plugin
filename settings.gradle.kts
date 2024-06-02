import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

////import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
   id("org.jetbrains.intellij.platform.settings") version "2.0.0-beta5"
}

dependencyResolutionManagement {
   repositories {
      mavenCentral()
      mavenLocal()
      maven("https://oss.sonatype.org/content/repositories/snapshots/")
      maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
      intellijPlatform {
         defaultRepositories()
      }
   }
   versionCatalogs {
      create("libs") {
         library("runtime-kotest-legacy-launcher", "io.kotest:kotest-launcher:1.0.10")
         library("kotest-framework-launcher", "io.kotest:kotest-framework-launcher:4.2.0")
         // Separate these from the Kotest libraries that will go into the plugin release
         // these are the versions used to TEST the plugin
         library("test-kotest-assertions-core", "io.kotest:kotest-assertions-core:5.8.1")
         library("test-kotest-framework-api", "io.kotest:kotest-framework-api:5.8.1")
         library("jaxb-api", "javax.xml.bind:jaxb-api:2.3.1")
         library("javax-activation", "javax.activation:activation:1.1.1")
      }
   }
}
