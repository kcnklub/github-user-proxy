package miller.kyle.github_user_proxy.client;

import miller.kyle.github_user_proxy.dto.GitHubRepoResponse;
import miller.kyle.github_user_proxy.dto.GitHubUserResponse;
import miller.kyle.github_user_proxy.exception.GitHubApiException;
import miller.kyle.github_user_proxy.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Client for interacting with GitHub's REST API
 */
@Component
public class GitHubApiClient {

    private static final Logger logger = LoggerFactory.getLogger(GitHubApiClient.class);

    private final RestTemplate restTemplate;
    private final String githubApiBaseUrl;

    public GitHubApiClient(
            RestTemplate restTemplate,
            @Value("${github.api.base-url}") String githubApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.githubApiBaseUrl = githubApiBaseUrl;
    }

    /**
     * Fetch user information from GitHub
     *
     * @param username GitHub username
     * @return GitHubUserResponse with user data
     * @throws UserNotFoundException if user doesn't exist
     * @throws GitHubApiException    if GitHub API returns an error
     */
    public GitHubUserResponse getUser(String username) {
        String url = githubApiBaseUrl + "/users/" + username;
        logger.debug("Fetching user data from GitHub: {}", url);

        try {
            ResponseEntity<GitHubUserResponse> response = restTemplate.getForEntity(
                    url,
                    GitHubUserResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.debug("Successfully fetched user data for: {}", username);
                return response.getBody();
            } else {
                throw new GitHubApiException("Unexpected response from GitHub API");
            }
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("User not found: {}", username);
            throw new UserNotFoundException(username);
        } catch (HttpClientErrorException e) {
            logger.error("GitHub API error for user {}: {} - {}", username, e.getStatusCode(), e.getMessage());
            throw new GitHubApiException("GitHub API error: " + e.getStatusCode());
        } catch (Exception e) {
            logger.error("Error fetching user data for {}: {}", username, e.getMessage());
            throw new GitHubApiException("Failed to fetch user data from GitHub", e);
        }
    }

    /**
     * Fetch user's repositories from GitHub
     *
     * @param username GitHub username
     * @return List of repositories
     * @throws GitHubApiException if GitHub API returns an error
     */
    public List<GitHubRepoResponse> getUserRepos(String username) {
        String url = githubApiBaseUrl + "/users/" + username + "/repos";
        logger.debug("Fetching repositories from GitHub: {}", url);

        try {
            ResponseEntity<List<GitHubRepoResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.debug("Successfully fetched {} repositories for user: {}",
                        response.getBody().size(), username);
                return response.getBody();
            } else {
                throw new GitHubApiException("Unexpected response from GitHub API");
            }
        } catch (HttpClientErrorException e) {
            logger.error("GitHub API error for repos of {}: {} - {}",
                    username, e.getStatusCode(), e.getMessage());
            throw new GitHubApiException("GitHub API error: " + e.getStatusCode());
        } catch (Exception e) {
            logger.error("Error fetching repositories for {}: {}", username, e.getMessage());
            throw new GitHubApiException("Failed to fetch repositories from GitHub", e);
        }
    }
}
