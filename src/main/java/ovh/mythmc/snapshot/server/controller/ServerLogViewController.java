package ovh.mythmc.snapshot.server.controller;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ovh.mythmc.snapshot.server.entity.ServerLog;
import ovh.mythmc.snapshot.server.repository.ServerLogRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;

/**
 * Controller for handling web view requests for server log search and display.
 * 
 * @author U8092
 */
@Controller
public final class ServerLogViewController {

    private static final Logger logger = LoggerFactory.getLogger(ServerLogViewController.class);
    
    private static final String DEBUG_CODE_PREFIX = "DBG-";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ServerLogRepository repository;

    @Value("snapshot.web-ui.timestamp-format")
    private String timestampFormat;

    /**
     * Constructs a new ServerLogViewController with the given repository.
     * 
     * @param repository the server log repository
     */
    public ServerLogViewController(ServerLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Displays the search form page.
     * 
     * @return the search template name
     */
    @GetMapping("/search")
    public String showSearchForm() {
        return "search";
    }

    /**
     * Handles the search form submission and displays results.
     * 
     * @param code the debug code to search for (e.g., "DBG-ABC123")
     * @param model the model to add attributes to
     * @return the result template name
     */
    @PostMapping("/search")
    public String handleSearch(@RequestParam("code") String code, Model model) {
        if (code == null || code.isBlank()) {
            logger.warn("Empty or null debug code provided");
            model.addAttribute("log", null);
            model.addAttribute("code", code);
            return "result";
        }

        // Normalize the code (remove prefix and whitespace)
        String normalizedCode = code.trim().toUpperCase();
        String uuidPrefix = extractUuidPrefix(normalizedCode);

        if (uuidPrefix == null || uuidPrefix.length() != 6) {
            logger.warn("Invalid debug code format: {}", code);
            model.addAttribute("log", null);
            model.addAttribute("code", code);
            return "result";
        }

        try {
            ServerLog log = repository.findByUuidPrefix(uuidPrefix)
                    .orElse(null);

            model.addAttribute("log", log);
            model.addAttribute("code", normalizedCode);

            if (log != null) {
                addLogAttributesToModel(log, model);
                logger.info("Found server log for code: {}", normalizedCode);
            } else {
                logger.info("No server log found for code: {}", normalizedCode);
            }

        } catch (Exception e) {
            logger.error("Error searching for log with code: {}", code, e);
            model.addAttribute("log", null);
            model.addAttribute("code", code);
        }

        return "result";
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
     * Adds log-specific attributes to the model for display.
     * 
     * @param log the server log
     * @param model the model to add attributes to
     */
    private void addLogAttributesToModel(ServerLog log, Model model) {
        // Format timestamp
        if (log.getTimestamp() != null) {
            model.addAttribute("formattedTimestamp", 
                    log.getTimestamp().format(DateTimeFormatter.ofPattern(timestampFormat)));
        }

        // Process extra field - handle any structure
        log.getExtraTree().ifPresent(extra -> {
            processExtraData(extra, model);
        });
    }

    /**
     * Processes the extra JSON data and adds it to the model.
     * Extra is always expected to be a JSON object with children.
     * 
     * @param extra the extra JsonNode (expected to be an object)
     * @param model the model to add attributes to
     */
    private void processExtraData(JsonNode extra, Model model) {
        if (extra == null || !extra.isObject()) {
            return;
        }

        Map<String, ExtraFieldData> extraFields = new LinkedHashMap<>();
        
        try {
            // Convert JsonNode to Map to iterate over fields
            Map<String, JsonNode> fieldsMap = OBJECT_MAPPER.convertValue(extra, 
                new TypeReference<Map<String, JsonNode>>() {});
            
            fieldsMap.forEach((key, value) -> {
                ExtraFieldData fieldData = processExtraFieldValue(key, value);
                extraFields.put(key, fieldData);
            });
        } catch (Exception e) {
            logger.warn("Failed to process extra fields as object", e);
        }
        
        if (!extraFields.isEmpty()) {
            model.addAttribute("extraFields", extraFields);
        }
    }

    /**
     * Processes a single extra field value and returns structured data.
     * 
     * @param key the field key (null for array items)
     * @param value the JsonNode value
     * @return ExtraFieldData containing the processed information
     */
    private ExtraFieldData processExtraFieldValue(String key, JsonNode value) {
        ExtraFieldData data = new ExtraFieldData();
        data.setKey(key);
        
        if (value == null || value.isNull()) {
            data.setType("null");
            data.setValue("null");
        } else if (value.isString()) {
            data.setType("string");
            String stringValue = value.asString();
            data.setValue(stringValue);
            
            // Try to decode as Base64 if it looks like encoded data
            try {
                String decoded = decodeBase64(stringValue);
                data.setDecodedValue(decoded);
            } catch (IllegalArgumentException e) {
                // Not Base64, that's fine
            }
        } else if (value.isArray()) {
            data.setType("array");
            List<String> arrayItems = new ArrayList<>();
            value.forEach(item -> {
                if (item.isString()) {
                    arrayItems.add(item.asString());
                } else {
                    arrayItems.add(item.toString());
                }
            });
            data.setArrayValue(arrayItems);
        } else if (value.isObject()) {
            data.setType("object");
            data.setValue(value.toString());
            // Recursively process nested objects
            Map<String, ExtraFieldData> nestedFields = new LinkedHashMap<>();
            try {
                // Convert JsonNode to Map to iterate over fields
                Map<String, JsonNode> fieldsMap = OBJECT_MAPPER.convertValue(value, 
                    new TypeReference<Map<String, JsonNode>>() {});
                
                fieldsMap.forEach((nestedKey, nodeValue) -> {
                    nestedFields.put(nestedKey, processExtraFieldValue(nestedKey, nodeValue));
                });
            } catch (Exception e) {
                logger.warn("Failed to process nested object fields", e);
            }
            data.setNestedFields(nestedFields);
        } else if (value.isNumber()) {
            data.setType("number");
            data.setValue(value.asString());
        } else if (value.isBoolean()) {
            data.setType("boolean");
            data.setValue(String.valueOf(value.asBoolean()));
        } else {
            data.setType("unknown");
            data.setValue(value.toString());
        }
        
        return data;
    }

    /**
     * Converts a field key to a human-readable label.
     * Examples: "serverPlugins" -> "Server plugins", "pluginSettingsYaml" -> "Plugin settings yaml"
     * 
     * @param key the field key
     * @return human-readable label
     */
    private static String toHumanReadableLabel(String key) {
        if (key == null || key.isBlank()) {
            return key;
        }
        
        // Handle common known keys
        if ("serverPlugins".equals(key)) {
            return "Server plugins";
        }
        if ("pluginSettingsYaml".equals(key)) {
            return "Plugin settings";
        }
        
        // Convert camelCase to Title Case with spaces
        StringBuilder result = new StringBuilder();
        boolean firstChar = true;
        
        for (char c : key.toCharArray()) {
            if (Character.isUpperCase(c) && !firstChar) {
                result.append(' ');
                result.append(Character.toLowerCase(c));
            } else if (firstChar) {
                result.append(Character.toUpperCase(c));
                firstChar = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }

    /**
     * Data class for representing extra field information.
     */
    public static class ExtraFieldData {
        private String key;
        private String type;
        private String value;
        private String decodedValue;
        private List<String> arrayValue;
        private Map<String, ExtraFieldData> nestedFields;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        /**
         * Gets the human-readable label for this field.
         * 
         * @return human-readable label
         */
        public String getLabel() {
            return toHumanReadableLabel(key);
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDecodedValue() {
            return decodedValue;
        }

        public void setDecodedValue(String decodedValue) {
            this.decodedValue = decodedValue;
        }

        public List<String> getArrayValue() {
            return arrayValue;
        }

        public void setArrayValue(List<String> arrayValue) {
            this.arrayValue = arrayValue;
        }

        public Map<String, ExtraFieldData> getNestedFields() {
            return nestedFields;
        }

        public void setNestedFields(Map<String, ExtraFieldData> nestedFields) {
            this.nestedFields = nestedFields;
        }
    }

    /**
     * Decodes a Base64 encoded string.
     * 
     * @param encoded the Base64 encoded string
     * @return the decoded string
     * @throws IllegalArgumentException if the string is not valid Base64
     */
    private static String decodeBase64(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalArgumentException("Encoded string cannot be null or blank");
        }
        
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encoded);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 encoding", e);
        }
    }
}
