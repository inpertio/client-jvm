plugins {
    `java-library`
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    `maven-publish`
    if (System.getenv("CI_ENV").isNullOrBlank()) {
        signing
    }
}

object Version {
    const val APP = "1.1.0"
    const val JUNIT = "5.8.2"
}

group = "tech.harmonysoft"
version = Version.APP

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:23.0.0")
}

tasks.javadoc.configure {
    source = sourceSets.main.get().allJava
    options {
        this as StandardJavadocDocletOptions
        addStringOption("Xdoclint:none", "-quiet")
    }
}

val javadocJar by tasks.creating(Jar::class) {
    dependsOn(":javadoc")
    archiveClassifier.set("javadoc")
    from(buildDir.resolve("docs/javadoc"))
}

val sourceJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

artifacts {
    archives(javadocJar)
    archives(sourceJar)
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

publishing {
    publications {
        create<MavenPublication>("main") {
            artifactId = project.name
            from(components["java"])
            artifact(sourceJar)
            artifact(javadocJar)

            pom {
                name.set(project.name)
                description.set("Inpertio common jvm client stuff")
                url.set("https://github.com/inpertio/client-jvm")

                licenses {
                    license {
                        name.set("The MIT License (MIT)")
                        url.set("http://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("denis")
                        name.set("Denis Zhdanov")
                        email.set("denzhdanov@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://https://github.com/inpertio/client-jvm.git")
                    developerConnection.set("scm:git:git://https://github.com/inpertio/client-jvm.git")
                    url.set("https://github.com/inpertio/client-jvm")
                }
            }
        }
    }
}

if (System.getenv("CI_ENV").isNullOrBlank()) {
    signing {
        sign(publishing.publications["main"])
    }
}