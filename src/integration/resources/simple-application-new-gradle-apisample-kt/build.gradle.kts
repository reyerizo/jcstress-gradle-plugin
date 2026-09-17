repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
}

plugins {
    id("io.github.reyerizo.gradle.jcstress")
    kotlin("jvm") version "2.1.21"
    kotlin("kapt") version "2.1.21"
}

jcstress {
    verbose = "true"
    timeMillis = "200"
    mode = "sanity"
    iterations = "1"
}
