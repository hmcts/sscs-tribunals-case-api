package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import uk.gov.hmcts.reform.sscs.scheduled.ResendHearingBookedTask;

@ExtendWith(MockitoExtension.class)
class ScheduledTaskRunnerTest {

    @Mock
    private ApplicationContext context;

    @Mock
    private ResendHearingBookedTask resendHearingBookedTask;

    @InjectMocks
    private ScheduledTaskRunner taskRunner;

    @Mock
    private Runnable task;

    @Test
    void shouldRunValidTask() {
        when(context.getBean("resendHearingBookedTask")).thenReturn(resendHearingBookedTask);

        taskRunner.run("ResendHearingBookedTask");

        verify(resendHearingBookedTask).run();
    }

    @Test
    void shouldNotFindTheBean() {
        when(context.getBean("missingBean")).thenThrow();

        taskRunner.run("missingBean");

        verifyNoInteractions(task);
    }

}
