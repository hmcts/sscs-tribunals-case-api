package uk.gov.hmcts.reform.sscs.ccd.presubmit.procesaudiovideo;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.EXCLUDE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.ISSUE_DIRECTIONS_NOTICE;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoEvidenceAboutToStartHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;

@RunWith(JUnitParamsRunner.class)
public class ProcessAudioVideoEvidenceAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private ProcessAudioVideoEvidenceAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;

    private SscsCaseData sscsCaseData;

    private final UserDetails userDetails = UserDetails.builder().roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue()))).build();

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ProcessAudioVideoEvidenceAboutToStartHandler(idamService);

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.PROCESS_AUDIO_VIDEO);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    }

    @Test
    public void givenAudioEvidenceListIsNull_ReturnError() {
        sscsCaseData.setAudioVideoEvidence(null);
        assertNoEvidenceError();
    }

    @Test
    public void givenAudioEvidenceListIsEmpty_ReturnError() {
        sscsCaseData.setAudioVideoEvidence(List.of());
        assertNoEvidenceError();
    }

    private void assertNoEvidenceError() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("Before running this event audio and video evidence must be uploaded", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenAudioEvidenceListIsNotEmpty_ProcessAudioVideoActionListIsCreated() {
        sscsCaseData.setAudioVideoEvidence(List.of(AudioVideoEvidence.builder().build()));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        SscsCaseData responseData = response.getData();
        assertEquals(1, responseData.getProcessAudioVideoAction().getListItems().size());
        assertEquals(ISSUE_DIRECTIONS_NOTICE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), ISSUE_DIRECTIONS_NOTICE.getCode()));
    }

    @Test
    @Parameters({"JUDGE", "SUPER_USER"})
    public void givenJudgeOrSuperUserRole_thenUserCanProcessMoreAudioVideoActions(UserRole userRole) {
        userDetails.getRoles().add(userRole.getValue());
        sscsCaseData.setAudioVideoEvidence(List.of(AudioVideoEvidence.builder().build()));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        SscsCaseData responseData = response.getData();
        assertEquals(2, responseData.getProcessAudioVideoAction().getListItems().size());
        assertEquals(ISSUE_DIRECTIONS_NOTICE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), ISSUE_DIRECTIONS_NOTICE.getCode()));
        assertEquals(EXCLUDE_EVIDENCE.getCode(), getItemCodeInList(responseData.getProcessAudioVideoAction(), EXCLUDE_EVIDENCE.getCode()));
    }

    private String getItemCodeInList(DynamicList dynamicList, String item) {
        return dynamicList.getListItems().stream()
                .filter(o -> item.equals(o.getCode()))
                .findFirst()
                .map(DynamicListItem::getCode)
                .orElse(null);
    }
}
