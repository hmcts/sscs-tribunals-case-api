package uk.gov.hmcts.reform.sscs;

import org.springframework.core.task.TaskDecorator;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class ContextCopyingTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {

        String authorization = null;
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            authorization = sra.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }

        var snapshot = new RequestSnapshotHolder.RequestSnapshot(authorization);

        return () -> {
            try {
                RequestSnapshotHolder.set(snapshot);
                task.run();
            } finally {
                RequestSnapshotHolder.clear();
            }
        };
    }
}

