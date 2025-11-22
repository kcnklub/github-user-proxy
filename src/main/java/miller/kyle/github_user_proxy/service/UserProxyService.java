package miller.kyle.github_user_proxy.service;

import miller.kyle.github_user_proxy.client.GitHubApiClient;
import miller.kyle.github_user_proxy.dto.GitHubRepoResponse;
import miller.kyle.github_user_proxy.dto.GitHubUserResponse;
import miller.kyle.github_user_proxy.dto.RepoInfo;
import miller.kyle.github_user_proxy.dto.UserProxyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service that orchestrates fetching and transforming GitHub user data
 */
@Service
public class UserProxyService {

    private static final Logger logger = LoggerFactory.getLogger(UserProxyService.class);
    private static final DateTimeFormatter RFC_1123_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;

    private final GitHubApiClient gitHubApiClient;

    public UserProxyService(GitHubApiClient gitHubApiClient) {
        this.gitHubApiClient = gitHubApiClient;
    }

    /**
     * Get user information and repositories from GitHub, transformed to our API format
     *
     * @param username GitHub username
     * @return UserProxyResponse with transformed data
     */
    public UserProxyResponse getUserData(String username) {
        logger.info("Fetching data for user: {}", username);

        // Fetch user data and repositories from GitHub
        GitHubUserResponse githubUser = gitHubApiClient.getUser(username);
        List<GitHubRepoResponse> githubRepos = gitHubApiClient.getUserRepos(username);

        // Transform to our response format
        UserProxyResponse response = new UserProxyResponse();
        response.setUserName(githubUser.getLogin());
        response.setDisplayName(githubUser.getName());
        response.setAvatar(githubUser.getAvatarUrl());
        response.setGeoLocation(githubUser.getLocation());
        response.setEmail(githubUser.getEmail());
        response.setUrl(githubUser.getUrl());
        response.setCreatedAt(formatDate(githubUser.getCreatedAt()));

        // Transform repositories
        List<RepoInfo> repos = githubRepos.stream()
                .map(repo -> new RepoInfo(repo.getName(), repo.getUrl()))
                .collect(Collectors.toList());
        response.setRepos(repos);

        logger.info("Successfully transformed data for user: {} with {} repositories",
                username, repos.size());

        return response;
    }

    /**
     * Format ISO 8601 date from GitHub to RFC 1123 format
     * Example: "2011-01-25T18:44:36Z" -> "Tue, 25 Jan 2011 18:44:36 GMT"
     *
     * @param isoDate ISO 8601 date string
     * @return RFC 1123 formatted date string
     */
    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return null;
        }

        try {
            ZonedDateTime dateTime = ZonedDateTime.parse(isoDate);
            return dateTime.format(RFC_1123_FORMATTER);
        } catch (Exception e) {
            logger.warn("Failed to parse date: {}, returning as-is", isoDate);
            return isoDate;
        }
    }
}
