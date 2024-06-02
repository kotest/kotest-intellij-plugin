plugins {
   java
   kotlin("jvm").version("1.9.23")
   id("org.jetbrains.intellij.platform")
}

data class PluginDescriptor(
   val since: String, // earliest version string this is compatible with
   val until: String, // latest version string this is compatible with, can be wildcard like 202.*
   // https://github.com/JetBrains/gradle-intellij-plugin#intellij-platform-properties
   val intellijVersion: String, // the version string passed to the intellijIdeaCommunity dependency
   val sourceFolder: String, // used as the source root for specifics of this build
)

// https://jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html
// useful link for kotlin plugin versions:
//    https://plugins.jetbrains.com/plugin/6954-kotlin/versions
// json output of versions:
//    https://jb.gg/intellij-platform-builds-list
// json output but restricted to IDEA ultimate:
//    https://data.services.jetbrains.com/products?fields=code,name,releases.downloads,releases.version,releases.build,releases.type&code=IIU
// when releasing for an EAP, look at snapshots and see the column called build number
//    https://www.jetbrains.com/intellij-repository/snapshots
// more versions here:
// https://data.services.jetbrains.com/products?code=IC

// for the sdk version we can use IC-2021.1 if the product is released
// or IC-213-EAP-SNAPSHOT if not

// for 'since' we can use an early build number without eap/snapshot eg 213.5281.15
// and 'until' we can use a wildcard eg 213.*

val plugins = listOf(
   PluginDescriptor(
      since = "223.4884.69", // this version is 2022.3
      until = "223.*",
      intellijVersion = "2022.3",
      sourceFolder = "IC-223",
   ),
   PluginDescriptor(
      since = "231.8109.163", // this version is 2023.1 release
      until = "231.*",
      intellijVersion = "2023.1",
      sourceFolder = "IC-231",
   ),
   PluginDescriptor(
      since = "232.5150.116", // this version is 2023.2
      until = "232.*",
      intellijVersion = "2023.2",
      sourceFolder = "IC-232",
   ),
   PluginDescriptor(
      since = "233.9802.16", // this version is 2023.3
      until = "233.*",
      intellijVersion = "2023.3",
      sourceFolder = "IC-233",
   ),
   PluginDescriptor(
      since = "241.15989.150", // this version is 2024.1
      until = "242.*",
      intellijVersion = "2024.1.1",
      sourceFolder = "IC-241",
   ),
//   PluginDescriptor(
//      since = "242.12881.66", // this version is 2024.2
//      until = "243.*",
//      intellijVersion = "242.12881.66",
//      sourceFolder = "IC-242",
//   ),
)

val productName = System.getenv("PRODUCT_NAME") ?: "IC-241"
val jvmTargetVersion = System.getenv("JVM_TARGET") ?: "17"
val descriptor = plugins.firstOrNull { it.sourceFolder == productName } ?: error("SDK not found $productName")

val jetbrainsToken: String by project

version = "1.4." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0-SNAPSHOT")

dependencies {

   implementation(libs.jaxb.api)
   implementation(libs.javax.activation)

   intellijPlatform {
      intellijIdeaCommunity(descriptor.intellijVersion)
      bundledPlugin("com.intellij.java")
      bundledPlugin("org.jetbrains.kotlin")
   }

   // we bundle this for 4.1 support
   // in kotest 4.2.0 the launcher has moved to a standalone module
   implementation(libs.runtime.kotest.legacy.launcher)

   // this is needed to use the launcher in 4.2.0, in 4.2.1+ the launcher is built
   // into the engine dep which should already be on the classpath
   implementation(libs.kotest.framework.launcher)

   // needed for the resource files which are loaded into java light tests
   testImplementation(libs.test.kotest.framework.api)
   testImplementation(libs.test.kotest.assertions.core)
   runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

sourceSets {
   main {
      kotlin {
         srcDir("src/${descriptor.sourceFolder}/kotlin")
      }
      resources {
         srcDir("src/${descriptor.sourceFolder}/resources")
      }
   }
}

intellijPlatform {
   buildSearchableOptions = true
   instrumentCode = false
   projectName = "Kotest Plugin"
   sandboxContainer.set(project.projectDir)
   pluginConfiguration {
      name.set("kotest-plugin-intellij")
      description.set("Plugin for Kotest for Intellij")
      version.set("${project.version}-${descriptor.intellijVersion}")
      ideaVersion {
         sinceBuild = descriptor.since
         untilBuild = descriptor.until
      }
   }

   publishing {
      token.set(System.getenv("JETBRAINS_TOKEN") ?: jetbrainsToken)
   }

   verifyPlugin {
   }
}

tasks {

   compileKotlin {
      kotlinOptions {
         jvmTarget = jvmTargetVersion
      }
   }

   withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
      kotlinOptions {
         jvmTarget = jvmTargetVersion
      }
   }

//   tasks {
//      printProductsReleases {
//         channels = listOf(ProductRelease.Channel.EAP)
//         types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
//         untilBuild = provider { null }
//
//         doLast {
//            val latestEap = productsReleases.get().max()
//         }
//      }
//   }

//   buildPlugin {
//      archiveClassifier.set(descriptor.sdkVersion)
//   }

   test {
      isScanForTestClasses = false
//       Only run tests from classes that end with "Test"
      include("**/*Test.class")
      include("**/*Tests.class")
   }
}
