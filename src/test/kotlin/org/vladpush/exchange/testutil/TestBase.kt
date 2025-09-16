package org.vladpush.exchange.testutil

import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, )
@ActiveProfiles("test")
@Testcontainers
abstract class TestBase {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @LocalServerPort
    var port: Int = 0

    fun baseUrl() = "http://localhost:$port"

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:17"))
            .withDatabaseName("exchange")
            .withUsername("test")
            .withPassword("test")

        @JvmStatic
        @BeforeAll
        fun startContainers() {
            postgres.start()
        }

//        @JvmStatic
//        @AfterAll
//        fun stopContainers() {
//            postgres.stop()
//        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProps(registry: DynamicPropertyRegistry) {
            // PostgreSQL
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.datasource.driver-class-name") { postgres.driverClassName }

            // Ensure schema.sql is applied in tests
            registry.add("spring.sql.init.mode") { "always" }
            registry.add("spring.sql.init.schema-locations") { "classpath:schema.sql" }
        }
    }
}
