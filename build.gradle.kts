import java.io.ByteArrayOutputStream

plugins {
    java
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use JUnit test framework.
    testImplementation(libs.junit)

    // This dependency is used by the application.
    implementation(libs.guava)
    implementation("com.amazonaws", "aws-lambda-java-runtime-interface-client", "2.3.2")
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
    from(configurations.runtimeClasspath.get().map({ if (it.isDirectory) it else zipTree(it) }))
}

abstract class DockerBuildTask
@Inject constructor(private var execOperations: ExecOperations) : DefaultTask() {

    @get:Input
    abstract val account_id: Property<String>

    @get:Input
    abstract val region: Property<String>

    @get:Input
    abstract val repo_name: Property<String>

    @get:OutputFile
    abstract val digest_file: RegularFileProperty

    init {
        digest_file.convention(project.layout.buildDirectory.file("digest"))
    }

    @TaskAction
    fun buildAndPublish() {
        val registry_uri = "${account_id.get()}.dkr.ecr.${region.get()}.amazonaws.com"
        val tag = "${registry_uri}/${repo_name.get()}:latest"

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
                    + "docker login --username AWS --password-stdin ${registry_uri}"
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

        var shaDigest = ByteArrayOutputStream().use { stream ->
            execOperations.exec {
                standardOutput = stream
                commandLine("docker", "inspect", "--format=\"{{index .RepoDigests 0}}\"", tag)
            }

            stream.toString();
        }

        logger.lifecycle("Writing sha ${shaDigest}")
        digest_file.get().asFile.writeText(shaDigest)
    }
}
tasks.register("docker-publish", DockerBuildTask::class) {
    dependsOn(tasks.build)
    account_id.set(project.property("account_id") as String)
    region.set(project.property("region") as String)
    repo_name.set(project.property("repo_name") as String)
}
