package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.HandlerHelper.buildCallback;
import static uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;

import java.util.HashMap;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class ManualCaseCreatedHandlerTest {

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @InjectMocks
    private ManualCaseCreatedHandler handler;

    @Before
    public void setUp() {
        openMocks(this);
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void shouldThrowException_givenCallbackIsNotSubmitted(CallbackType callbackType) {
        Callback<SscsCaseData> sscsCaseDataCallback = buildTestCallbackForGivenData(null, READY_TO_LIST, VALID_APPEAL_CREATED);

        assertThrows(IllegalStateException.class, () ->
            handler.handle(callbackType, sscsCaseDataCallback)
        );
    }

    @Test
    @Parameters({"VALID_APPEAL_CREATED", "INCOMPLETE_APPLICATION_RECEIVED", "NON_COMPLIANT"})
    public void shouldReturnTrue_givenAQualifyingEvent(EventType eventType) {
        assertTrue(handler.canHandle(SUBMITTED,
            buildTestCallbackForGivenData(SscsCaseData.builder()
                    .createdInGapsFrom(READY_TO_LIST.getId()).build(),
                READY_TO_LIST,
                eventType)
        ));
    }

    @Test
    public void shouldReturnFalse_givenANonQualifyingCallbackType() {
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT,
            buildTestCallbackForGivenData(SscsCaseData.builder()
                    .createdInGapsFrom(READY_TO_LIST.getId()).build(),
                READY_TO_LIST,
                NON_COMPLIANT)
        ));
    }

    @Test
    public void shouldReturnFalse_givenANonQualifyingEvent() {
        assertFalse(handler.canHandle(SUBMITTED,
            buildTestCallbackForGivenData(SscsCaseData.builder()
                    .createdInGapsFrom(READY_TO_LIST.getId()).build(),
                READY_TO_LIST,
                DECISION_ISSUED)
        ));
    }

    public void shouldThrowException_givenCallbackIsNull() {
        assertThrows(NullPointerException.class, () ->
            handler.canHandle(SUBMITTED, null)
        );
    }

    @Test
    public void shouldAddServiceId_givenNullSupplementaryData() {
        handler.handle(SUBMITTED, buildCallback(SscsCaseData.builder()
                .createdInGapsFrom(READY_TO_LIST.getId()).build(),
            READY_TO_LIST,
            VALID_APPEAL_CREATED)
        );

        verify(ccdService).setSupplementaryData(any(), any(), eq(getWrappedData(getSupplementaryData())));
    }

    @Test
    public void shouldUpdateCcd_givenCaseAccessManagementFeatureEnabled() {
        setField(handler, "caseAccessManagementFeature", true);
        Callback<SscsCaseData> callback = buildCallback(SscsCaseData.builder()
                .createdInGapsFrom(READY_TO_LIST.getId()).build(),
            READY_TO_LIST,
            VALID_APPEAL_CREATED);

        handler.handle(SUBMITTED, callback);

        verify(ccdService).updateCase(
            eq(callback.getCaseDetails().getCaseData()),
            eq(callback.getCaseDetails().getId()),
            eq(UPDATE_CASE_ONLY.getCcdType()),
            eq("Case Update - Manual Case Created"),
            eq("Case was updated in SSCS-Evidence-Share"),
            any());
    }

    private Map<String, Map<String, Object>> getSupplementaryData() {
        Map<String, Object> hmctsServiceIdMap = new HashMap<>();
        hmctsServiceIdMap.put("HMCTSServiceId", "BBA3");
        Map<String, Map<String, Object>> supplementaryDataRequestMap = new HashMap<>();
        supplementaryDataRequestMap.put("$set", hmctsServiceIdMap);
        return supplementaryDataRequestMap;
    }

    private Map<String, Map<String, Map<String, Object>>> getWrappedData(Map<String, Map<String, Object>> supplementaryData) {
        Map<String, Map<String, Map<String, Object>>> supplementaryDataUpdates = new HashMap<>();
        supplementaryDataUpdates.put("supplementary_data_updates", supplementaryData);
        return supplementaryDataUpdates;
    }

}
