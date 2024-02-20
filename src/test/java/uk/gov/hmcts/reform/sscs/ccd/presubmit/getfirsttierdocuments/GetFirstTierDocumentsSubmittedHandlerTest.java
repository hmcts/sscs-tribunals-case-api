package uk.gov.hmcts.reform.sscs.ccd.presubmit.getfirsttierdocuments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdCallbackMap;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;

@ExtendWith(MockitoExtension.class)
public class GetFirstTierDocumentsSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private GetFirstTierDocumentsSubmittedHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CcdCallbackMapService ccdCallbackMapService;

    private SscsCaseData sscsCaseData;

    private final ArgumentCaptor<CcdCallbackMap> capture = ArgumentCaptor.forClass(CcdCallbackMap.class);


    @BeforeEach
    public void setUp() {
        handler = new GetFirstTierDocumentsSubmittedHandler(true, true, ccdCallbackMapService);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("1").build();
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.GET_FIRST_TIER_DOCUMENTS);
        assertThat(handler.canHandle(SUBMITTED, callback)).isTrue();
    }

    @Test
    public void givenANonGetFirstTierDocumentsEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.GET_FIRST_TIER_DOCUMENTS);
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    public void shouldUpdateEvent() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(ccdCallbackMapService.handleCcdCallbackMap(capture.capture(), eq(sscsCaseData))).thenReturn(sscsCaseData);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(ccdCallbackMapService).handleCcdCallbackMap(capture.capture(), eq(sscsCaseData));
        assertThat(capture.getValue().getCallbackEvent()).isEqualTo(EventType.BUNDLE_CREATED_FOR_UPPER_TRIBUNAL);
        assertThat(capture.getValue().getCallbackSummary()).isEqualTo("Bundle created for UT");
        assertThat(capture.getValue().getCallbackDescription()).isEqualTo("Bundle created for UT");
    }
}
