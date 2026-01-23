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

    tasks.withType<JacocoReport> {
        reports {
            xml.required = true
            html.required = true
        }
    }

    tasks.withType<JacocoCoverageVerification> {
        violationRules {
            rule {
                limit {
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
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
