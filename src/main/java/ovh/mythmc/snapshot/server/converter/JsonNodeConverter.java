package ovh.mythmc.snapshot.server.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.JsonNodeException;

/**
 * JPA AttributeConverter for converting JsonNode to/from database String columns.
 * Handles serialization and deserialization of JSON data stored in the database.
 * 
 * @author U8092
 */
@Converter(autoApply = false)
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

    private static final Logger logger = LoggerFactory.getLogger(JsonNodeConverter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Converts a JsonNode entity attribute to a database column value.
     * 
     * @param attribute the JsonNode to convert
     * @return JSON string representation, or null if attribute is null
     * @throws IllegalStateException if serialization fails
     */
    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        if (attribute == null) {
            return null;
        }
        
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonNodeException e) {
            logger.error("Failed to serialize JsonNode to database column", e);
            throw new IllegalStateException("Failed to serialize JsonNode to JSON string", e);
        }
    }

    /**
     * Converts a database column value to a JsonNode entity attribute.
     * 
     * @param dbData the JSON string from the database
     * @return JsonNode representation, or null if dbData is null or empty
     * @throws IllegalStateException if deserialization fails
     */
    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        
        try {
            return OBJECT_MAPPER.readTree(dbData);
        } catch (Exception e) {
            logger.error("Failed to deserialize JSON string from database column", e);
            throw new IllegalStateException("Failed to deserialize JSON string to JsonNode", e);
        }
    }
}
