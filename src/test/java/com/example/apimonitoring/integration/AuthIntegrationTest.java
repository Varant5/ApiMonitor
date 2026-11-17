package com.example.apimonitoring.integration;

import com.example.apimonitoring.TestcontainersConfiguration;
import com.example.apimonitoring.entity.User;
import com.example.apimonitoring.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full authentication flow on a real database: register -> log in -> use the token.
 */
@SpringBootTest(properties = "monitoring.scheduler.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

    private static final String EMAIL = "jan.kowalski@example.com";
    private static final String PASSWORD = "Secret123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    // ---------- register ----------

    @Test
    void shouldRegisterUserAndStoreHashedPassword() throws Exception {

        register(EMAIL, PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User saved = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(saved.getPasswordHash())
                .isNotEqualTo(PASSWORD)
                .startsWith("$2");
    }

    @Test
    void shouldNormalizeEmailOnRegistration() throws Exception {

        register("  Jan.Kowalski@EXAMPLE.com ", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void shouldReturn409WhenEmailIsAlreadyRegistered() throws Exception {

        register(EMAIL, PASSWORD).andExpect(status().isCreated());

        register(EMAIL.toUpperCase(), "OtherPassword1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    void shouldReturn400WhenRegistrationDataIsInvalid() throws Exception {

        register("not-an-email", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    // ---------- login ----------

    @Test
    void shouldLoginAndReturnToken() throws Exception {

        register(EMAIL, PASSWORD).andExpect(status().isCreated());

        login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void shouldReturn401ForWrongPassword() throws Exception {

        register(EMAIL, PASSWORD).andExpect(status().isCreated());

        login(EMAIL, "WrongPassword1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.errors[0]").value("Invalid email or password"));
    }

    @Test
    void shouldReturnSameErrorForUnknownEmail() throws Exception {

        // the same message as for a wrong password -> the API does not reveal which emails exist
        login("nobody@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0]").value("Invalid email or password"));
    }

    // ---------- using the token ----------

    @Test
    void shouldAccessProtectedEndpointWithToken() throws Exception {

        register(EMAIL, PASSWORD).andExpect(status().isCreated());
        String token = obtainToken(EMAIL, PASSWORD);

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));

        mockMvc.perform(get("/api/monitors").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WithoutToken() throws Exception {

        mockMvc.perform(get("/api/monitors"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.errors[0]").value("Missing or invalid access token"));
    }

    @Test
    void shouldReturn401ForInvalidToken() throws Exception {

        mockMvc.perform(get("/api/monitors").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldReturn401ForTamperedToken() throws Exception {

        register(EMAIL, PASSWORD).andExpect(status().isCreated());
        String token = obtainToken(EMAIL, PASSWORD);

        // change one character of the signature -> signature check must fail
        // (the second-to-last char, because the last Base64url char carries only some bits of the signature)
        int position = token.length() - 2;
        char replacement = token.charAt(position) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, position) + replacement + token.substring(position + 1);

        mockMvc.perform(get("/api/monitors").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointsShouldNotRequireToken() throws Exception {

        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ---------- helpers ----------

    private org.springframework.test.web.servlet.ResultActions register(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "password": "%s" }
                        """.formatted(email, password)));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "password": "%s" }
                        """.formatted(email, password)));
    }

    private String obtainToken(String email, String password) throws Exception {
        String body = login(email, password)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
