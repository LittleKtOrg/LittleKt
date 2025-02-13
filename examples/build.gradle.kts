import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins { alias(libs.plugins.kotlin.multiplatform) }

repositories { maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }

kotlin {
    tasks.withType<JavaExec> { jvmArgs("--enable-native-access=ALL-UNNAMED") }
    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_22 }
        compilations {
            val main by getting
            val mainClassName = "com.littlekt.examples.JvmRunnerKt"
            tasks {
                register<Copy>("copyResources") {
                    group = "package"
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    dependsOn(named("jvmProcessResources"))
                    from(main.output.resourcesDir)
                    destinationDir = File("${layout.buildDirectory.asFile.get()}/publish")
                }
                register<Jar>("packageFatJar") {
                    group = "package"
                    archiveClassifier.set("all")
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    dependsOn(named("jvmJar"))
                    dependsOn(named("copyResources"))
                    manifest { attributes["Main-Class"] = mainClassName }
                    destinationDirectory.set(File("${layout.buildDirectory.asFile.get()}/publish/"))
                    from(
                        main.runtimeDependencyFiles.map { if (it.isDirectory) it else zipTree(it) },
                        main.output.classesDirs,
                    )
                    doLast {
                        logger.lifecycle(
                            "[LittleKt] The packaged jar is available at: ${outputs.files.first().parent}"
                        )
                    }
                }
                if (Os.isFamily(Os.FAMILY_MAC)) {
                    register<JavaExec>("jvmRun") {
                        jvmArgs(
                            "-XstartOnFirstThread",
                            "--add-opens=java.base/java.lang=ALL-UNNAMED",
                            "--enable-native-access=ALL-UNNAMED"
                        )
                        mainClass.set(mainClassName)
                        kotlin {
                            val mainCompile = targets["jvm"].compilations["main"]
                            dependsOn(mainCompile.compileAllTaskName)
                            classpath(
                                { mainCompile.output.allOutputs.files },
                                (configurations["jvmRuntimeClasspath"]),
                            )
                        }
                    }
                }
            }
        }
    }

    js {
        binaries.executable()
        browser {
            testTask { useKarma { useChromeHeadless() } }
            commonWebpackConfig {
                devServer =
                    (devServer
                            ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
                                .DevServer())
                        .copy(open = mapOf("app" to mapOf("name" to "chrome")))
            }
        }

        this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)

        compilerOptions { sourceMap = true }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(project(":core"))
                implementation(project(":scene-graph"))
            }
        }

        val jsMain by getting { dependencies { implementation(libs.kotlinx.html.js) } }
    }
}
