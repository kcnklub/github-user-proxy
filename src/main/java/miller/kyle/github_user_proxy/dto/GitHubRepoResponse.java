package miller.kyle.github_user_proxy.dto;

/**
 * DTO representing a single repository from GitHub's /users/{username}/repos API response
 */
public class GitHubRepoResponse {

    private String name;
    private String url;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
