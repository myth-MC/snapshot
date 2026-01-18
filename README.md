<div align="center">
  <p>
    <h1>snapshot</h1>
    <a href="https://github.com/myth-MC/snapshot/releases/latest"><img src="https://img.shields.io/github/v/release/myth-MC/snapshot" alt="Latest release" /></a>
    <a href="https://github.com/myth-MC/snapshot/pulls"><img src="https://img.shields.io/github/issues-pr/myth-MC/snapshot" alt="Pull requests" /></a>
    <a href="https://github.com/myth-MC/snapshot/issues"><img src="https://img.shields.io/github/issues/myth-MC/snapshot" alt="Issues" /></a>
    <a href="https://github.com/myth-MC/snapshot/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-blue.svg" alt="License" /></a>
    <br>
    A Spring Boot application for collecting and viewing Minecraft server debug information.
  </p>
</div>

<details open="open">
  <summary>🧲 Quick navigation</summary>
  <ol>
    <li>
      <a href="#information">📚 Information</a>
    </li>
    <li>
      <a href="#requirements">📋 Requirements</a>
    </li>
    <li>
      <a href="#install">📥 Installation</a>
    </li>
    <li>
      <a href="#configuration">🔨 Configuration</a>
    </li>
    <li>
      <a href="#usage">💡 Usage</a>
    </li>
    <li>
      <a href="#api-endpoints">🔗 API Endpoints</a>
    </li>
    <li>
      <a href="#development">☕️ Development</a>
    </li>
    <li>
      <a href="#references">📕 References</a>
    </li>
  </ol>
</details>

<div id="information"></div>

# 📚 Information

**Snapshot** is a Spring Boot application for collecting and viewing Minecraft server debug information. It provides a REST API for log uploads and a simple web interface for searching and viewing logs.

Currently, **Snapshot** supports the following features:
- **Modern Web UI** for searching and viewing logs with a debug code
- **REST API** for uploading and retrieving server reports
- **Configurable rate limiting** to prevent API abuse per-server
- **Automatic cleanup** - Logs are automatically deleted after 24 hours

<div id="requirements"></div>

# 📋 Requirements
- **Java 17** or higher
- **Maven 3.6+** if you want to compile the project
  
<div id="install"></div>

# 📥 Installation

