plugins {
    application
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.bundles.log4j)
    implementation(libs.minimessage)
    implementation(libs.clikt)

    implementation(libs.party.api.lite) {
        exclude("io.netty")
        exclude("app.simplecloud")
        exclude("io.grpc")
        exclude("com.google.protobuf")
        exclude("com.google.common")
    }
}

application {
    mainClass.set("club.mcsports.droplet.queue.launcher.LauncherKt")
}