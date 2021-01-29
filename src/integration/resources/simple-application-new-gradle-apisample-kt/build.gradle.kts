import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    jcenter()
}

dependencies {
}

plugins {
    id("jcstress")
    kotlin("jvm") version "1.4.21"
    kotlin("kapt") version "1.4.21"
}

jcstress {
    verbose = "true"
    timeMillis = "200"
    forks = "0"
    iterations = "1"
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
