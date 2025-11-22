package miller.kyle.github_user_proxy.service;

import miller.kyle.github_user_proxy.client.GitHubApiClient;
import miller.kyle.github_user_proxy.dto.GitHubRepoResponse;
import miller.kyle.github_user_proxy.dto.GitHubUserResponse;
import miller.kyle.github_user_proxy.dto.UserProxyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProxyServiceTest {

    @Mock
    private GitHubApiClient gitHubApiClient;

    @InjectMocks
    private UserProxyService userProxyService;

    private GitHubUserResponse mockUserResponse;
    private List<GitHubRepoResponse> mockRepoResponses;

    @BeforeEach
    void setUp() {
        // Setup mock user response
        mockUserResponse = new GitHubUserResponse();
        mockUserResponse.setLogin("octocat");
        mockUserResponse.setName("The Octocat");
        mockUserResponse.setAvatarUrl("https://avatars.githubusercontent.com/u/583231?v=4");
        mockUserResponse.setLocation("San Francisco");
        mockUserResponse.setEmail(null);
        mockUserResponse.setUrl("https://api.github.com/users/octocat");
        mockUserResponse.setCreatedAt("2011-01-25T18:44:36Z");

        // Setup mock repo responses
        GitHubRepoResponse repo1 = new GitHubRepoResponse();
        repo1.setName("Hello-World");
        repo1.setUrl("https://api.github.com/repos/octocat/Hello-World");

        GitHubRepoResponse repo2 = new GitHubRepoResponse();
        repo2.setName("boysenberry-repo-1");
        repo2.setUrl("https://api.github.com/repos/octocat/boysenberry-repo-1");

        mockRepoResponses = Arrays.asList(repo1, repo2);
    }

    @Test
    void getUserData_shouldReturnTransformedData() {
        // Arrange
        String username = "octocat";
        when(gitHubApiClient.getUser(username)).thenReturn(mockUserResponse);
        when(gitHubApiClient.getUserRepos(username)).thenReturn(mockRepoResponses);

        // Act
        UserProxyResponse result = userProxyService.getUserData(username);

        // Assert
        assertNotNull(result);
        assertEquals("octocat", result.getUserName());
        assertEquals("The Octocat", result.getDisplayName());
        assertEquals("https://avatars.githubusercontent.com/u/583231?v=4", result.getAvatar());
        assertEquals("San Francisco", result.getGeoLocation());
        assertNull(result.getEmail());
        assertEquals("https://api.github.com/users/octocat", result.getUrl());
        assertNotNull(result.getCreatedAt());
        assertTrue(result.getCreatedAt().contains("Tue, 25 Jan 2011"));

        // Verify repositories
        assertNotNull(result.getRepos());
        assertEquals(2, result.getRepos().size());
        assertEquals("Hello-World", result.getRepos().get(0).getName());
        assertEquals("https://api.github.com/repos/octocat/Hello-World", result.getRepos().get(0).getUrl());

        // Verify interactions
        verify(gitHubApiClient, times(1)).getUser(username);
        verify(gitHubApiClient, times(1)).getUserRepos(username);
    }

    @Test
    void getUserData_shouldHandleNullValues() {
        // Arrange
        String username = "testuser";
        mockUserResponse.setName(null);
        mockUserResponse.setLocation(null);
        mockUserResponse.setEmail(null);

        when(gitHubApiClient.getUser(username)).thenReturn(mockUserResponse);
        when(gitHubApiClient.getUserRepos(username)).thenReturn(mockRepoResponses);

        // Act
        UserProxyResponse result = userProxyService.getUserData(username);

        // Assert
        assertNotNull(result);
        assertNull(result.getDisplayName());
        assertNull(result.getGeoLocation());
        assertNull(result.getEmail());
    }

    @Test
    void getUserData_shouldHandleEmptyRepositories() {
        // Arrange
        String username = "emptyuser";
        when(gitHubApiClient.getUser(username)).thenReturn(mockUserResponse);
        when(gitHubApiClient.getUserRepos(username)).thenReturn(Arrays.asList());

        // Act
        UserProxyResponse result = userProxyService.getUserData(username);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getRepos());
        assertEquals(0, result.getRepos().size());
    }

    @Test
    void getUserData_shouldFormatDateCorrectly() {
        // Arrange
        String username = "octocat";
        mockUserResponse.setCreatedAt("2011-01-25T18:44:36Z");

        when(gitHubApiClient.getUser(username)).thenReturn(mockUserResponse);
        when(gitHubApiClient.getUserRepos(username)).thenReturn(mockRepoResponses);

        // Act
        UserProxyResponse result = userProxyService.getUserData(username);

        // Assert
        assertNotNull(result.getCreatedAt());
        // RFC 1123 format: "Tue, 25 Jan 2011 18:44:36 GMT"
        assertTrue(result.getCreatedAt().contains("Jan 2011"));
    }
}
