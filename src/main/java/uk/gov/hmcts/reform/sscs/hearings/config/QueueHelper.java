package uk.gov.hmcts.reform.sscs.hearings.config;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusFailureReason;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class QueueHelper {

    private QueueHelper() {
    }

    public static void processError(ServiceBusErrorContext context) {
        log.error("Error when receiving messages from namespace: '{}'. Entity: '{}'",
            context.getFullyQualifiedNamespace(), context.getEntityPath()
        );

        if (!(context.getException() instanceof ServiceBusException)) {
            log.error("Non-ServiceBusException occurred: {}", context.getException().toString());
            return;
        }

        ServiceBusException exception = (ServiceBusException) context.getException();
        ServiceBusFailureReason reason = exception.getReason();
        if (Objects.equals(reason, ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED)
            || Objects.equals(reason, ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND)
            || Objects.equals(reason, ServiceBusFailureReason.UNAUTHORIZED)) {
            log.error("An unrecoverable error occurred. Stopping processing with reason {}: {}",
                reason, exception.getMessage()
            );
        } else if (Objects.equals(reason, ServiceBusFailureReason.MESSAGE_LOCK_LOST)) {
            log.warn("Message lock lost for message: {}", context.getException().toString());
        } else {
            log.error("Error source {}, reason {}, message: {}", context.getErrorSource(),
                reason, context.getException()
            );
        }
    }
}
