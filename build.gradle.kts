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

// Jacoco 설정 (커버리지 체크는 Codecov에서)
subprojects {
    apply(plugin = "jacoco")

    configure<JacocoPluginExtension> {
        toolVersion = "0.8.12"
    }

    afterEvaluate {
        tasks.withType<Test> {
            finalizedBy("jacocoTestReport")
        }

        tasks.withType<JacocoReport> {
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }
    }
}
