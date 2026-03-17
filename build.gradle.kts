plugins {
    id("java")
    alias(libs.plugins.run.paper)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")

    maven("https://repo.codemc.io/repository/maven-public/") {
        mavenContent {
            includeGroup("org.popcraft")
        }
    }
}

java.sourceCompatibility = JavaVersion.VERSION_21

dependencies {
    compileOnly(libs.paper)
    compileOnly(libs.chunky)
    compileOnly(libs.chunkyborder)
    compileOnly(libs.squaremap)
}

tasks {
    runServer {
        minecraftVersion("1.21.8")

        downloadPlugins {
            modrinth("chunky", "P3y2MXnd")
            modrinth("chunkyborder", "1.2.23")
            modrinth("squaremap", "p1vSXDNS")
        }
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()

        expand("version" to project.version)
    }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        @Suppress("UnstableApiUsage")
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}
