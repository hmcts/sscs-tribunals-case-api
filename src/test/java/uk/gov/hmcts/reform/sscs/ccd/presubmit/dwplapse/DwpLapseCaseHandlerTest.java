package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwplapse;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.LAPSED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;


@RunWith(JUnitParamsRunner.class)
public class DwpLapseCaseHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private DwpLapseCaseHandler handler;
    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private DwpDocumentService dwpDocumentService;

    @Before
    public void setUp() {
        openMocks(this);
        dwpDocumentService = new DwpDocumentService();
        handler = new DwpLapseCaseHandler(dwpDocumentService);

        when(callback.getEvent()).thenReturn(EventType.DWP_LAPSE_CASE);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .createdInGapsFrom(State.READY_TO_LIST.getId())
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }


    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleDwpLapseEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAHandleDwpLapseEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.DWP_LAPSE_CASE);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenDwpLapseCaseEventWithEmptyDwpDocuments_thenSetInterlocReviewToAdminAndMoveLt203DocToDwpDocuments() {
        sscsCaseData = sscsCaseData.toBuilder().state(State.WITH_DWP)
                .dwpLT203(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("lt203Link").build()).build())
                .dwpLapseLetter(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("lapseLink").build()).build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getDwpLT203());
        assertNull(response.getData().getDwpLapseLetter());

        assertEquals(2, response.getData().getDwpDocuments().size());

        assertThat(response.getData().getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                        hasProperty("documentLink", allOf(
                                hasProperty("documentUrl", is("lt203Link"))
                        )),
                        hasProperty("documentType", is(DwpDocumentType.DWP_LT_203.getValue()))
                ))
        ));

        assertThat(response.getData().getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                        hasProperty("documentLink", allOf(
                                hasProperty("documentUrl", is("lapseLink"))
                        )),
                        hasProperty("documentType", is(DwpDocumentType.DWP_LAPSE_LETTER.getValue()))
                ))
        ));

        assertEquals(AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertEquals(LAPSED, response.getData().getDwpState());
    }

    @Test
    public void givenDwpLapseCaseEventWithExistingDwpDocuments_thenSetInterlocReviewToAdminAndMoveDocsToDwpDocuments() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing.com").build()).build()).build());

        sscsCaseData = sscsCaseData.toBuilder().state(State.WITH_DWP)
                .dwpDocuments(dwpDocuments)
                .dwpLT203(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("lt203Link").build()).build())
                .dwpLapseLetter(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("lapseLink").build()).build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getDwpLT203());
        assertNull(response.getData().getDwpLapseLetter());

        assertEquals(3, response.getData().getDwpDocuments().size());

        assertThat(response.getData().getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                        hasProperty("documentLink", allOf(
                                hasProperty("documentUrl", is("lt203Link"))
                        )),
                        hasProperty("documentType", is(DwpDocumentType.DWP_LT_203.getValue()))
                ))
        ));

        assertThat(response.getData().getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                        hasProperty("documentLink", allOf(
                                hasProperty("documentUrl", is("lapseLink"))
                        )),
                        hasProperty("documentType", is(DwpDocumentType.DWP_LAPSE_LETTER.getValue()))
                ))
        ));

        assertThat(response.getData().getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                        hasProperty("documentLink", allOf(
                                hasProperty("documentUrl", is("existing.com"))
                        ))
                ))
        ));

        assertEquals(AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertEquals(LAPSED, response.getData().getDwpState());
    }
}
