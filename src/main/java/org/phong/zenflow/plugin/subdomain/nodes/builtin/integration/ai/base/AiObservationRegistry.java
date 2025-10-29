package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Base observation registry for AI operations with default logging.
 * Models can copy() to create independent registries with custom handlers.
 */
@Component
@Slf4j
public class AiObservationRegistry {
    
    private final ObservationRegistry registry;
    
    public AiObservationRegistry() {
        this.registry = ObservationRegistry.create();
        this.registry.observationConfig()
                .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(
                        new LoggingObservationHandler()
                ));
        log.info("Base AiObservationRegistry initialized with default logging handler");
    }
    
    /**
     * Private constructor for copy() - creates new registry with same config
     */
    private AiObservationRegistry(ObservationRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * Creates independent copy - models use this to avoid sharing state
     */
    public AiObservationRegistry copy() {
        ObservationRegistry newRegistry = ObservationRegistry.create();
        newRegistry.observationConfig()
                .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(
                        new LoggingObservationHandler()
                ));
        log.debug("Created copy of AiObservationRegistry");
        return new AiObservationRegistry(newRegistry);
    }
    
    public AiObservationRegistry addHandler(ObservationHandler<Observation.Context> handler) {
        this.registry.observationConfig().observationHandler(handler);
        log.info("Added custom observation handler: {}", handler.getClass().getSimpleName());
        return this;
    }
    
    public ObservationRegistry getRegistry() {
        return registry;
    }
    
    private static class LoggingObservationHandler implements ObservationHandler<Observation.Context> {
        
        @Override
        public void onStart(Observation.Context context) {
            log.debug("AI operation started: {}", context.getName());
        }
        
        @Override
        public void onStop(Observation.Context context) {
            log.debug("AI operation completed: {} (duration: {}ms)", 
                    context.getName(), 
                    context.getOrDefault("duration", "unknown"));
        }
        
        @Override
        public void onError(Observation.Context context) {
            log.warn("AI operation failed: {}", context.getName(), context.getError());
        }
        
        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }
    }
}
