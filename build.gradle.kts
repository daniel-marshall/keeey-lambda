import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

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

    @TaskAction
    fun buildAndPublish() {
        val registry_uri = "${account_id.get()}.dkr.ecr.${region.get()}.amazonaws.com"
        val registry = "${registry_uri}/${repo_name.get()}"
        execOperations.exec {
            commandLine("ls", "-la")
        }
        execOperations.exec {
            commandLine("docker", "build", "--tag", registry, ".")
        }

        logger.info("Build Complete")

        var stdout = ByteArrayOutputStream()
        execOperations.exec {
            standardOutput = stdout
            commandLine(
                "aws",
                "ecr",
                "get-login-password",
                "--region",
                region.get()
            )
        }

        val stdin = PipedInputStream()
        val pipedout = PipedOutputStream(stdin)

        execOperations.exec {
            standardInput = stdin
            commandLine(
                "docker",
                "login",
                "--username",
                "AWS",
                "--password-stdin",
                registry_uri
            )
        }

        stdout.writeTo(pipedout)

        logger.info("Login Complete")

        execOperations.exec {
            commandLine(
                "docker",
                "push",
                registry
            )
        }

        logger.info("Push Complete")
    }
}
tasks.register("docker-publish", DockerBuildTask::class) {
    dependsOn(tasks.build)
    account_id.set(project.property("account_id") as String)
    region.set(project.property("region") as String)
    repo_name.set(project.property("repo_name") as String)
}
