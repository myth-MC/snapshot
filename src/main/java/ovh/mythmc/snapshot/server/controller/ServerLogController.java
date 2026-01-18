package ovh.mythmc.snapshot.server.controller;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ovh.mythmc.snapshot.server.entity.ServerLog;
import ovh.mythmc.snapshot.server.repository.ServerLogRepository;

/**
 * REST controller for handling server log uploads.
 * Implements rate limiting using Bucket4J to prevent abuse.
 * 
 * @author U8092
 */
@RestController
@RequestMapping("/api/v1")
@Validated
public class ServerLogController {

    private static final Logger logger = LoggerFactory.getLogger(ServerLogController.class);
    
    private static final String DEBUG_CODE_PREFIX = "DBG-";
    private static final int UUID_PREFIX_LENGTH = 6;
    
    // HTTP header for forwarded IP addresses
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String UNKNOWN_IP = "unknown";

    // Rate limiting configuration
    @Value("${snapshot.rate-limiting.capacity}")
    private int RATE_LIMIT_CAPACITY;

    @Value("${snapshot.rate-limiting.refill-amount}")
    private int RATE_LIMIT_REFILL_AMOUNT;

    @Value("${snapshot.rate-limiting.refill-minutes}")
    private int RATE_LIMIT_REFILL_MINUTES;

    private final ServerLogRepository repository;
    private final Map<String, Bucket> rateLimitBuckets = new ConcurrentHashMap<>();

    /**
     * Constructs a new ServerLogController with the given repository.
     * 
     * @param repository the server log repository
     */
    public ServerLogController(ServerLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Uploads a server log entry.
     * Implements rate limiting per server (IP:Port combination).
     * 
     * @param log the server log to upload
     * @param request the HTTP request
     * @return response containing status and debug code if successful, or rate limit error
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadLog(
            @Valid @RequestBody ServerLog log,
            HttpServletRequest request) {
        
        try {
            String serverId = buildServerId(request, log.getServerPort());
            Bucket bucket = resolveRateLimitBucket(serverId);

            if (!bucket.tryConsume(1)) {
                logger.warn("Rate limit exceeded for server: {}", serverId);
                return ResponseEntity
                        .status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(createErrorResponse("rate_limited", "Too many requests. Please try again later."));
            }

            ServerLog savedLog = repository.save(log);
            String debugCode = generateDebugCode(savedLog.getUuid());

            logger.info("Server log uploaded successfully. Debug code: {}, Server: {}", debugCode, serverId);

            return ResponseEntity.ok(createSuccessResponse(debugCode));
            
        } catch (Exception e) {
            logger.error("Error uploading server log", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("error", "An error occurred while processing your request."));
        }
    }

    /**
     * Retrieves a server log entry by debug code or UUID prefix.
     * 
     * @param code the debug code (e.g., "DBG-ABC123") or UUID prefix (e.g., "ABC123")
     * @return the server log as JSON, or 404 if not found
     */
    @GetMapping("/log/{code}")
    public ResponseEntity<?> getLog(@PathVariable("code") String code) {
        if (code == null || code.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("error", "Debug code cannot be empty"));
        }

        try {
            // Normalize the code (remove prefix and whitespace)
            String normalizedCode = code.trim().toUpperCase();
            String uuidPrefix = extractUuidPrefix(normalizedCode);

            if (uuidPrefix == null || uuidPrefix.length() != 6) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("error", "Invalid debug code format"));
            }

            ServerLog log = repository.findByUuidPrefix(uuidPrefix)
                    .orElse(null);

            if (log == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("not_found", "Log not found for code: " + normalizedCode));
            }

            logger.info("Retrieved server log for code: {}", normalizedCode);
            
            // Build response map excluding 'extra' and 'extraTree'
            Map<String, Object> response = new HashMap<>();
            response.put("uuid", log.getUuid());
            response.put("requester", log.getRequester());
            response.put("pluginName", log.getPluginName());
            response.put("pluginVersion", log.getPluginVersion());
            response.put("serverVersion", log.getServerVersion());
            response.put("serverSoftware", log.getServerSoftware());
            response.put("serverOnlineMode", log.getServerOnlineMode());
            response.put("timestamp", log.getTimestamp());
            response.put("extra", log.getExtra().asString());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving server log with code: {}", code, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("error", "An error occurred while retrieving the log."));
        }
    }

    /**
     * Retrieves a server log entry by query parameter (alternative endpoint).
     * 
     * @param code the debug code (e.g., "DBG-ABC123") or UUID prefix (e.g., "ABC123")
     * @return the server log as JSON, or 404 if not found
     */
    @GetMapping("/log")
    public ResponseEntity<?> getLogByQuery(@RequestParam(value = "code", required = false) String code) {
        if (code == null || code.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("error", "Debug code parameter is required"));
        }
        return getLog(code);
    }

    /**
     * Resolves or creates a rate limit bucket for the given server ID.
     * 
     * @param serverId the unique server identifier (IP:Port)
     * @return the rate limit bucket for this server
     */
    private Bucket resolveRateLimitBucket(String serverId) {
        return rateLimitBuckets.computeIfAbsent(serverId, id -> 
            Bucket.builder()
                .addLimit(limit -> limit
                    .capacity(RATE_LIMIT_CAPACITY)
                    .refillGreedy(RATE_LIMIT_REFILL_AMOUNT, Duration.ofMinutes(RATE_LIMIT_REFILL_MINUTES)))
                .build()
        );
    }

    /**
     * Builds a unique server identifier from the request and server port.
     * 
     * @param request the HTTP request
     * @param serverPort the server port
     * @return the server identifier (IP:Port)
     */
    private String buildServerId(HttpServletRequest request, Integer serverPort) {
        String clientIp = extractClientIp(request);
        return clientIp + ":" + serverPort;
    }

    /**
     * Extracts the client IP address from the request, considering proxy headers.
     * 
     * @param request the HTTP request
     * @return the client IP address
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
        
        if (forwardedFor != null && !forwardedFor.isBlank() && !UNKNOWN_IP.equalsIgnoreCase(forwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            String firstIp = forwardedFor.split(",")[0].trim();
            if (!firstIp.isBlank()) {
                return firstIp;
            }
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Generates a debug code from a UUID.
     * 
     * @param uuid the UUID to generate the code from
     * @return the debug code (e.g., "DBG-ABC123")
     */
    private String generateDebugCode(java.util.UUID uuid) {
        String uuidString = uuid.toString().replace("-", "");
        String prefix = uuidString.substring(0, UUID_PREFIX_LENGTH).toUpperCase();
        return DEBUG_CODE_PREFIX + prefix;
    }

    /**
     * Extracts the UUID prefix from a debug code.
     * 
     * @param code the debug code (e.g., "DBG-ABC123")
     * @return the UUID prefix (e.g., "ABC123") or null if invalid
     */
    private String extractUuidPrefix(String code) {
        if (code.startsWith(DEBUG_CODE_PREFIX)) {
            return code.substring(DEBUG_CODE_PREFIX.length());
        }
        return code;
    }

    /**
     * Creates a success response map.
     * 
     * @param debugCode the generated debug code
     * @return the response map
     */
    private Map<String, Object> createSuccessResponse(String debugCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("logId", debugCode);
        return response;
    }

    /**
     * Creates an error response map.
     * 
     * @param status the error status
     * @param message the error message
     * @return the response map
     */
    private Map<String, Object> createErrorResponse(String status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("message", message);
        return response;
    }
}
