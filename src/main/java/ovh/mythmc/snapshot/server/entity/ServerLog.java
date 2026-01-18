package ovh.mythmc.snapshot.server.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ovh.mythmc.snapshot.server.converter.JsonNodeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Entity representing a server log entry submitted by a Minecraft server plugin.
 * Contains server information, plugin details, and optional extra data in JSON format.
 * 
 * @author U8092
 */
@Entity
@Table(name = "server_logs", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_plugin", columnList = "pluginName"),
    @Index(name = "idx_uuid_prefix", columnList = "uuid")
})
public class ServerLog {

    private static final Logger logger = LoggerFactory.getLogger(ServerLog.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue
    private UUID uuid;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Requester cannot be blank")
    private String requester;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Plugin name cannot be blank")
    private String pluginName;

    @Column(nullable = false, length = 50)
    @NotBlank(message = "Plugin version cannot be blank")
    private String pluginVersion;

    @Column(nullable = false)
    @NotNull(message = "Server port cannot be null")
    @Positive(message = "Server port must be positive")
    private Integer serverPort;

    @Column(nullable = false, length = 50)
    @NotBlank(message = "Server version cannot be blank")
    private String serverVersion;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Server software cannot be blank")
    private String serverSoftware;

    @Column(nullable = false)
    @NotNull(message = "Server online mode cannot be null")
    private Boolean serverOnlineMode;

    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "json")
    private JsonNode extra;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // Getters and Setters

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public String getServerSoftware() {
        return serverSoftware;
    }

    public void setServerSoftware(String serverSoftware) {
        this.serverSoftware = serverSoftware;
    }

    public Boolean getServerOnlineMode() {
        return serverOnlineMode;
    }

    public void setServerOnlineMode(Boolean serverOnlineMode) {
        this.serverOnlineMode = serverOnlineMode;
    }

    public JsonNode getExtra() {
        return extra;
    }

    public void setExtra(JsonNode extra) {
        this.extra = extra;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // Utility methods for accessing extra JSON fields

    /**
     * Retrieves the extra JSON tree as a JsonNode.
     * The extra field can be any valid JSON: object, array, string, number, boolean, or null.
     * 
     * @return Optional containing the JsonNode if found, empty otherwise
     */
    public Optional<JsonNode> getExtraTree() {
        return Optional.ofNullable(OBJECT_MAPPER.readTree(extra.asString()));
    }

    /**
     * Retrieves a field from the extra JSON node by key.
     * Only works if the extra field is a JSON object.
     * 
     * @param key the key to look up in the extra JSON
     * @return Optional containing the JsonNode if found, empty otherwise
     */
    public Optional<JsonNode> getExtraField(String key) {
        if (extra == null || key == null || key.isBlank()) {
            return Optional.empty();
        }

        try {
            // Check if extra is an object and has the key
            if (extra.isObject() && extra.has(key)) {
                return Optional.ofNullable(extra.get(key));
            }
        } catch (Exception e) {
            logger.warn("Failed to access extra field '{}' from log {}", key, uuid, e);
        }
        return Optional.empty();
    }

    /**
     * Retrieves a text value from the extra JSON node by key.
     * Only works if the extra field is a JSON object.
     * 
     * @param key the key to look up in the extra JSON
     * @return Optional containing the string value if found and is a string, empty otherwise
     */
    public Optional<String> getExtraAsText(String key) {
        return getExtraField(key)
                .filter(JsonNode::isString)
                .map(JsonNode::asString);
    }

    /**
     * Retrieves a list of strings from the extra JSON node by key.
     * Only works if the extra field is a JSON object.
     * 
     * @param key the key to look up in the extra JSON
     * @return Optional containing the list of strings if found and is an array, empty otherwise
     */
    public Optional<List<String>> getExtraAsStringList(String key) {
        return getExtraField(key)
                .filter(JsonNode::isArray)
                .map(node -> {
                    List<String> list = new ArrayList<>();
                    node.forEach(n -> {
                        if (n.isString()) {
                            list.add(n.asString());
                        }
                    });
                    return list;
                });
    }

    // Equals, hashCode, and toString

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServerLog serverLog = (ServerLog) o;
        return Objects.equals(uuid, serverLog.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public String toString() {
        return "ServerLog{" +
                "uuid=" + uuid +
                ", requester='" + requester + '\'' +
                ", pluginName='" + pluginName + '\'' +
                ", pluginVersion='" + pluginVersion + '\'' +
                ", serverPort=" + serverPort +
                ", timestamp=" + timestamp +
                '}';
    }
}
