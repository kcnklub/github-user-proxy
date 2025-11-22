package miller.kyle.github_user_proxy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing repository information in our API response
 */
public class RepoInfo {

    @JsonProperty("name")
    private String name;

    @JsonProperty("url")
    private String url;

    public RepoInfo() {
    }

    public RepoInfo(String name, String url) {
        this.name = name;
        this.url = url;
    }

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
