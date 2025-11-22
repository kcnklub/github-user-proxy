# GitHub User Proxy Service

A Spring Boot REST API that serves as a proxy to GitHub's API, providing a simplified interface for retrieving user information and repositories.

## Overview

This service integrates with GitHub's REST API to fetch user data and repositories, then transforms and merges the information into a single, simplified JSON response format.

## Architecture

### Design Pattern: Layered Architecture

```
Controller Layer (UserProxyController)
    ↓
Service Layer (UserProxyService)
    ↓
Client Layer (GitHubApiClient)
    ↓
GitHub REST API
```

### Components

- **Controller Layer**: Handles HTTP requests and responses
  - `UserProxyController`: Exposes REST endpoint `/api/users/{username}`

- **Service Layer**: Business logic and data transformation
  - `UserProxyService`: Orchestrates API calls and transforms data to required format

- **Client Layer**: External API communication
  - `GitHubApiClient`: Manages HTTP communication with GitHub's API

- **DTOs**: Data Transfer Objects for request/response handling
  - `GitHubUserResponse`: Maps GitHub's `/users/{username}` response
  - `GitHubRepoResponse`: Maps GitHub's `/users/{username}/repos` response
  - `UserProxyResponse`: Our API's response format
  - `RepoInfo`: Repository information in our response
  - `ErrorResponse`: Standardized error responses

- **Exception Handling**: Centralized error handling
  - `GlobalExceptionHandler`: Handles all exceptions globally
  - `UserNotFoundException`: Thrown when GitHub user doesn't exist
  - `GitHubApiException`: Thrown on GitHub API errors

### Key Design Decisions

1. **RestTemplate over WebClient**: Used RestTemplate for simplicity since this is a synchronous API. WebClient would be preferred for reactive applications.

2. **No Caching (Initially)**: Caching was skipped in the initial implementation to keep it simple. The Spring Boot Cache starter is already included in dependencies and can be easily added later with `@Cacheable` annotations.

3. **Field Mapping**: GitHub API uses snake_case, but our API returns data with the exact field names specified in requirements (user_name, display_name, etc.).

4. **Date Formatting**: GitHub returns ISO 8601 dates (`2011-01-25T18:44:36Z`), which are converted to RFC 1123 format (`Tue, 25 Jan 2011 18:44:36 GMT`) to match the requirements.

5. **Error Handling**:
   - 404 for users not found
   - 502 Bad Gateway for GitHub API errors
   - 500 for unexpected errors

6. **Dependency Injection**: Constructor-based injection for better testability and immutability.

## API Documentation

### Endpoint

**GET** `/api/users/{username}`

Retrieves GitHub user information along with their repositories.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| username | String | Yes | GitHub username |

#### Success Response (200 OK)

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

#### Error Responses

**404 Not Found** - User doesn't exist
```json
{
  "error": "Not Found",
  "message": "GitHub user not found: username",
  "status": 404
}
```

**502 Bad Gateway** - GitHub API error
```json
{
  "error": "Bad Gateway",
  "message": "Error communicating with GitHub API: ...",
  "status": 502
}
```

**500 Internal Server Error** - Unexpected error
```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "status": 500
}
```

## Prerequisites

- **Java 25** (as configured in the project)
- **Gradle 9.1.0** (wrapper included)
- Internet connection to access GitHub API

## Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd github-user-proxy
   ```

2. **Build the project**
   ```bash
   ./gradlew clean build
   ```

   This will:
   - Compile the source code
   - Run all tests (unit and integration)
   - Generate a JAR file in `build/libs/`

## Running the Application

### Option 1: Using Gradle

```bash
./gradlew bootRun
```

### Option 2: Using the JAR file

```bash
java -jar build/libs/github-user-proxy-0.0.1-SNAPSHOT.jar
```

The application will start on **port 8080** by default.

You should see output similar to:
```
Started GithubUserProxyApplication in X.XXX seconds
```

## Testing the Application

### Run All Tests

```bash
./gradlew test
```

This runs:
- Unit tests for Controller, Service, and Client layers
- Integration tests for the complete request/response flow

### Test Reports

After running tests, view the HTML report:
```bash
open build/reports/tests/test/index.html
```

### Manual Testing with curl

#### Get user data for "octocat"
```bash
curl http://localhost:8080/api/users/octocat
```

#### Test with a non-existent user (should return 404)
```bash
curl -i http://localhost:8080/api/users/thisuserdoesnotexist12345
```

#### Test with different users
```bash
curl http://localhost:8080/api/users/torvalds
curl http://localhost:8080/api/users/gvanrossum
curl http://localhost:8080/api/users/rails
```

### Testing with HTTPie (alternative)

```bash
http GET localhost:8080/api/users/octocat
```

## Configuration

Configuration is managed in `src/main/resources/application.properties`:

```properties
# Server configuration
server.port=8080

# GitHub API configuration
github.api.base-url=https://api.github.com

