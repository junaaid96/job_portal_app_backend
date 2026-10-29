package org.example.jobportalappbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import jakarta.servlet.Filter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full application context against PostgreSQL in Testcontainers.
 *
 * <p>{@code @SpringBootTest} boots the real beans (security, JPA, validation), not mocks of them.
 * {@code @AutoConfigureMockMvc} is required on Boot 4: {@code @SpringBootTest} no longer provides {@link MockMvc} by itself.
 * {@code webEnvironment = RANDOM_PORT} also starts Tomcat so one test can observe the thread that serves a real request.
 * {@code @ServiceConnection} copies the container's JDBC URL over {@code spring.datasource.*}.
 *
 * <p>The container is {@code static} so JUnit starts it once for every test in this class.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(JobPortalApiTests.VirtualThreadCapture.class)
class JobPortalApiTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    // Field injection is normal in tests: JUnit creates this class, then Spring fills the fields.
    // Production code in this project uses constructors instead.
    @Autowired
    private MockMvc mockMvc;

    /** Port of the real Tomcat started by webEnvironment = RANDOM_PORT. MockMvc does not use it. */
    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void registerLoginAndJobLifecycle() throws Exception {
        String owner = uniqueUsername();
        String intruder = uniqueUsername();
        String ownerToken = registerAndLogin(owner);
        String intruderToken = registerAndLogin(intruder);

        String created = mockMvc.perform(post("/jobPost")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postProfile": "Java Developer %s",
                                  "postDescription": "Build APIs with Spring",
                                  "requiredExperience": "2",
                                  "postTechStack": ["Java", "Spring Boot"],
                                  "addedBy": "not-the-real-owner"
                                }
                                """.formatted(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").isNumber())
                .andExpect(jsonPath("$.postProfile").value(containsString("Java Developer")))
                .andExpect(jsonPath("$.requiredExperience").value(2))
                .andExpect(jsonPath("$.postTechStack[0]").value("Java"))
                .andExpect(jsonPath("$.addedBy").value(owner))
                .andReturn()
                .getResponse()
                .getContentAsString();
        int postId = com.jayway.jsonpath.JsonPath.read(created, "$.postId");

        mockMvc.perform(get("/jobPosts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.postId == %d)].addedBy".formatted(postId)).value(owner));

        mockMvc.perform(get("/jobPost/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postDescription").value("Build APIs with Spring"));

        mockMvc.perform(get("/jobPosts/search/{keyword}", "Java Developer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postId").value(postId));

        mockMvc.perform(get("/jobPosts/search/{keyword}", "java developer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(put("/jobPost")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postId": %d,
                                  "postProfile": "Stolen",
                                  "postDescription": "Nope",
                                  "requiredExperience": 1,
                                  "postTechStack": ["Java"],
                                  "addedBy": "%s"
                                }
                                """.formatted(postId, intruder)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("You are not authorized to edit this job post"));

        mockMvc.perform(put("/jobPost")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postId": %d,
                                  "postProfile": "Senior Java Developer",
                                  "postDescription": "Build APIs with Spring",
                                  "requiredExperience": 4,
                                  "postTechStack": ["Java", "PostgreSQL"],
                                  "addedBy": "somebody-else"
                                }
                                """.formatted(postId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postProfile").value("Senior Java Developer"))
                .andExpect(jsonPath("$.addedBy").value(owner))
                .andExpect(jsonPath("$.postTechStack[1]").value("PostgreSQL"));

        mockMvc.perform(delete("/jobPost/{postId}", postId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You are not authorized to delete this job post"));

        mockMvc.perform(delete("/jobPost/{postId}", postId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("This Job Post is Deleted!"));

        mockMvc.perform(get("/jobPost/{postId}", postId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("This Job Post does not exist!"));
    }

    @Test
    void loginFailureStaysPlainTextSoTheFrontendDoesNotStoreIt() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"missing-user","password":"whatever"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Invalid username or password"));
    }

    @Test
    void registerRejectsADuplicateAndDoesNotReturnThePassword() throws Exception {
        String username = uniqueUsername();
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"secret-pass"}
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"secret-pass"}
                                """.formatted(username)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Username is already taken"));
    }

    @Test
    void createRequiresATokenAndValidatesTheBody() throws Exception {
        mockMvc.perform(post("/jobPost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"postProfile":"Title","postDescription":"Body","requiredExperience":1,"postTechStack":["Java"]}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));

        String token = registerAndLogin(uniqueUsername());
        mockMvc.perform(post("/jobPost")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"postProfile":"  ","postDescription":"Body","requiredExperience":-1,"postTechStack":["Java"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.postProfile").value("Job title is required"))
                .andExpect(jsonPath("$.errors.requiredExperience").value("Required experience cannot be negative"));
    }

    @Test
    void missingJobIdAndDocsEndpoints() throws Exception {
        mockMvc.perform(get("/jobPost/{postId}", 0))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Job Portal API"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("Job Portal Backend"));

        // Security runs before the controller map. An anonymous call to a route that is not public is 401,
        // even when that route does not exist. An authenticated call falls through to MVC and is 404.
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/loadData"))
                .andExpect(status().isUnauthorized());

        String token = registerAndLogin(uniqueUsername());
        mockMvc.perform(get("/actuator/beans").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/loadData").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void tomcatHandlesRequestsOnVirtualThreads() throws Exception {
        VirtualThreadCapture.SAW_VIRTUAL_THREAD.set(false);

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/actuator/health")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(VirtualThreadCapture.SAW_VIRTUAL_THREAD).isTrue();
    }

    @Test
    void loginTokenIsNotJson() throws Exception {
        String username = uniqueUsername();
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"secret-pass"}
                                """.formatted(username)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"secret-pass"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/plain")))
                .andExpect(content().string(org.hamcrest.Matchers.matchesPattern(
                        "[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")));
    }

    private String registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"secret-pass"}
                                """.formatted(username)))
                .andExpect(status().isCreated());
        return mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"secret-pass"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static String uniqueUsername() {
        return "user-" + UUID.randomUUID();
    }

    /**
     * Records whether the real Tomcat thread for a request is a virtual thread.
     * MockMvc calls controllers on the test thread, so it cannot answer this question.
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class VirtualThreadCapture {

        static final AtomicBoolean SAW_VIRTUAL_THREAD = new AtomicBoolean(false);

        @Bean
        Filter virtualThreadCaptureFilter() {
            return (request, response, chain) -> {
                if (Thread.currentThread().isVirtual()) {
                    SAW_VIRTUAL_THREAD.set(true);
                }
                chain.doFilter(request, response);
            };
        }
    }
}
