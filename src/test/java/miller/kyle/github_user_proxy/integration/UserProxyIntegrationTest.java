package miller.kyle.github_user_proxy.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import miller.kyle.github_user_proxy.dto.GitHubRepoResponse;
import miller.kyle.github_user_proxy.dto.GitHubUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserProxyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void getUserData_shouldReturnCompleteUserDataWithRepos() throws Exception {
        // Arrange
        String username = "octocat";

        // Mock GitHub user response
        GitHubUserResponse mockUser = new GitHubUserResponse();
        mockUser.setLogin("octocat");
        mockUser.setName("The Octocat");
        mockUser.setAvatarUrl("https://avatars.githubusercontent.com/u/583231?v=4");
        mockUser.setLocation("San Francisco");
        mockUser.setEmail(null);
        mockUser.setUrl("https://api.github.com/users/octocat");
        mockUser.setCreatedAt("2011-01-25T18:44:36Z");

        // Mock GitHub repos response
        GitHubRepoResponse repo1 = new GitHubRepoResponse();
        repo1.setName("Hello-World");
        repo1.setUrl("https://api.github.com/repos/octocat/Hello-World");

        GitHubRepoResponse repo2 = new GitHubRepoResponse();
        repo2.setName("boysenberry-repo-1");
        repo2.setUrl("https://api.github.com/repos/octocat/boysenberry-repo-1");

        List<GitHubRepoResponse> mockRepos = Arrays.asList(repo1, repo2);

        // Setup mock server expectations
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

        // Act & Assert
        mockMvc.perform(get("/api/users/{username}", username))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user_name").value("octocat"))
                .andExpect(jsonPath("$.display_name").value("The Octocat"))
                .andExpect(jsonPath("$.avatar").value("https://avatars.githubusercontent.com/u/583231?v=4"))
                .andExpect(jsonPath("$.geo_location").value("San Francisco"))
                .andExpect(jsonPath("$.email").isEmpty())
                .andExpect(jsonPath("$.url").value("https://api.github.com/users/octocat"))
                .andExpect(jsonPath("$.created_at").exists())
                .andExpect(jsonPath("$.repos").isArray())
                .andExpect(jsonPath("$.repos.length()").value(2))
                .andExpect(jsonPath("$.repos[0].name").value("Hello-World"))
                .andExpect(jsonPath("$.repos[0].url").value("https://api.github.com/repos/octocat/Hello-World"))
                .andExpect(jsonPath("$.repos[1].name").value("boysenberry-repo-1"))
                .andExpect(jsonPath("$.repos[1].url").value("https://api.github.com/repos/octocat/boysenberry-repo-1"));

        mockServer.verify();
    }

    @Test
    void getUserData_shouldReturn404WhenUserNotFound() throws Exception {
        // Arrange
        String username = "nonexistentuser";

        mockServer.expect(requestTo("https://api.github.com/users/" + username))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // Act & Assert
        mockMvc.perform(get("/api/users/{username}", username))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("GitHub user not found: nonexistentuser"))
                .andExpect(jsonPath("$.status").value(404));

        mockServer.verify();
    }

    @Test
    void getUserData_shouldHandleEmptyRepositories() throws Exception {
        // Arrange
        String username = "emptyrepouser";

        GitHubUserResponse mockUser = new GitHubUserResponse();
        mockUser.setLogin("emptyrepouser");
        mockUser.setName("Empty Repo User");
        mockUser.setAvatarUrl("https://avatars.githubusercontent.com/u/123?v=4");
        mockUser.setLocation("Nowhere");
        mockUser.setEmail("test@example.com");
        mockUser.setUrl("https://api.github.com/users/emptyrepouser");
        mockUser.setCreatedAt("2020-01-01T00:00:00Z");

        mockServer.expect(requestTo("https://api.github.com/users/" + username))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(mockUser)));

        mockServer.expect(requestTo("https://api.github.com/users/" + username + "/repos"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[]"));

        // Act & Assert
        mockMvc.perform(get("/api/users/{username}", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value("emptyrepouser"))
                .andExpect(jsonPath("$.repos").isArray())
                .andExpect(jsonPath("$.repos.length()").value(0));

        mockServer.verify();
    }

    @Test
    void getUserData_shouldHandleGitHubApiError() throws Exception {
        // Arrange
        String username = "testuser";

        mockServer.expect(requestTo("https://api.github.com/users/" + username))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        // Act & Assert
        mockMvc.perform(get("/api/users/{username}", username))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.status").value(502));

        mockServer.verify();
    }
}
