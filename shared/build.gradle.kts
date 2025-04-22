dependencies {
    api(libs.mcsports.proto) {
        exclude("io.grpc")
        exclude("io.netty")
    }
    api(libs.bundles.configurate)
    api(libs.bundles.simplecloud) {
        exclude("org.slf4j")
        exclude("org.apache.logging")
        exclude("io.netty")
        exclude("io.grpc")
    }
    api(libs.bundles.grpc)
}