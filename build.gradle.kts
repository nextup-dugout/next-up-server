plugins {
    kotlin("jvm") version "2.1.10" apply false
    kotlin("plugin.spring") version "2.1.10" apply false
    kotlin("plugin.jpa") version "2.1.10" apply false
    id("org.springframework.boot") version "3.4.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    jacoco
}

allprojects {
    group = "com.nextup"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "jacoco")

    configure<JacocoPluginExtension> {
        toolVersion = "0.8.12"
    }

    // Jacoco 커버리지 제외 대상 (Ant 스타일 패턴)
    val jacocoExcludes = listOf(
        // Kotlin generated
        "**/*\$default*",

        // Config classes - 모든 config 패키지
        "**/config/**",

        // Application classes
        "**/*Application*",

        // DTO classes
        "**/dto/**",

        // Exception classes
        "**/exception/**",

        // Mapper classes
        "**/mapper/**",

        // Health controllers
        "**/HealthController*"
    )

    tasks.withType<JacocoReport> {
        reports {
            xml.required = true
            html.required = true
        }
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(jacocoExcludes)
                }
            })
        )
    }

    tasks.withType<JacocoCoverageVerification> {
        violationRules {
            rule {
                limit {
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(jacocoExcludes)
                }
            })
        )
    }

    // test 태스크에 jacoco 연동
    tasks.withType<Test> {
        finalizedBy(tasks.withType<JacocoReport>())
    }
}

// 멀티모듈 통합 커버리지 리포트
tasks.register("jacocoRootReport") {
    dependsOn(subprojects.map { it.tasks.named("test") })
    dependsOn(subprojects.map { it.tasks.withType<JacocoReport>() })

    doLast {
        println("Individual module coverage reports generated.")
        println("Check each module's build/reports/jacoco/ directory.")
    }
}
