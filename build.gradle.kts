import java.io.ByteArrayOutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

plugins {
    java
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.2.2")
    }
}

dependencies {
    // Use JUnit test framework.
    testImplementation(libs.junit)

    // This dependency is used by the application.
    implementation(libs.guava)
    implementation("com.amazonaws", "aws-lambda-java-runtime-interface-client", "2.3.2")
    implementation("com.amazonaws", "aws-lambda-java-events", "3.15.0")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.marshallArts.keeey.LambdaMain"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

abstract class DockerBuildTask
@Inject constructor(private var execOperations: ExecOperations) : DefaultTask() {

    @get:Input
    abstract val accountId: Property<String>

    @get:Input
    abstract val region: Property<String>

    @get:Input
    abstract val repoName: Property<String>

    @get:OutputFile
    abstract val digestFile: RegularFileProperty

    init {
        digestFile.convention(project.layout.buildDirectory.file("digest"))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun buildAndPublish() {
        val registryUri = "${accountId.get()}.dkr.ecr.${region.get()}.amazonaws.com/${repoName.get()}"
        val tag = "${registryUri}:latest"

        execOperations.exec {
            commandLine("docker", "build", "--tag", tag, ".")
        }

        logger.lifecycle("Build Complete")

        execOperations.exec {
            commandLine(
                "bash",
                "-c",
                "aws ecr get-login-password --region ${region.get()}"
                    + " | "
                    + "docker login --username AWS --password-stdin $registryUri"
            )
        }

        logger.lifecycle("Login Complete")

        execOperations.exec {
            commandLine(
                "docker",
                "push",
                tag
            )
        }

        logger.lifecycle("Push Complete")

        val shaDigest = ByteArrayOutputStream().use { stream ->
            execOperations.exec {
                standardOutput = stream
                commandLine("docker", "manifest", "inspect", "--verbose", tag)
            }
            val json = Json.decodeFromString<JsonObject>(stream.toString())

            logger.lifecycle("Manifest Json: '$json'")
            json.getValue("Descriptor")
                .jsonObject
                .getValue("digest")
                .jsonPrimitive
                .content
                .removePrefix("sha256:")
        }

        logger.lifecycle("Writing sha: '${shaDigest}'")
        digestFile.get().asFile.writeText(shaDigest)
    }
}
tasks.register("docker-publish", DockerBuildTask::class) {
    dependsOn(tasks.build)
    accountId.set(project.property("account_id") as String)
    region.set(project.property("region") as String)
    repoName.set(project.property("repo_name") as String)
}