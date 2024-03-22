package uk.gov.hmcts.reform.sscs.servicebus;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.qpid.server.SystemLauncher;
import org.apache.qpid.server.configuration.IllegalConfigurationException;
import org.apache.qpid.server.model.SystemConfig;

class EmbeddedInMemoryQpidBroker {

    private static final String DEFAULT_INITIAL_CONFIGURATION_LOCATION = "qpid-embedded-inmemory-configuration.json";

    private final SystemLauncher systemLauncher;

    EmbeddedInMemoryQpidBroker() {
        this.systemLauncher = new SystemLauncher();
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    void start() throws Exception {
        this.systemLauncher.startup(createSystemConfig());
    }

    void shutdown() {
        this.systemLauncher.shutdown();
    }

    private Map<String, Object> createSystemConfig() throws IllegalConfigurationException {
        URL initialConfigUrl = Thread.currentThread().getContextClassLoader()
            .getResource(DEFAULT_INITIAL_CONFIGURATION_LOCATION);
        if (initialConfigUrl == null) {
            throw new IllegalConfigurationException("Configuration location '"
                + DEFAULT_INITIAL_CONFIGURATION_LOCATION + "' not found");
        }
        Map<String, Object> attributes = new ConcurrentHashMap<>();
        attributes.put(SystemConfig.TYPE, "Memory");
        attributes.put(SystemConfig.INITIAL_CONFIGURATION_LOCATION, initialConfigUrl.toExternalForm());
        attributes.put(SystemConfig.STARTUP_LOGGED_TO_SYSTEM_OUT, true);
        return attributes;
    }
}
