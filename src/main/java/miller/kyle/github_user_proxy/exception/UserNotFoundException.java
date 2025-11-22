package miller.kyle.github_user_proxy.exception;

/**
 * Exception thrown when a GitHub user is not found
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String username) {
        super("GitHub user not found: " + username);
    }
}
