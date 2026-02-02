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

    // Jacoco 커버리지 제외 대상 (codecov.yml과 동일하게 설정)
    val jacocoExcludes = listOf(
        "**/*\$default*",                    // Kotlin default parameter methods
        "**/config/*",                        // Config 클래스
        "**/config/**",                       // Config 하위 클래스
        "**/*Config.class",                   // Config로 끝나는 클래스
        "**/*Config\$*.class",                // Config 내부 클래스
        "**/*Application.class",              // Application 클래스
        "**/*Application\$*.class",           // Application 내부 클래스
        "**/dto/*",                           // DTO 클래스
        "**/dto/**",                          // DTO 하위 클래스
        "**/exception/*",                     // Exception 클래스
        "**/exception/**"                     // Exception 하위 클래스
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
