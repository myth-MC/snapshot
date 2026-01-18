package ovh.mythmc.snapshot.server.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ovh.mythmc.snapshot.server.entity.ServerLog;

/**
 * Repository interface for ServerLog entity operations.
 * Provides data access methods for server log entries.
 * 
 * @author U8092
 */
public interface ServerLogRepository extends JpaRepository<ServerLog, UUID> {

    /**
     * Deletes all server logs with timestamps before the specified cutoff time.
     * 
     * @param cutoff the cutoff timestamp
     */
    void deleteAllByTimestampBefore(LocalDateTime cutoff);

    /**
     * Finds a server log by UUID prefix (first 6 characters).
     * This is used to find logs by their debug code (DBG-XXXXXX).
     * Uses native SQL for better UUID string handling.
     * H2 stores UUIDs without dashes when converted to string, so we remove dashes for comparison.
     * 
     * @param uuidPrefix the first 6 characters of the UUID (case-insensitive)
     * @return Optional containing the ServerLog if found
     */
    @Query(value = "SELECT * FROM server_logs WHERE LOWER(SUBSTRING(REPLACE(CAST(uuid AS VARCHAR), '-', ''), 1, 6)) = LOWER(:uuidPrefix)", nativeQuery = true)
    Optional<ServerLog> findByUuidPrefix(@Param("uuidPrefix") String uuidPrefix);

    /**
     * Finds all server logs with timestamps before the specified cutoff time.
     * 
     * @param cutoff the cutoff timestamp
     * @return list of server logs to be deleted
     */
    List<ServerLog> findAllByTimestampBefore(LocalDateTime cutoff);
}
