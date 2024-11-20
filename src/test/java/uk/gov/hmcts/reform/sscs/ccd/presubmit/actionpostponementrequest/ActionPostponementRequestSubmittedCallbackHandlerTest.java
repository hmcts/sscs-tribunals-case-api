package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@ExtendWith(MockitoExtension.class)
public class ActionPostponementRequestSubmittedCallbackHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    ActionPostponementRequestSubmittedCallbackHandler handler;

    SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock SscsCaseDetails sscsCaseDetails;

    @Mock
    UpdateCcdCaseService ccdService;

    @Mock
    IdamService idamService;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder()
                .documentGeneration(DocumentGeneration.builder()
                        .generateNotice(YES)
                        .build())
                .appeal(Appeal.builder().appellant(Appellant.builder()
                        .name(Name.builder().firstName("APPELLANT").lastName("LastNamE").build())
                        .identity(Identity.builder().build()).build()).build())
                .directionDueDate(LocalDate.now().plusDays(1).toString())
                .postponementRequest(PostponementRequest.builder().build())
                .build();

        handler = new ActionPostponementRequestSubmittedCallbackHandler(ccdService, idamService, true);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.ACTION_POSTPONEMENT_REQUEST);
        assertThat(handler.canHandle(SUBMITTED, callback)).isTrue();
    }

    @Test
    public void givenIncorrectCallbackType_thenThrowError() {
        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    public void givenWaFlagIsFalse_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.ACTION_POSTPONEMENT_REQUEST);
        handler = new ActionPostponementRequestSubmittedCallbackHandler(ccdService, idamService, false);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }


    @Test
    public void givePostponementActionIsNull_thenShouldNotThrowError() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getEvent()).thenReturn(EventType.ACTION_POSTPONEMENT_REQUEST);
        var result = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(result).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = ProcessRequestAction.class)
    public void givenPostponementRequestSubmitted_whenUpdateCase(ProcessRequestAction processRequestAction) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(sscsCaseDetails.getData()).thenReturn(sscsCaseData);
        when(callback.getEvent()).thenReturn(EventType.ACTION_POSTPONEMENT_REQUEST);
        when(ccdService
                .triggerCaseEventV2(anyLong(), anyString(), anyString(), anyString(), any())
        ).thenReturn(sscsCaseDetails);
        sscsCaseData.getPostponementRequest().setActionPostponementRequestSelected(processRequestAction.getValue());

        var result = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(result).isNotNull();
        verify(ccdService, times(1)).triggerCaseEventV2(any(), any(), any(), any(), any());
    }
}
