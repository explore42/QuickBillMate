pluginManagement {
    repositories {
        // 国内镜像优先
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("org\\.jetbrains\\.kotlin.*")
            }
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }

        // 保留官方源作为 fallback
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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        // 国内镜像：代理 Google Maven（包含所有 androidx.compose.*）
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }

        // 国内镜像：代理 Maven Central
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            content {
                includeGroupByRegex("org\\.jetbrains.*")
                includeGroupByRegex("org\\.gradle.*")
                // 其他非 Google/非 Android 的依赖
            }
        }

        // 官方 fallback（镜像失效时使用）
        google()
        mavenCentral()
    }
}

rootProject.name = "QuickBillMate"
include(":app")