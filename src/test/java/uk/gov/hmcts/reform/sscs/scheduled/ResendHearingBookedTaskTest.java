package uk.gov.hmcts.reform.sscs.scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@ExtendWith(MockitoExtension.class)
public class ResendHearingBookedTaskTest {

    @Mock
    private IdamService idamService;
    @Mock
    private CcdService ccdService;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;
    @InjectMocks
    ResendHearingBookedTask resendHearingBookedTask;

    private SscsCaseDetails sscsCaseDetails;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resendHearingBookedTask, "caseReferences", "1234");
        sscsCaseDetails = SscsCaseDetails.builder().id(1234L).build();
    }

    @Test
    public void shouldCallCcdWithResendHearingBooked_WhenReferenceIsPresent() {
        when(ccdService.getByCaseId(eq(1234L), any())).thenReturn(sscsCaseDetails);
        resendHearingBookedTask.run();
        verify(updateCcdCaseService).triggerCaseEventV2(eq(1234L),
                eq(EventType.RESEND_HEARING_BOOKED.getCcdType()), anyString(), any(), any());
        assertThat(resendHearingBookedTask.getMigratedCases()).contains(1234L);
    }

    @Test
    public void shouldFailCase_WhenCcdReturnsException() {
        when(ccdService.getByCaseId(eq(1234L), any())).thenReturn(sscsCaseDetails);
        when(updateCcdCaseService.triggerCaseEventV2(eq(1234L),
                eq(EventType.RESEND_HEARING_BOOKED.getCcdType()),
                anyString(), any(), any())).thenThrow(new RuntimeException());
        resendHearingBookedTask.run();
        assertThat(resendHearingBookedTask.getFailedCases()).contains(1234L);
    }

}
