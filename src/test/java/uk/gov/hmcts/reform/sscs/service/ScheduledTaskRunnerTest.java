package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import uk.gov.hmcts.reform.sscs.scheduled.MigrateCasesTask;

@ExtendWith(MockitoExtension.class)
class ScheduledTaskRunnerTest {

    @Mock
    private ApplicationContext context;

    @Mock
    private MigrateCasesTask migrateCasesTask;

    @InjectMocks
    private ScheduledTaskRunner taskRunner;

    @Mock
    private Runnable task;

    @Test
    void shouldRunValidTask() {
        when(context.getBean("migrateCasesTask")).thenReturn(migrateCasesTask);

        taskRunner.run("MigrateCasesTask");

        verify(migrateCasesTask).run();
    }

    @Test
    void shouldNotFindTheBean() {
        when(context.getBean("missingBean")).thenThrow();

        taskRunner.run("missingBean");

        verifyNoInteractions(task);
    }

}
