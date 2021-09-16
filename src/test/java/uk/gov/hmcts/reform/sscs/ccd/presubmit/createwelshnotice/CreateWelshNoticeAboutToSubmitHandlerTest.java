package uk.gov.hmcts.reform.sscs.ccd.presubmit.createwelshnotice;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.FooterDetails;
import uk.gov.hmcts.reform.sscs.service.WelshFooterService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;

@RunWith(JUnitParamsRunner.class)
public class CreateWelshNoticeAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String ENGLISH_PDF = "english.pdf";

    private CreateWelshNoticeAboutToSubmitHandler handler;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private DocmosisPdfService docmosisPdfService;
    @Mock
    private EvidenceManagementService evidenceManagementService;
    @Mock
    private WelshFooterService welshFooterService;

    String template = "TB-SCS-GNO-WEL-00473.docx";
    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new CreateWelshNoticeAboutToSubmitHandler(docmosisPdfService, evidenceManagementService, welshFooterService, template);
        when(callback.getEvent()).thenReturn(EventType.CREATE_WELSH_NOTICE);
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void canHandleCorrectly() {
        boolean actualResult = handler.canHandle(ABOUT_TO_SUBMIT, callback);
        assertTrue(actualResult);
    }

    @Test
    @Parameters({"DIRECTION_NOTICE, DIRECTION_ISSUED_WELSH, DIRECTIONS NOTICE, HYSBYSIAD CYFARWYDDIADAU",
            "DECISION_NOTICE, DECISION_ISSUED_WELSH, DECISION NOTICE, HYSBYSIAD O BENDERFYNIAD",
            "ADJOURNMENT_NOTICE, ISSUE_ADJOURNMENT_NOTICE_WELSH, ADJOURNMENT NOTICE, ADJOURNMENT NOTICE",
            "AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE, PROCESS_AUDIO_VIDEO_WELSH, DIRECTIONS NOTICE, HYSBYSIAD CYFARWYDDIADAU",
            "POSTPONEMENT_REQUEST_DIRECTION_NOTICE, ACTION_POSTPONEMENT_REQUEST_WELSH, DIRECTIONS NOTICE, HYSBYSIAD CYFARWYDDIADAU"
    })
    public void handleMethodCallsCorrectServicesAndSetsDataCorrectly(DocumentType documentType, EventType eventType, String expectedType, String expectedWelshType) {
        byte[] expectedPdf = new byte[]{2, 4, 6, 0, 1};

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), anyString())).thenReturn(uploadResponse);
        ArgumentCaptor<Object> capture = ArgumentCaptor.forClass(Object.class);
        when(docmosisPdfService.createPdf(capture.capture(),any())).thenReturn(expectedPdf);
        FooterDetails footerDetails = new FooterDetails(DocumentLink.builder().build(), "bundleAddition", "bundleFilename");
        when(welshFooterService.addFooterToExistingToContentAndCreateNewUrl(any(),any(), any(), any(), any())).thenReturn(footerDetails);

        Callback<SscsCaseData> callback = buildCallback(buildSscsDocuments(documentType),
                buildSscsWelshDocuments(documentType.getValue()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getEnglishBodyContent());
        assertNull(response.getData().getWelshBodyContent());
        assertEquals(eventType.getCcdType(), response.getData().getSscsWelshPreviewNextEvent());
        assertEquals("No",response.getData().getTranslationWorkOutstanding());
        assertEquals(ENGLISH_PDF,response.getData().getSscsWelshDocuments().get(0).getValue().getOriginalDocumentFileName());
        Map<String, Object> placeholders = (Map<String, Object>) capture.getValue();
        assertEquals(expectedType, placeholders.get("en_notice_type"));
        assertEquals(expectedWelshType, placeholders.get("cy_notice_type"));
    }

    private Callback<SscsCaseData> buildCallback(List<SscsDocument> sscsDocuments, List<SscsWelshDocument> welshDocuments) {

        DocumentType selectedDocumentType = DocumentType.fromValue(welshDocuments.get(0).getValue().getDocumentType());

        final DynamicList dynamicDocumentTypeList = new DynamicList(new DynamicListItem(selectedDocumentType.getValue(),
                selectedDocumentType.getLabel()),
                asList(new DynamicListItem(DocumentType.DIRECTION_NOTICE.getValue(), DocumentType.DIRECTION_NOTICE.getLabel()),
                        new DynamicListItem(DocumentType.DECISION_NOTICE.getValue(), DocumentType.DECISION_NOTICE.getLabel()),
                        new DynamicListItem(DocumentType.ADJOURNMENT_NOTICE.getValue(), DocumentType.ADJOURNMENT_NOTICE.getLabel())));

        final DynamicList originalNoticeTypeList = new DynamicList(new DynamicListItem(ENGLISH_PDF, ENGLISH_PDF),
                singletonList(new DynamicListItem(ENGLISH_PDF, ENGLISH_PDF)));

        SscsCaseData sscsCaseData = CaseDataUtils.buildMinimalCaseData();
        sscsCaseData.setDocumentTypes(dynamicDocumentTypeList);
        sscsCaseData.setOriginalNoticeDocuments(originalNoticeTypeList);
        sscsCaseData.setSscsWelshPreviewDocuments(welshDocuments);
        sscsCaseData.setSscsDocument(sscsDocuments);

        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.VALID_APPEAL, sscsCaseData, LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), EventType.CREATE_WELSH_NOTICE, false);
    }

    private List<SscsDocument> buildSscsDocuments(DocumentType documentType) {
        SscsDocument sscs1Doc = buildSscsDocument(documentType);
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscs1Doc);
        return sscsDocuments;
    }

    private List<SscsWelshDocument> buildSscsWelshDocuments(String documentType) {
        return singletonList(SscsWelshDocument.builder()
                .value(SscsWelshDocumentDetails.builder()
                        .documentLink(DocumentLink.builder()
                                .documentUrl("/anotherUrl")
                                .documentFilename("welsh.pdf")
                                .build())
                        .documentLanguage("welsh")
                        .documentType(documentType)
                        .build())
                .build());
    }

    private SscsDocument buildSscsDocument(DocumentType documentType) {
        return SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder()
                                .documentUrl("/anotherUrl")
                                .documentFilename(ENGLISH_PDF)
                                .build())
                        .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED)
                        .documentType(documentType.getValue())
                        .build())
                .build();
    }

    private UploadResponse createUploadResponse() {
        UploadResponse response = mock(UploadResponse.class);
        UploadResponse.Embedded embedded = mock(UploadResponse.Embedded.class);
        when(response.getEmbedded()).thenReturn(embedded);
        Document document = createDocument();
        when(embedded.getDocuments()).thenReturn(singletonList(document));
        return response;
    }

    private static Document createDocument() {
        Document document = new Document();
        Document.Links links = new Document.Links();
        Document.Link link = new Document.Link();
        link.href = "some location";
        links.self = link;
        document.links = links;
        return document;
    }
}
