package miller.kyle.github_user_proxy.exception;

/**
 * Exception thrown when GitHub API returns an error
 */
public class GitHubApiException extends RuntimeException {

    public GitHubApiException(String message) {
        super(message);
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
