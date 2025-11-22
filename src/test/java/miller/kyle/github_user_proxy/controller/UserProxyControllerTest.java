package miller.kyle.github_user_proxy.controller;

import miller.kyle.github_user_proxy.dto.RepoInfo;
import miller.kyle.github_user_proxy.dto.UserProxyResponse;
import miller.kyle.github_user_proxy.exception.UserNotFoundException;
import miller.kyle.github_user_proxy.service.UserProxyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserProxyController.class)
class UserProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProxyService userProxyService;

    private UserProxyResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = new UserProxyResponse();
        mockResponse.setUserName("octocat");
        mockResponse.setDisplayName("The Octocat");
        mockResponse.setAvatar("https://avatars.githubusercontent.com/u/583231?v=4");
        mockResponse.setGeoLocation("San Francisco");
        mockResponse.setEmail(null);
        mockResponse.setUrl("https://api.github.com/users/octocat");
        mockResponse.setCreatedAt("Tue, 25 Jan 2011 18:44:36 GMT");

        RepoInfo repo1 = new RepoInfo("Hello-World", "https://api.github.com/repos/octocat/Hello-World");
        RepoInfo repo2 = new RepoInfo("boysenberry-repo-1", "https://api.github.com/repos/octocat/boysenberry-repo-1");
        mockResponse.setRepos(Arrays.asList(repo1, repo2));
    }

    @Test
    void getUserData_shouldReturnUserData() throws Exception {
        // Arrange
        String username = "octocat";
        when(userProxyService.getUserData(username)).thenReturn(mockResponse);

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
                .andExpect(jsonPath("$.created_at").value("Tue, 25 Jan 2011 18:44:36 GMT"))
                .andExpect(jsonPath("$.repos").isArray())
                .andExpect(jsonPath("$.repos.length()").value(2))
                .andExpect(jsonPath("$.repos[0].name").value("Hello-World"))
                .andExpect(jsonPath("$.repos[0].url").value("https://api.github.com/repos/octocat/Hello-World"));

        verify(userProxyService, times(1)).getUserData(username);
    }

    @Test
    void getUserData_shouldReturn404WhenUserNotFound() throws Exception {
        // Arrange
        String username = "nonexistentuser";
        when(userProxyService.getUserData(username))
                .thenThrow(new UserNotFoundException(username));

        // Act & Assert
        mockMvc.perform(get("/api/users/{username}", username))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("GitHub user not found: nonexistentuser"))
                .andExpect(jsonPath("$.status").value(404));

        verify(userProxyService, times(1)).getUserData(username);
    }

    @Test
    void getUserData_shouldHandleSpecialCharactersInUsername() throws Exception {
        // Arrange
        String username = "user-name_123";
        when(userProxyService.getUserData(username)).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/users/{username}", username))
                .andExpect(status().isOk());

        verify(userProxyService, times(1)).getUserData(username);
    }
}
