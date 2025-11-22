package miller.kyle.github_user_proxy.client;

import miller.kyle.github_user_proxy.dto.GitHubRepoResponse;
import miller.kyle.github_user_proxy.dto.GitHubUserResponse;
import miller.kyle.github_user_proxy.exception.GitHubApiException;
import miller.kyle.github_user_proxy.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    private GitHubApiClient gitHubApiClient;

    private final String baseUrl = "https://api.github.com";

    @BeforeEach
    void setUp() {
        gitHubApiClient = new GitHubApiClient(restTemplate, baseUrl);
    }

    @Test
    void getUser_shouldReturnUserWhenSuccessful() {
        // Arrange
        String username = "octocat";
        GitHubUserResponse mockUser = new GitHubUserResponse();
        mockUser.setLogin(username);
        mockUser.setName("The Octocat");

        ResponseEntity<GitHubUserResponse> responseEntity = ResponseEntity.ok(mockUser);
        when(restTemplate.getForEntity(anyString(), eq(GitHubUserResponse.class)))
                .thenReturn(responseEntity);

        // Act
        GitHubUserResponse result = gitHubApiClient.getUser(username);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getLogin());
        assertEquals("The Octocat", result.getName());
        verify(restTemplate, times(1)).getForEntity(
                eq(baseUrl + "/users/" + username),
                eq(GitHubUserResponse.class)
        );
    }

    @Test
    void getUser_shouldThrowUserNotFoundExceptionWhen404() {
        // Arrange
        String username = "nonexistentuser";
        when(restTemplate.getForEntity(anyString(), eq(GitHubUserResponse.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> gitHubApiClient.getUser(username));
        verify(restTemplate, times(1)).getForEntity(
                eq(baseUrl + "/users/" + username),
                eq(GitHubUserResponse.class)
        );
    }

    @Test
    void getUser_shouldThrowGitHubApiExceptionOnOtherErrors() {
        // Arrange
        String username = "testuser";
        when(restTemplate.getForEntity(anyString(), eq(GitHubUserResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        // Act & Assert
        assertThrows(GitHubApiException.class, () -> gitHubApiClient.getUser(username));
    }

    @Test
    void getUserRepos_shouldReturnRepositoriesWhenSuccessful() {
        // Arrange
        String username = "octocat";
        GitHubRepoResponse repo1 = new GitHubRepoResponse();
        repo1.setName("Hello-World");
        repo1.setUrl("https://api.github.com/repos/octocat/Hello-World");

        GitHubRepoResponse repo2 = new GitHubRepoResponse();
        repo2.setName("test-repo");
        repo2.setUrl("https://api.github.com/repos/octocat/test-repo");

        List<GitHubRepoResponse> repos = Arrays.asList(repo1, repo2);
        ResponseEntity<List<GitHubRepoResponse>> responseEntity = ResponseEntity.ok(repos);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // Act
        List<GitHubRepoResponse> result = gitHubApiClient.getUserRepos(username);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Hello-World", result.get(0).getName());
        assertEquals("test-repo", result.get(1).getName());
    }

    @Test
    void getUserRepos_shouldThrowGitHubApiExceptionOnError() {
        // Arrange
        String username = "testuser";
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        // Act & Assert
        assertThrows(GitHubApiException.class, () -> gitHubApiClient.getUserRepos(username));
    }
}
