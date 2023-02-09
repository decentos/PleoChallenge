plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-core"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
    implementation("org.quartz-scheduler:quartz:2.3.1")
}
