package miller.kyle.github_user_proxy.controller;

import miller.kyle.github_user_proxy.dto.UserProxyResponse;
import miller.kyle.github_user_proxy.service.UserProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for the GitHub User Proxy API
 */
@RestController
@RequestMapping("/api/users")
public class UserProxyController {

    private static final Logger logger = LoggerFactory.getLogger(UserProxyController.class);

    private final UserProxyService userProxyService;

    public UserProxyController(UserProxyService userProxyService) {
        this.userProxyService = userProxyService;
    }

    /**
     * Get GitHub user data with repositories
     *
     * @param username GitHub username
     * @return UserProxyResponse with user data and repositories
     */
    @GetMapping("/{username}")
    public ResponseEntity<UserProxyResponse> getUserData(@PathVariable String username) {
        logger.info("Received request for username: {}", username);

        UserProxyResponse response = getCachedUserData(username);

        return ResponseEntity.ok(response);
    }

    /**
     * This private function is required to cache the response from the service without adding caching to the
     * service layer of the application. It would be nice to be able to cache the entire ResponseEntity, but it doesn't
     * have a default constructor so Jackson doesn't play nice with caching it.
     *
     * @param username GitHub username
     * @return UserProxyResponse with user data and repositories
     */
    @Cacheable(value = "github-users", key = "#username")
    private UserProxyResponse getCachedUserData(String username) {
        logger.debug("Cache miss - fetching data for username: {}", username);
        return userProxyService.getUserData(username);
    }
}
