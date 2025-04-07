plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "org.schedo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks {
    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map {if (it.isDirectory) it else zipTree(it)})
    }
}