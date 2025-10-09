pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // This line causes the error if you don't centralize repositories
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "i"
include(":connectX")