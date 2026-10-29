package org.example.jobportalappbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Starts the job-portal API.
 *
 * <p>{@code @SpringBootApplication} is three annotations in one:
 * {@code @Configuration} (this class may declare beans),
 * {@code @EnableAutoConfiguration} (Boot configures Tomcat, JPA, Security, Jackson, and the rest from the classpath),
 * and {@code @ComponentScan} (find {@code @RestController}, {@code @Service}, and the other stereotypes
 * in this package and below).
 *
 * <p>{@code @ConfigurationPropertiesScan} registers records such as {@code JwtProperties}
 * that carry {@code @ConfigurationProperties}.
 *
 * <p>Package map (each package has one job):
 * <ul>
 *   <li>{@code controller} — HTTP in and out. No database code.</li>
 *   <li>{@code service} — use cases: create a job, register a user, and so on.</li>
 *   <li>{@code repository} — Spring Data interfaces. Spring writes the SQL.</li>
 *   <li>{@code model} — JPA entities, the tables.</li>
 *   <li>{@code dto} — records that cross the HTTP boundary. Entities never do.</li>
 *   <li>{@code security} — password checks and JWT.</li>
 *   <li>{@code config} — beans and {@code app.*} settings.</li>
 *   <li>{@code exception} — turn failures into RFC 9457 responses.</li>
 *   <li>{@code aop} — logging and timing that wrap the job service.</li>
 * </ul>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class JobPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobPortalApplication.class, args);
    }
}