# Logging configuration
logging.level.miller.kyle.github_user_proxy=INFO
```

### Customizing Port

To run on a different port:

```bash
./gradlew bootRun --args='--server.port=9090'
```

Or:
```bash
java -jar build/libs/github-user-proxy-0.0.1-SNAPSHOT.jar --server.port=9090
```

## Rate Limiting Considerations

GitHub API has rate limits:
- **Unauthenticated requests**: 60 requests/hour per IP
- **Authenticated requests**: 5,000 requests/hour

### Future Enhancement: Caching

To avoid rate limiting, caching can be added:

1. Already included: `spring-boot-starter-cache` dependency
2. Add `@EnableCaching` to the main application class
3. Add `@Cacheable` to service methods:
   ```java
   @Cacheable(value = "users", key = "#username")
   public UserProxyResponse getUserData(String username) {
       // ...
   }
   ```

4. Configure cache TTL in `application.properties`

## Project Structure

```
github-user-proxy/
├── src/
│   ├── main/
│   │   ├── java/miller/kyle/github_user_proxy/
│   │   │   ├── client/
│   │   │   │   └── GitHubApiClient.java
│   │   │   ├── config/
│   │   │   │   └── AppConfig.java
│   │   │   ├── controller/
│   │   │   │   └── UserProxyController.java
│   │   │   ├── dto/
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── GitHubRepoResponse.java
│   │   │   │   ├── GitHubUserResponse.java
│   │   │   │   ├── RepoInfo.java
│   │   │   │   └── UserProxyResponse.java
│   │   │   ├── exception/
│   │   │   │   ├── GitHubApiException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── UserNotFoundException.java
│   │   │   ├── service/
│   │   │   │   └── UserProxyService.java
│   │   │   └── GithubUserProxyApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/miller/kyle/github_user_proxy/
│           ├── client/
│           │   └── GitHubApiClientTest.java
│           ├── controller/
│           │   └── UserProxyControllerTest.java
│           ├── integration/
│           │   └── UserProxyIntegrationTest.java
│           ├── service/
│           │   └── UserProxyServiceTest.java
│           └── GithubUserProxyApplicationTests.java
├── build.gradle
├── settings.gradle
└── README.md
```

## Dependencies

Key dependencies (from `build.gradle`):

- **Spring Boot 3.5.7**: Framework
- **Spring Web**: REST API support
- **Spring Boot Cache**: Caching abstraction (for future use)
- **JUnit 5**: Testing framework
- **Mockito**: Mocking for unit tests
- **Spring Boot Test**: Integration testing support

## Data Flow

1. Client makes HTTP GET request to `/api/users/{username}`
2. `UserProxyController` receives the request
3. `UserProxyService` is called to fetch and transform data
4. `GitHubApiClient` makes two parallel-capable calls to GitHub:
   - `GET https://api.github.com/users/{username}`
   - `GET https://api.github.com/users/{username}/repos`
5. Service transforms the data:
   - Maps field names (login → user_name, name → display_name, etc.)
   - Formats dates (ISO 8601 → RFC 1123)
   - Combines user and repo data into single response
6. Controller returns JSON response to client

## Error Handling Flow

1. `GitHubApiClient` catches HTTP errors from GitHub API
2. Throws custom exceptions (`UserNotFoundException`, `GitHubApiException`)
3. `GlobalExceptionHandler` catches exceptions
4. Returns appropriate HTTP status codes and error messages

## Testing Strategy

### Unit Tests
- **Controller**: Mock the service layer, test HTTP handling
- **Service**: Mock the client layer, test business logic and transformation
- **Client**: Mock RestTemplate, test GitHub API communication

### Integration Tests
- Use `MockRestServiceServer` to mock GitHub API responses
- Test complete request/response flow through all layers
- Verify JSON response format matches requirements

## Future Enhancements

1. **Caching**: Add Redis or in-memory caching to reduce GitHub API calls
2. **Authentication**: Support GitHub Personal Access Tokens for higher rate limits
3. **Pagination**: Handle paginated repository responses for users with many repos
4. **Metrics**: Add Actuator for health checks and metrics
5. **API Documentation**: Add Swagger/OpenAPI documentation
6. **Retry Logic**: Implement exponential backoff for transient failures
7. **Response Filtering**: Allow clients to specify which fields they want
8. **WebClient**: Migrate to reactive WebClient for better performance

## Troubleshooting

### Port already in use
```bash
lsof -i :8080
kill -9 <PID>
```

Or change the port in `application.properties`

### GitHub API Rate Limit
If you hit the rate limit (60 requests/hour), wait an hour or use a GitHub Personal Access Token.

### Tests failing
Ensure you have internet connectivity as integration tests mock GitHub API but unit tests verify the mocking setup.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass: `./gradlew test`
6. Submit a pull request

## License

This project is created as a technical assessment and is provided as-is.

## Contact

For questions or issues, please open an issue in the repository.
