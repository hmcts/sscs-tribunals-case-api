package uk.gov.hmcts.reform.sscs.ccd.presubmit.requesttranslation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.REQUEST_TRANSLATION_FROM_WLU;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.domain.email.RequestTranslationTemplate;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.EmailService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;

@RunWith(JUnitParamsRunner.class)
public class RequestTranslationAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private IdamService idamService;
    @Mock
    private DocmosisPdfService docmosisPdfService;
    @Mock
    private EvidenceManagementService evidenceManagementService;
    @Mock
    private EmailService emailService;
    @Mock
    private RequestTranslationTemplate requestTranslationTemplate;

    private RequestTranslationAboutToSubmitHandler handler;

    @Before
    public void setUp() {
        handler  = new RequestTranslationAboutToSubmitHandler(docmosisPdfService, evidenceManagementService,
                requestTranslationTemplate, emailService, idamService);
    }

    @Test
    public void canHandleCorrectly() {
        Callback<SscsCaseData> callback = buildCallback(buildSscsDocuments());
        boolean actualResult = handler.canHandle(ABOUT_TO_SUBMIT, callback);
        assertTrue(actualResult);
    }

    @Test
    public void requestTranslationForWelshCase() {
        byte[] expectedPdf = new byte[]{2, 4, 6, 0, 1};
        byte[] expectedBytes = new byte[]{1, 2, 3};
        given(evidenceManagementService.download(any(), any())).willReturn(expectedBytes);
        when(docmosisPdfService.createPdf(any(), any())).thenReturn(expectedPdf);
        Callback<SscsCaseData> callback = buildCallback(buildSscsDocuments());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback,
                USER_AUTHORISATION);
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED,
                response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());
    }

    private Callback<SscsCaseData> buildCallback(List<SscsDocument> sscsDocuments) {
        SscsCaseData sscsCaseData = CaseDataUtils.buildMinimalCaseData();
        sscsCaseData.setCcdCaseId("123");
        sscsCaseData.setSscsDocument(sscsDocuments);
        sscsCaseData.setLanguagePreferenceWelsh("Yes");
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.VALID_APPEAL, sscsCaseData, LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), REQUEST_TRANSLATION_FROM_WLU, false);
    }

    private List<SscsDocument> buildSscsDocuments() {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder()
                                .documentUrl("/anotherUrl")
                                .documentFilename("english.pdf")
                                .build())
                        .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED)
                        .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                        .build())
                .build());
        return sscsDocuments;
    }
}