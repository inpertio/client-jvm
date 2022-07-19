plugins {
    id("inpertio-conventions")
}

dependencies {
    api(project(":inpertio-client-jvm-api"))

    implementation("tech.harmonysoft:harmonysoft-event-bus-api:${Version.HARMONYSOFT_LIBS}")
}