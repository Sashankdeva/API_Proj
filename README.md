# SquadUp — Player Matching Platform (Spring Boot)

SquadUp is a web platform that fixes the social gaps in multiplayer matchmaking. Standard
matchmaking looks at skill and latency; SquadUp lets players build squads ("Looking for Group"
lobbies) around the things that actually make a team work: the game, the language they speak,
their play style. Players can add friends, join or create squads, get squad recommendations
and chat in real time with friends and squad mates.

The project was originally built on the MERN stack (MongoDB, Express, Node.js, Socket.io).
This version replaces the whole backend with **Spring Boot 3.5** and is organised around the
*API & Micro Services* course (241AI024): every unit of the syllabus is used in real
application code (see [Syllabus map](#syllabus-map)).

---

## Quick start

Requirements: **JDK 17 or newer**. Nothing else: Maven is downloaded by the wrapper and the
default database (H2) is embedded.

```bash
./mvnw spring-boot:run          # macOS / Linux / Git Bash
```

```powershell
.\start.ps1                     # Windows PowerShell (frees port 5500 first)
```

Open **http://localhost:5500** and log in with a demo account. On first start the database is
filled with 20 players, 18 squads, friendships and chat history. **The password of every demo
player is the same as the username**, e.g. `sniperking` / `sniperking`, `neon_ninja` / `neon_ninja`.

| What                  | URL                                                    |
|-----------------------|--------------------------------------------------------|
| Web app               | http://localhost:5500                                  |
| REST API              | http://localhost:5500/api/v1/...                       |
| SOAP WSDL (SoapUI)    | http://localhost:5500/soap/players.wsdl                |
| H2 database console   | http://localhost:5500/h2-console (JDBC URL `jdbc:h2:file:./data/squadup`, user `sa`, no password) |

Run the tests (55 tests covering every unit):

```bash
./mvnw test
```

### Using MySQL instead of H2

MySQL 8 is supported through the `mysql` profile ([application-mysql.properties](src/main/resources/application-mysql.properties)).
The database `squadup` is created automatically; credentials come from environment variables.

```powershell
$env:DB_USERNAME="root"; $env:DB_PASSWORD="your-password"
.\start.ps1 mysql
```

---

## Architecture

```
Browser (HTML/CSS/JS in src/main/resources/static)
   │  fetch() + "Authorization: Bearer <token>"          WebSocket (STOMP)
   ▼                                                            ▼
Spring Security filter chain ──► @RestController ──► @Service ──► Spring Data JPA ──► H2 / MySQL
   (TokenAuthenticationFilter)        │                  │            JdbcTemplate (stats)
                                      │                  └── AOP: logging, timing, audit
                                      └── @RestControllerAdvice (errors → JSON)
SOAP client (SoapUI) ──► Spring WS MessageDispatcherServlet ──► @Endpoint ──► UserService
```

```
src/main/java/com/squadup
├── SquadUpApplication.java     @SpringBootApplication entry point
├── config/                     Java config: AppConfig, SecurityConfig, WebConfig, WebSocketConfig,
│                               SoapConfig, RequestTrace (request scope), DataSeeder
├── entity/                     JPA entities: User, Squad, Friendship, Message, AuthToken
├── repository/                 Spring Data repositories + SquadRepositoryCustomImpl (Criteria API)
├── jdbc/                       PlatformStatsDao (JdbcTemplate)
├── service/                    Business logic, transactions (UserService interface + impl/)
├── matching/                   Squad recommendation engine (prototype-scoped SquadMatcher)
├── controller/                 REST controllers (v1 + v2) and the MVC PageController
├── websocket/                  STOMP chat controller, auth interceptor, real-time notifier
├── soap/                       SOAP endpoint + JAXB classes (schema/)
├── aop/                        LoggingAspect, PerformanceAspect, AuditAspect
├── security/                   Token filter, principal, JSON 401/403 handler
├── exception/                  ApiException + GlobalExceptionHandler
├── client/                     SquadUpApiClient (RestTemplate REST client)
└── dto/                        Request/response records with Bean Validation
```

---

## Syllabus map

### Unit I — Spring 5 Basics
| Topic | Where |
|---|---|
| IoC container, Java-based configuration (`@Configuration`, `@Bean`) | [AppConfig](src/main/java/com/squadup/config/AppConfig.java): `PasswordEncoder`, `Clock`, `SecureRandom`, `ChatModerator` |
| Constructor injection | Every service and controller, e.g. [AuthService](src/main/java/com/squadup/service/AuthService.java) |
| Setter injection | `ChatModerator` configured with setters in [AppConfig](src/main/java/com/squadup/config/AppConfig.java); `@Autowired` setter in [ChatService](src/main/java/com/squadup/service/ChatService.java) |
| Auto scanning | `@SpringBootApplication` → `@ComponentScan` of `com.squadup.*` ([SquadUpApplication](src/main/java/com/squadup/SquadUpApplication.java)) |
| Spring JDBC | [PlatformStatsDao](src/main/java/com/squadup/jdbc/PlatformStatsDao.java) with `JdbcTemplate` + `RowMapper` → `GET /api/v1/stats` |
| Spring MVC | `@Controller` [PageController](src/main/java/com/squadup/controller/PageController.java) (view redirect) and the REST controllers |

### Unit II — Spring Boot
| Topic | Where |
|---|---|
| `@SpringBootApplication`, auto-configuration | [SquadUpApplication](src/main/java/com/squadup/SquadUpApplication.java), [application.properties](src/main/resources/application.properties) |
| Auto-wiring (by type, interface → implementation, collections, `@Order`) | `UserService` → `UserServiceImpl`; `List<MatchScorer>` in [SquadMatcher](src/main/java/com/squadup/matching/SquadMatcher.java) |
| Bean scopes | singleton (default), **prototype** [SquadMatcher](src/main/java/com/squadup/matching/SquadMatcher.java) via `ObjectProvider`, **request** [RequestTrace](src/main/java/com/squadup/config/RequestTrace.java) |
| Logger (SLF4J + Logback) | [logback-spring.xml](src/main/resources/logback-spring.xml): console, rolling `logs/squadup.log`, separate `logs/audit.log` |
| AOP: `@Before`, `@AfterReturning`, `@AfterThrowing` | [LoggingAspect](src/main/java/com/squadup/aop/LoggingAspect.java) — around every service method |
| AOP: `@Around` + custom annotation | [PerformanceAspect](src/main/java/com/squadup/aop/PerformanceAspect.java) + [`@TrackExecutionTime`](src/main/java/com/squadup/aop/TrackExecutionTime.java) |
| AOP: `@After`, audit trail | [AuditAspect](src/main/java/com/squadup/aop/AuditAspect.java) (logins, password changes, account deletion) |
| Tests for auto-wiring, logging and AOP | [AutowiringAndLoggingTest](src/test/java/com/squadup/AutowiringAndLoggingTest.java), [AopAdviceTest](src/test/java/com/squadup/aop/AopAdviceTest.java) |

### Unit III — Spring Data JPA with Boot
| Topic | Where |
|---|---|
| Entities and relationships (`@ManyToOne`, `@ManyToMany`, `@ElementCollection`) | [entity/](src/main/java/com/squadup/entity) |
| Repository interfaces and CRUD | [repository/](src/main/java/com/squadup/repository) |
| Pagination and sorting (`Pageable`, `Sort`, `Page`) | `GET /api/v1/users?page=0&size=10&sort=username,asc`, `GET /api/v1/squads?...&sort=name,desc` |
| Query approaches: derived queries | `findByUsernameContainingIgnoreCase`, `findByMembers_IdOrderByCreatedAtDesc` |
| Named queries | `@NamedQuery("User.findByLogin")` on [User](src/main/java/com/squadup/entity/User.java) → `UserRepository.findByLogin` |
| `@Query` (JPQL and native), custom sorting | `findMostPopular` (order by member count), `findPlayersOfGame(game, Sort)`, native `removeMemberFromAllSquads` |
| Declarative transactions (`@Transactional`) | Services: `readOnly` reads, [UserServiceImpl.deleteAccount](src/main/java/com/squadup/service/impl/UserServiceImpl.java) (many steps, one transaction), pessimistic lock in `SquadService.join` |
| Update operation | `@Modifying` [FriendshipRepository.updateStatus](src/main/java/com/squadup/repository/FriendshipRepository.java) |
| Custom repository implementation | [SquadRepositoryCustomImpl](src/main/java/com/squadup/repository/SquadRepositoryCustomImpl.java) (Criteria API, dynamic filters) |
| Tests | [RepositoryTest](src/test/java/com/squadup/repository/RepositoryTest.java) (`@DataJpaTest`) |

### Unit IV — Web Services
| Topic | Where |
|---|---|
| RESTful web services | All `/api/**` controllers |
| SOAP web service: service interface + `@Service` | [UserService](src/main/java/com/squadup/service/UserService.java) / [UserServiceImpl](src/main/java/com/squadup/service/impl/UserServiceImpl.java) |
| SOAP: XSD contract, generated WSDL | [players.xsd](src/main/resources/xsd/players.xsd), [SoapConfig](src/main/java/com/squadup/config/SoapConfig.java) → `/soap/players.wsdl` |
| SOAP endpoint, SOAP faults | [PlayerSoapEndpoint](src/main/java/com/squadup/soap/PlayerSoapEndpoint.java), `@SoapFault` exceptions |
| Tests | [PlayerSoapEndpointTest](src/test/java/com/squadup/soap/PlayerSoapEndpointTest.java) |

### Unit V — Spring REST
| Topic | Where |
|---|---|
| REST controllers, CRUD | [SquadController](src/main/java/com/squadup/controller/SquadController.java), [UserController](src/main/java/com/squadup/controller/UserController.java), [FriendController](src/main/java/com/squadup/controller/FriendController.java) |
| `@RequestBody` and `ResponseEntity` (201 + `Location`, cache headers) | [AuthController.register](src/main/java/com/squadup/controller/AuthController.java), `SquadController.create`, [StatsController](src/main/java/com/squadup/controller/StatsController.java) |
| `@PathVariable`, `@RequestParam` | throughout the controllers |
| `@MatrixVariable` | `GET /api/v1/squads/browse/filter;game=Valorant;language=English;openOnly=true` |
| Exception handling | [GlobalExceptionHandler](src/main/java/com/squadup/exception/GlobalExceptionHandler.java) (`@RestControllerAdvice`), [JsonSecurityErrorHandler](src/main/java/com/squadup/security/JsonSecurityErrorHandler.java) |
| Data validation | Bean Validation on DTOs, e.g. [AuthDtos](src/main/java/com/squadup/dto/AuthDtos.java), `@Min/@Max` on request params |
| Creating a REST client | [SquadUpApiClient](src/main/java/com/squadup/client/SquadUpApiClient.java) (`RestTemplate`), used in [SquadUpApiClientTest](src/test/java/com/squadup/client/SquadUpApiClientTest.java) |
| Versioning | URI `/api/v2/squads` and header `X-API-Version: 2` on `/api/squads` ([SquadV2Controller](src/main/java/com/squadup/controller/SquadV2Controller.java)) |
| Enabling CORS | Global rules in [WebConfig](src/main/java/com/squadup/config/WebConfig.java), `@CrossOrigin` on [StatsController](src/main/java/com/squadup/controller/StatsController.java) |
| Securing REST endpoints | [SecurityConfig](src/main/java/com/squadup/config/SecurityConfig.java) + [TokenAuthenticationFilter](src/main/java/com/squadup/security/TokenAuthenticationFilter.java), ownership checks in [AccessGuard](src/main/java/com/squadup/security/AccessGuard.java) |
| Tests (MockMvc) | [AuthAndUserApiTest](src/test/java/com/squadup/controller/AuthAndUserApiTest.java), [SquadFriendChatApiTest](src/test/java/com/squadup/controller/SquadFriendChatApiTest.java) |

---

## REST API

All endpoints except register/login need `Authorization: Bearer <token>` (returned by login).
Errors always look like `{"error": "...", "status": 400, "path": "...", "requestId": "...", "fieldErrors": {...}}`.

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Create an account → 201 |
| POST | `/api/v1/auth/login` | `{username (or e-mail), password}` → `{token, user}` |
| POST | `/api/v1/auth/logout` | Revoke the current token |
| GET | `/api/v1/users?page&size&sort` | Player directory (paged) |
| GET | `/api/v1/users/search?username=&page&size` | Search players (paged) |
| GET | `/api/v1/users/players?game=&sortBy=&dir=` | Players of a game |
| GET / PUT / DELETE | `/api/v1/users/{id}` | Read profile / update own profile / delete own account |
| PUT | `/api/v1/users/{id}/password` | Change own password |
| GET | `/api/v1/friends/{userId}` | Accepted friends (own list only) |
| GET | `/api/v1/friends/pending/{userId}` | Incoming requests |
| POST | `/api/v1/friends/request` | `{recipientUsername}` |
| POST | `/api/v1/friends/respond` | `{friendshipId, action: accept\|reject}` |
| DELETE | `/api/v1/friends/{friendshipId}` | Unfriend |
| GET | `/api/v1/squads?q&game&language&openOnly&page&size&sort` | Search squads (custom repository) |
| GET | `/api/v1/squads/browse/filter;game=..;language=..;openOnly=..` | Same with matrix variables |
| GET | `/api/v1/squads/popular` | Ordered by member count |
| GET | `/api/v1/squads/recommended?limit=5` | Matches for the logged-in player, with reasons |
| GET | `/api/v1/squads/{id}` | One squad |
| GET | `/api/v1/squads/user/{userId}` | Squads I am in |
| GET | `/api/v1/squads/public/{userId}` | Squads I can join |
| POST | `/api/v1/squads` | Create → 201 + `Location` |
| POST | `/api/v1/squads/{id}/join` | Join (409 if full / already in) |
| DELETE | `/api/v1/squads/{id}` | Owner deletes, member leaves |
| GET | `/api/v2/squads`, `/api/v2/squads/{id}` | v2 compact representation |
| GET | `/api/squads` + header `X-API-Version: 2` | v2 via header |
| GET | `/api/v1/chat/history/{roomId}?limit=50` | Chat history (room members only) |
| GET | `/api/v1/stats?topGames=5` | Platform statistics (JdbcTemplate) |

### Try it with curl

```bash
# log in and keep the token
TOKEN=$(curl -s -X POST http://localhost:5500/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"sniperking","password":"sniperking"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')

curl -H "Authorization: Bearer $TOKEN" "http://localhost:5500/api/v1/squads?game=Valorant&sort=name,asc&size=5"
curl -H "Authorization: Bearer $TOKEN" "http://localhost:5500/api/v1/squads/browse/filter;game=Valorant;openOnly=true"
curl -H "Authorization: Bearer $TOKEN" -H "X-API-Version: 2" "http://localhost:5500/api/squads"
curl -H "Authorization: Bearer $TOKEN" "http://localhost:5500/api/v1/squads/recommended"
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Night Owls","game":"Valorant","language":"English","maxMembers":5}' \
  http://localhost:5500/api/v1/squads
```

In Postman, set the `Authorization` tab to *Bearer Token* and paste the token from the login response.

### SOAP with SoapUI or curl

In SoapUI choose *New SOAP Project* and use `http://localhost:5500/soap/players.wsdl`. With curl:

```bash
curl -s -X POST http://localhost:5500/soap -H "Content-Type: text/xml" -d '
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:p="http://squadup.com/soap/players">
  <soapenv:Body>
    <p:getPlayerRequest><p:username>sniperking</p:username></p:getPlayerRequest>
  </soapenv:Body>
</soapenv:Envelope>'
```

The second operation is `searchPlayersRequest` with `<p:query>` and an optional `<p:maxResults>`.

---

## Real-time chat

Socket.io was replaced by Spring's WebSocket message broker with the STOMP protocol:

| Destination | Direction | Purpose |
|---|---|---|
| `/ws` | handshake | WebSocket endpoint; STOMP `CONNECT` must carry `Authorization: Bearer <token>` |
| `/app/chat.send` | client → server | `{roomId, text}`: moderated, stored, broadcast |
| `/app/chat.typing` | client → server | `{roomId, typing}` typing indicator |
| `/topic/room.{roomId}` | server → clients | Messages and typing events of a room (members only) |
| `/user/queue/friends` | server → one user | "friend-update" when requests/friends change |

Room ids are the same as before: `dm_<idA>_<idB>` and `squad_<squadId>`. In the browser,
[realtime.js](src/main/resources/static/realtime.js) wraps the STOMP client behind the old
`socket.on(...)` / `socket.emit(...)` calls, so the pages changed very little.

---

## What changed from the MERN version

| MERN | Spring Boot |
|---|---|
| Express routes (`backend/routes/*.js`) | `@RestController` classes under `/api/v1` |
| Mongoose models | JPA entities + Spring Data repositories |
| MongoDB | H2 (default) or MySQL |
| Socket.io | WebSocket + STOMP (`realtime.js` on the client) |
| `seed.js` | [DataSeeder](src/main/java/com/squadup/config/DataSeeder.java) (runs automatically on an empty database) |
| User id sent in the request body | Identity taken from the bearer token; players can only change their own data |
| `_id` fields | `id` fields (numbers) |

---

## Troubleshooting

* **Port 5500 already in use**: run `.\start.ps1`, which stops the old process first, or start with `--server.port=8080`.
* **`Unable to establish loopback connection`** when you start the app with `java -jar` or from an IDE:
  on some Windows machines Java cannot create its internal socket file in `%TEMP%`. The Maven
  run and test commands already work around this; for other launchers add
  `-Djdk.net.unixdomain.tmpdir=C:\some\writable\folder` to the JVM options.
* **Reset the demo data**: stop the app and delete the `data/` folder. The next start seeds a fresh database.
