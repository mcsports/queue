dependencies {
    api(project(":api"))
}

tasks.shadowJar {
    dependencies {
        include(project(":api"))
        include(project(":shared"))
        include(dependency(libs.mcsports.proto.java.get()))
        include(dependency(libs.mcsports.proto.kotlin.get()))
    }
    mergeServiceFiles()
    archiveFileName.set("${rootProject.name}-${project.name}.jar")
}