pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Add Maven Central mirror as fallback for GitHub Actions
        maven {
            url = uri("https://repo1.maven.org/maven2/")
        }
    }
}

rootProject.name = "CamperGas"
include(":app")