## Pre-compiled binary file (.jar)
1. Download the latest pre-compiled binary file from the [Releases](https://github.com/myth-MC/snapshot/releases) page
2. Run the application:
   ```bash
   java -jar snapshot-server.jar
   ```
3. You can access the web UI in `localhost:9090` by default

## Self-compiled
1. Clone the repository:
   ```bash
   git clone https://github.com/myth-MC/snapshot.git
   cd snapshot
   ```
2. Build the project:
   ```bash
   mvn clean package
   ```
3. Run the application:
   ```bash
   java -jar target/snapshot-server-0.1.0.jar
   ```
4. You can access the web UI in `localhost:9090` by default

<div id="configuration"></div>

# 🔨 Configuration
Most options can be configured by editing `application.properties`:
- **Server port**: Change `server.port` to your desired port
- **Rate limiting**:
  - `snapshot.rate-limiting.capacity`: Maximum requests per window
  - `snapshot.rate-limiting.refill-amount`: Requests restored per refill
  - `snapshot.rate-limiting.refill-minutes`: Time window for refill in minutes
- **Web UI**:
  - `snapshot.web-ui.timestamp-format`: Timestamp format

>[!TIP]
>You can also pass these settings as system properties in the `java` command:
>```bash
>java -Dserver.port=80 -Dsnapshot.rate-limiting.capacity=10 -jar snapshot-server.jar
>```

<div id="usage"></div>

# 💡 Usage
You can view logs by entering a debug code in the `/search` page:
1. Navigate to `/search` (e.g., `http://localhost:9090/search`)
2. Enter a debug code (e.g., `DBG-ABC123`) to view the associated log
3. The result page displays:
   - General information (UUID, timestamp, requester)
   - Server environment (software, version, online mode)
   - Plugin information (name, version)
   - Additional data such as:
     - YAML files
     - Strings
     - Integers
     - Booleans
     - Lists

<div id="api-endpoints"></div>

# 🔗 API Endpoints
The API exposes two endpoints to retrieve and upload data with JSON format.

## POST `/api/v1/upload`
Uploads a log entry.
### Field Descriptions
| Name                  | Type            | Description                            | Required? |
|-----------------------|-----------------|----------------------------------------|-----------|
| `requester`           | String          | Name of the report requester           | ✅        |
| `pluginName`          | String          | Name of the targeted plugin            | ✅        |
| `pluginVersion`       | String          | Version of the targeted plugin         | ✅        |
| `serverPort`          | Integer         | Port of the server                     | ✅        |
| `serverVersion`       | String          | Version of the server                  | ✅        |
| `serverSoftware`      | String          | Software of the server                 | ✅        |
| `serverOnlineMode`    | Boolean         | Whether the server runs in online mode | ✅        |
| `extra`               | JSON structure  | Additional data                        | ❌        |

>[!NOTE]
>This is how a full valid request body would look like:
>```json
>{
>  "requester": "PlayerName",
>  "pluginName": "MyPlugin",
>  "pluginVersion": "1.0.0",
>  "serverPort": 25565,
>  "serverVersion": "1.20.1",
>  "serverSoftware": "Paper",
>  "serverOnlineMode": true,
>  "extra": {
>    "serverPlugins": ["Plugin1", "Plugin2"],
>    "config.yml": "base64encodedcontent...",
>    "bukkit.yml": "base64encodedcontent..."
>  }
>}
>```

## GET `/api/v1/log/DBG-XXXXXX`
Retrieves a log by its code.

### Field Descriptions
| Name                  | Type            | Description                            |
|-----------------------|-----------------|----------------------------------------|
| `uuid`                | String          | UUID of the created log                |
| `timestamp`           | String          | Creation timestamp of the log          |
| `requester`           | String          | Name of the report requester           |
| `pluginName`          | String          | Name of the targeted plugin            |
| `pluginVersion`       | String          | Version of the targeted plugin         |
| `serverVersion`       | String          | Version of the server                  |
| `serverSoftware`      | String          | Software of the server                 |
| `serverOnlineMode`    | Boolean         | Whether the server runs in online mode |
| `extra`               | JSON structure  | Additional data                        |

>[!NOTE]
>This is how a full response could look like:
>```json
>{
>  "uuid": "abc12345-6789-0123-4567-890123456789",
>  "timestamp": "2026-01-18T03:03:44.047833",
>  "requester": "PlayerName",
>  "pluginName": "MyPlugin",
>  "pluginVersion": "1.0.0",
>  "serverVersion": "1.20.1",
>  "serverSoftware": "Paper",
>  "serverOnlineMode": true,
>  "extra": {
>    "serverPlugins": ["Plugin1", "Plugin2"],
>    "config.yml": "base64encodedcontent...",
>    "bukkit.yml": "base64encodedcontent..."
>  }
>}
>```

## Extra field structure
The `extra` field accepts any valid JSON structure and can be used to display additional data.
### JSON objects
Most JSON objects (strings, integers, booleans, arrays...) in the `extra` field will be handled automatically.
```json
{
  "extra": {
    "exampleString": "This is an example string!",
    "exampleInteger": 25565,
    "exampleBoolean": true
  }
}
```
### Text files (YAML, JSON...)
Text files **must** be properly encoded into Base64.
```json
{
  "extra": {
    "config.yml": "base64encoded...",
    "server.properties": "base64encoded...",
    "settings.json": "base64encoded..."
  }
}
```

<div id="development"></div>

# ☕️ Development
You may extend the functionality of the application easily with some basic Java knowledge.
## Project Setup
### Cloning the repository
```bash
git clone https://github.com/myth-MC/snapshot.git
git checkout dev
```

### Building
```bash
mvn clean package
```

### Running in Development Mode
```bash
mvn spring-boot:run
```

## Project Structure
```
src/
├── main/
│   ├── java/ovh/mythmc/snapshot/server/
│   │   ├── controller/      # REST and web controllers
│   │   ├── entity/          # JPA entities
│   │   ├── repository/      # Data repositories
│   │   ├── converter/       # Custom converters
│   │   └── scheduler/       # Scheduled tasks
│   └── resources/
│       ├── templates/       # Thymeleaf templates
│       ├── static/          # CSS, JS, images
│       └── application.properties
└── pom.xml
```

<div id="references"></div>

# 📕 References
- This application is built with [Spring Boot](https://spring.io/projects/spring-boot)
- [H2](https://www.h2database.com/html/main.html) is used to provide database functionality
- Rate limiting is powered by [Bucket4J](https://github.com/bucket4j/bucket4j)

These are some of the projects we've used as inspiration for the application:

- [bStats](https://github.com/Bastian/bStats) by Bastian
- [spark](https://github.com/lucko/spark) by lucko
