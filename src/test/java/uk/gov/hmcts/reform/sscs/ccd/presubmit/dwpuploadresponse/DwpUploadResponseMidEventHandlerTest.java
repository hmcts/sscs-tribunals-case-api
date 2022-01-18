package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;


@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    private DwpUploadResponseMidEventHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;


    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    private SscsCaseData sscsCaseData;

    private SscsCaseData sscsCaseDataBefore;

    @Before
    public void setUp() {
        handler = new DwpUploadResponseMidEventHandler();

        openMocks(this);

        AddNoteService addNoteService = new AddNoteService(userDetailsService);


        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
                .forename("Chris").surname("Davis").build().getFullName());

        when(callback.getEvent()).thenReturn(EventType.DWP_UPLOAD_RESPONSE);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .benefitCode("002")
                .issueCode("CC")
                .dwpFurtherInfo("Yes")
                .dwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("a.pdf").documentFilename("a.pdf").build()).build())
                .dwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("b.pdf").documentFilename("b.pdf").build()).build())
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("taxCredit").build()).build())
                .build();

        sscsCaseDataBefore = SscsCaseData.builder().build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetails.getId()).thenReturn(Long.valueOf(sscsCaseData.getCcdCaseId()));
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);

    }


    @Test
    public void testCaseTaxCreditWithEditedEvidenceReasonIsConfidentialityAppendix12DocHaveDocumentThenReject() {

        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("childSupportConfidentiality");
        callback.getCaseDetails().getCaseData().setAppendix12Doc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("b.pdf").documentFilename("b.pdf").build()).build());
        callback.getCaseDetails().getCaseData().getAppendix12Doc().setDocumentLink(DocumentLink.builder().documentUrl("b.pdf").documentFilename("b.pdf").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);


        assertNotNull(response.getErrors());
    }

    @Test
    public void testCaseTaxCreditWithEditedEvidenceReasonIsConfidentialityAppendix12DocHaveNoDocumentThenPass() {

        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("childSupportConfidentiality");
        callback.getCaseDetails().getCaseData().setAppendix12Doc(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(empty()));
    }


}
