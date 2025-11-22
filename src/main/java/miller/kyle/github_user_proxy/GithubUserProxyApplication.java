package miller.kyle.github_user_proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class GithubUserProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(GithubUserProxyApplication.class, args);
	}

}
