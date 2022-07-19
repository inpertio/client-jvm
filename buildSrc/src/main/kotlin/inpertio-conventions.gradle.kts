import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    `maven-publish`
    if (System.getenv("CI_ENV").isNullOrBlank()) {
        signing
    }
}

group = "tech.harmonysoft"
version = Version.APP

repositories {
    mavenCentral()
}

dependencies {
    api("javax.inject:javax.inject:${Version.JAVAX_INJECT}")

    implementation("org.jetbrains:annotations:${Version.JETBRAINS_ANNOTATIONS}")
    implementation("org.slf4j:slf4j-api:${Version.SLF4J}")

    compileOnly("org.springframework.boot:spring-boot-starter")

    testImplementation("org.junit.jupiter:junit-jupiter:${Version.JUNIT}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Version.JUNIT}")
    testImplementation("org.assertj:assertj-core:${Version.ASSERTJ}")
    testImplementation("tech.harmonysoft:harmonysoft-common-test:${Version.HARMONYSOFT_LIBS}")
}

tasks.getByName("bootJar").enabled = false

tasks.compileJava {
    options.release.set(8)
}

tasks.test {
    useJUnitPlatform()
}

tasks.dokkaJavadoc.configure {
    outputDirectory.set(buildDir.resolve("dokkaJavadoc"))
    dokkaSourceSets {
        configureEach {
            noStdlibLink.set(false)
            noJdkLink.set(false)
        }
    }
}

val docJar by tasks.creating(Jar::class) {
    dependsOn(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
    from(buildDir.resolve("dokkaJavadoc"))
}

val sourceJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

artifacts {
    archives(docJar)
    archives(sourceJar)
}

publishing {
    publications {
        create<MavenPublication>("main") {
            artifactId = project.name
            from(components["java"])
            artifact(sourceJar)
            artifact(docJar)

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