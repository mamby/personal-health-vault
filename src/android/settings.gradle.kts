pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                if (providers.gradleProperty("androidKitUseMavenLocal").getOrElse("false").toBoolean()) {
                    mavenLocal()
                } else {
                    maven {
                        name = "AndroidKit"
                        url = uri("https://mamby.github.io/android-kit-docs/maven/")
                    }
                }
            }
            filter {
                includeGroup("net.mamby.androidkit")
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "PersonalHealthVaultAndroid"
include(":app")
