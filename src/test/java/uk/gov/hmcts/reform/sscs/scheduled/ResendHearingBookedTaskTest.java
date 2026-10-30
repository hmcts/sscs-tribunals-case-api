package uk.gov.hmcts.reform.sscs.scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@ExtendWith(MockitoExtension.class)
public class ResendHearingBookedTaskTest {

    @Mock
    private IdamService idamService;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;
    @InjectMocks
    ResendHearingBookedTask resendHearingBookedTask;

    @Test
    public void shouldCallCcdWithResendHearingBooked_WhenReferenceIsPresent() {
        ReflectionTestUtils.setField(resendHearingBookedTask, "caseReferences", "1234");
        resendHearingBookedTask.run();
        verify(updateCcdCaseService).triggerCaseEventV2(eq(1234L),
                eq(EventType.RESEND_HEARING_BOOKED.getCcdType()), anyString(), any(), any());
        assertThat(resendHearingBookedTask.getMigratedCases()).contains(1234L);
    }

    @Test
    public void shouldFailCase_WhenCcdReturnsException() {
        ReflectionTestUtils.setField(resendHearingBookedTask, "caseReferences", "1234");
        when(updateCcdCaseService.triggerCaseEventV2(eq(1234L),
                eq(EventType.RESEND_HEARING_BOOKED.getCcdType()),
                anyString(), any(), any())).thenThrow(new RuntimeException());
        resendHearingBookedTask.run();
        assertThat(resendHearingBookedTask.getFailedCases()).contains(1234L);
    }

}
