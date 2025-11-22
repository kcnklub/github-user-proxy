# GitHub User Proxy Service

A Spring Boot REST API that proxies GitHub's API, providing a simplified interface for retrieving user information and repositories with Redis caching support.

## Quick Start

### Prerequisites
- Java 25
- Docker & Docker Compose
- Internet connection (GitHub API access)

### Running with Docker Compose

```bash
# Start Redis
docker-compose up -d

# Run the application
./gradlew bootRun

# Stop Redis when done
docker-compose down
```

The application runs on `http://localhost:8080`.

### Build and Run JAR

```bash
./gradlew build
java -jar build/libs/github-user-proxy-0.0.1-SNAPSHOT.jar
```

## API Endpoint

**GET** `/api/users/{username}`

Returns GitHub user information and repositories in a simplified format.

### Example Request
```bash
curl http://localhost:8080/api/users/octocat
```

### Example Response (200 OK)
```json
{
  "user_name": "octocat",
  "display_name": "The Octocat",
  "avatar": "https://avatars.githubusercontent.com/u/583231?v=4",
  "geo_location": "San Francisco",
  "email": null,
  "url": "https://api.github.com/users/octocat",
  "created_at": "Tue, 25 Jan 2011 18:44:36 GMT",
  "repos": [
    {
      "name": "boysenberry-repo-1",
      "url": "https://api.github.com/repos/octocat/boysenberry-repo-1"
    },
    {
      "name": "Hello-World",
      "url": "https://api.github.com/repos/octocat/Hello-World"
    }
  ]
}
```

### Error Responses
- **404 Not Found**: User doesn't exist
- **502 Bad Gateway**: GitHub API error
- **500 Internal Server Error**: Unexpected error

## Testing

### Run All Tests
```bash
./gradlew test
```

### View Test Reports
```bash
open build/reports/tests/test/index.html
```

## Architecture

### Layered Design
```
Controller → Service → Client → GitHub API
                ↓
              Redis Cache
```

### Components
- **UserProxyController**: REST endpoint handler
- **UserProxyService**: Business logic and data transformation
- **GitHubApiClient**: GitHub API communication
- **GlobalExceptionHandler**: Centralized error handling
- **DTOs**: Data transfer objects for request/response mapping

### Key Features
- Redis caching to reduce GitHub API calls
- Field name mapping (GitHub snake_case → custom format)
- Date formatting (ISO 8601 → RFC 1123)
- Constructor-based dependency injection
- Comprehensive error handling

## Configuration

Edit `src/main/resources/application.properties`:

```properties
server.port=8080
github.api.base-url=https://api.github.com
logging.level.miller.kyle.github_user_proxy=INFO
```

### Custom Port
```bash
./gradlew bootRun --args='--server.port=9090'
```

## Docker Compose Services

The `docker-compose.yml` provides:
- **Redis 7 Alpine**: In-memory cache with data persistence
- **Health checks**: Ensures Redis is ready before app starts
- **Volume persistence**: Data survives container restarts

## Project Structure

```
github-user-proxy/
├── src/main/java/miller/kyle/github_user_proxy/
│   ├── client/GitHubApiClient.java
│   ├── config/AppConfig.java
│   ├── controller/UserProxyController.java
│   ├── dto/
│   ├── exception/
│   └── service/UserProxyService.java
├── src/test/java/
├── docker-compose.yml
├── build.gradle
└── README.md
```

## Dependencies

- Spring Boot 3.5.7
- Spring Web (REST API)
- Spring Data Redis (Caching)
- Spring Boot Cache
- JUnit 5 & Mockito (Testing)
- Testcontainers (Integration tests)

## GitHub API Rate Limits

- **Unauthenticated**: 60 requests/hour
- **Authenticated**: 5,000 requests/hour

Redis caching helps avoid rate limits by storing responses.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Run `./gradlew test` to ensure all tests pass
5. Submit a pull request

## License

This project is created as a technical assessment and is provided as-is.
