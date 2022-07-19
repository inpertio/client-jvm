plugins {
    id("inpertio-conventions")
}

dependencies {
    api(project(":inpertio-client-jvm-common"))

    implementation("tech.harmonysoft:harmonysoft-common:${Version.HARMONYSOFT_LIBS}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Version.KOTLIN_REFLECT}")
}