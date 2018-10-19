import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc

apply {
    plugin("kotlin")
}

plugins {
    id("com.github.jk1.tcdeps") version "0.17"
}

repositories {
    teamcityServer {
        setUrl("http://buildserver.labs.intellij.net")
        credentials {
            username = "guest"
            password = "guest"
        }
    }
}

val cidrPluginDir: File by rootProject.extra
val clionPluginDir: File by rootProject.extra
val clionVersion = rootProject.extra["versions.clion"] as String
val clionVersionRepo = rootProject.extra["versions.clion.repo"] as String

val cidrPlugin by configurations.creating
val platformDepsZip by configurations.creating

val pluginXmlPath = "META-INF/plugin.xml"
val platformDepsJarName = "kotlinNative-platformDeps.jar"

// Do not rename, used in JPS importer
val projectsToShadow by extra(listOf(
    ":kotlin-ultimate:cidr-native",
    ":kotlin-ultimate:clion-native"))


dependencies {
    cidrPlugin(project(":prepare:cidr-plugin"))
    platformDepsZip(tc("$clionVersionRepo:$clionVersion:CL-plugins/kotlinNative-platformDeps-$clionVersion.zip"))
}

val kotlinPluginXml by tasks.creating {
    inputs.files(cidrPlugin)
    outputs.files(fileFrom(buildDir, name, "META-INF/KotlinPlugin.xml"))

    doFirst {
        val pluginXmlText = zipTree(inputs.files.singleFile)
            .matching { include(pluginXmlPath) }
            .singleFile
            .readText()
        outputs.files.singleFile.writeText(pluginXmlText)
    }
}

val jar = runtimeJar {
    archiveName = "kotlin-plugin.jar"
    dependsOn(cidrPlugin)
    from(kotlinPluginXml) { into("META-INF") }

    from {
        zipTree(cidrPlugin.singleFile).matching {
            exclude(pluginXmlPath)
        }
    }

    for (p in projectsToShadow) {
        dependsOn("$p:classes")
        from(getSourceSetsFrom(p)["main"].output)
    }
}

val platformDepsJar by task<Zip> {
    archiveName = platformDepsJarName
    val platformDepsJar = zipTree(platformDepsZip.singleFile).matching { include("**/$platformDepsJarName") }.singleFile
    from(zipTree(platformDepsJar)) {
        exclude(pluginXmlPath)
    }
}

task<Copy>("clionPlugin") {
    into(clionPluginDir)
    from(cidrPluginDir) { exclude("lib/kotlin-plugin.jar") }
    from(jar) { into("lib") }
    from(platformDepsJar) { into("lib") }
    from(zipTree(platformDepsZip.singleFile).files) {
        exclude("**/$platformDepsJarName")
        into("lib")
    }
    from(File(project(":kotlin-ultimate:clion-native").projectDir, "templates")) { into("templates") }
}
