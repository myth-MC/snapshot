package ovh.mythmc.snapshot.server.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice to add common attributes to all models.
 * 
 * @author U8092
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    @Value("${spring.application.version:0.1.0}")
    private String applicationVersion;

    /**
     * Adds the application version to all models.
     * Reads from Spring Boot's manifest, falls back to default if not available.
     * 
     * @return the application version
     */
    @ModelAttribute("appVersion")
    public String getAppVersion() {
        return applicationVersion;
    }
}
