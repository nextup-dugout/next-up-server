package com.nextup.api.integration

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * 통합 테스트 기본 클래스
 *
 * Testcontainers로 PostgreSQL + PostGIS를 실행하고,
 * Flyway 마이그레이션을 적용하여 실제 DB 환경에서 테스트합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration")
abstract class IntegrationTestBase {
    companion object {
        private val postgisImage =
            DockerImageName.parse("postgis/postgis:15-3.3")
                .asCompatibleSubstituteFor("postgres")

        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(postgisImage)
                .withDatabaseName("nextup_test")
                .withUsername("test")
                .withPassword("test")

        init {
            postgres.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
