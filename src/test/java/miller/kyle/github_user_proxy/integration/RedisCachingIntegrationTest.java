package miller.kyle.github_user_proxy.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import miller.kyle.github_user_proxy.dto.GitHubRepoResponse;
import miller.kyle.github_user_proxy.dto.GitHubUserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RedisCachingIntegrationTest {

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @AfterEach
    void tearDown() {
        // Clear all caches after each test to ensure test isolation
        cacheManager.getCacheNames().forEach(cacheName ->
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear()
        );
    }

    @Test
    void getUserData_shouldCacheResponse_whenCalledMultipleTimes() throws Exception {
        // Arrange
        String username = "octocat";

        // Mock GitHub user response
        GitHubUserResponse mockUser = createMockUser(username);

        // Mock GitHub repos response
        List<GitHubRepoResponse> mockRepos = createMockRepos();

        // Setup mock server to expect ONLY ONE call to GitHub API (cache miss on first request)
        mockServer.expect(requestTo("https://api.github.com/users/" + username))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(mockUser)));

        mockServer.expect(requestTo("https://api.github.com/users/" + username + "/repos"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(mockRepos)));

        // Act - First request (cache miss)
        mockMvc.perform(get("/api/users/{username}", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value("octocat"))
                .andExpect(jsonPath("$.display_name").value("The Octocat"));

        // Verify GitHub API was called once
        mockServer.verify();

        // Reset mock server - if GitHub API is called again, the test will fail
        mockServer.reset();

        // Act - Second request (should be cache hit - NO GitHub API call expected)
        mockMvc.perform(get("/api/users/{username}", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value("octocat"))
                .andExpect(jsonPath("$.display_name").value("The Octocat"));

        // Act - Third request (should also be cache hit)
        mockMvc.perform(get("/api/users/{username}", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value("octocat"))
                .andExpect(jsonPath("$.display_name").value("The Octocat"));

        // Verify no additional GitHub API calls were made
        mockServer.verify();
    }

    @Test
    void getUserData_shouldCacheIndependently_forDifferentUsers() throws Exception {
        // Arrange
        String user1 = "octocat";
        String user2 = "torvalds";

        // Mock responses for user1
        GitHubUserResponse mockUser1 = createMockUser(user1);
        List<GitHubRepoResponse> mockRepos1 = createMockRepos();

        // Mock responses for user2
        GitHubUserResponse mockUser2 = createMockUser(user2);
        mockUser2.setName("Linus Torvalds");
        List<GitHubRepoResponse> mockRepos2 = createMockRepos();

        // Setup mock server - expect one call for each user
        mockServer.expect(requestTo("https://api.github.com/users/" + user1))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(mockUser1)));

        mockServer.expect(requestTo("https://api.github.com/users/" + user1 + "/repos"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(mockRepos1)));

        mockServer.expect(requestTo("https://api.github.com/users/" + user2))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(mockUser2)));

        mockServer.expect(requestTo("https://api.github.com/users/" + user2 + "/repos"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(mockRepos2)));

        // Act - First request for user1 (cache miss)
        mockMvc.perform(get("/api/users/{username}", user1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value(user1))
                .andExpect(jsonPath("$.display_name").value("The Octocat"));

        // Act - First request for user2 (cache miss)
        mockMvc.perform(get("/api/users/{username}", user2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value(user2))
                .andExpect(jsonPath("$.display_name").value("Linus Torvalds"));

        // Verify both users' data was fetched from GitHub API
        mockServer.verify();

        // Reset mock server
        mockServer.reset();

        // Act - Second request for user1 (cache hit)
        mockMvc.perform(get("/api/users/{username}", user1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value(user1))
                .andExpect(jsonPath("$.display_name").value("The Octocat"));

        // Act - Second request for user2 (cache hit)
        mockMvc.perform(get("/api/users/{username}", user2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value(user2))
                .andExpect(jsonPath("$.display_name").value("Linus Torvalds"));

        // Verify no additional GitHub API calls were made
        mockServer.verify();
    }

    @Test
    void getUserData_shouldNotCacheErrors() throws Exception {
        // Arrange
        String username = "nonexistentuser";

        // Setup mock server to expect TWO calls (errors should not be cached)
        mockServer.expect(requestTo("https://api.github.com/users/" + username))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        mockServer.expect(requestTo("https://api.github.com/users/" + username))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // Act - First request (404 error)
        mockMvc.perform(get("/api/users/{username}", username))
                .andExpect(status().isNotFound());

        // Act - Second request (should make another GitHub API call since errors aren't cached)
        mockMvc.perform(get("/api/users/{username}", username))
                .andExpect(status().isNotFound());

        // Verify both requests hit the GitHub API
        mockServer.verify();
    }

    // Helper methods to create mock data

    private GitHubUserResponse createMockUser(String username) {
        GitHubUserResponse mockUser = new GitHubUserResponse();
        mockUser.setLogin(username);
        mockUser.setName("The Octocat");
        mockUser.setAvatarUrl("https://avatars.githubusercontent.com/u/583231?v=4");
        mockUser.setLocation("San Francisco");
        mockUser.setEmail(null);
        mockUser.setUrl("https://api.github.com/users/" + username);
        mockUser.setCreatedAt("2011-01-25T18:44:36Z");
        return mockUser;
    }

    private List<GitHubRepoResponse> createMockRepos() {
        GitHubRepoResponse repo1 = new GitHubRepoResponse();
        repo1.setName("Hello-World");
        repo1.setUrl("https://api.github.com/repos/octocat/Hello-World");

        GitHubRepoResponse repo2 = new GitHubRepoResponse();
        repo2.setName("boysenberry-repo-1");
        repo2.setUrl("https://api.github.com/repos/octocat/boysenberry-repo-1");

        return Arrays.asList(repo1, repo2);
    }
}
