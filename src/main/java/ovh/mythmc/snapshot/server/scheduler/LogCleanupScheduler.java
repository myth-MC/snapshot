package ovh.mythmc.snapshot.server.scheduler;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ovh.mythmc.snapshot.server.repository.ServerLogRepository;

/**
 * Scheduled task component for cleaning up old server logs.
 * Automatically deletes logs older than 24 hours to maintain data privacy.
 * 
 * @author U8092
 */
@Component
public final class LogCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LogCleanupScheduler.class);
    
    // Cleanup configuration
    private static final int LOG_RETENTION_HOURS = 24;
    private static final long CLEANUP_INTERVAL_MS = 60 * 60 * 1000; // 1 hour

    private final ServerLogRepository repository;

    /**
     * Constructs a new LogCleanupScheduler with the given repository.
     * 
     * @param repository the server log repository
     */
    public LogCleanupScheduler(ServerLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Scheduled task that runs every hour to delete logs older than 24 hours.
     * This ensures data privacy by automatically removing old log entries.
     */
    @Scheduled(fixedRate = CLEANUP_INTERVAL_MS)
    public void deleteOldLogs() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(LOG_RETENTION_HOURS);
            int deletedCount = repository.findAllByTimestampBefore(cutoff).size();
            repository.deleteAllByTimestampBefore(cutoff);
            
            if (deletedCount > 0) {
                logger.info("Cleaned up {} old server log(s) older than {} hours", 
                        deletedCount, LOG_RETENTION_HOURS);
            }
        } catch (Exception e) {
            logger.error("Error during scheduled log cleanup", e);
        }
    }
}
