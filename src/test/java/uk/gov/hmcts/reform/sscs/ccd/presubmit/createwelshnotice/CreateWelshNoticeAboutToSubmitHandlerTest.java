package uk.gov.hmcts.reform.sscs.ccd.presubmit.createwelshnotice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_WELSH_NOTICE;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;

@RunWith(JUnitParamsRunner.class)
public class CreateWelshNoticeAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private CreateWelshNoticeAboutToSubmitHandler handler;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private DocmosisPdfService docmosisPdfService;
    @Mock
    private EvidenceManagementService evidenceManagementService;

    String template = "TB-SCS-GNO-WEL-00473.docx";
    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new CreateWelshNoticeAboutToSubmitHandler(docmosisPdfService, evidenceManagementService, template);
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
    public void handleMethodCallsCorrectServicesAndSetsDataCorrectly() {
        byte[] expectedPdf = new byte[]{2, 4, 6, 0, 1};

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), anyString())).thenReturn(uploadResponse);
        when(docmosisPdfService.createPdf(any(),any())).thenReturn(expectedPdf);
        Callback<SscsCaseData> callback = buildCallback("english.pdf", CREATE_WELSH_NOTICE, buildSscsDocuments(),
                buildSscsWelshDocuments(DocumentType.DIRECTION_NOTICE.getValue()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback,
                USER_AUTHORISATION);
        assertNull(response.getData().getEnglishBodyContent());
        assertNull(response.getData().getWelshBodyContent());
        assertNull(response.getData().getSignedBy());
        assertNull(response.getData().getSignedRole());
        assertNull(response.getData().getDateAdded());
        assertEquals(EventType.DIRECTION_ISSUED_WELSH.getCcdType(), response.getData().getSscsWelshPreviewNextEvent());
        assertEquals("No",response.getData().getTranslationWorkOutstanding());
        assertEquals("english.pdf",response.getData().getSscsWelshDocuments().get(0).getValue().getOriginalDocumentFileName());
    }

    private Callback<SscsCaseData> buildCallback(String dynamicListItemCode, EventType eventType,
                                                 List<SscsDocument> sscsDocuments, List<SscsWelshDocument> welshDocuments) {

        final DynamicList dynamicDocumentTypeList = new DynamicList(new DynamicListItem("Direction Notice",
                "Direction Notice"),
                Collections.singletonList(new DynamicListItem("Direction Notice", "Direction Notice")));

        final DynamicList originalNoticeTypeList = new DynamicList(new DynamicListItem(dynamicListItemCode,
                dynamicListItemCode),
                Collections.singletonList(new DynamicListItem(dynamicListItemCode, dynamicListItemCode)));

        SscsCaseData sscsCaseData = CaseDataUtils.buildMinimalCaseData();
        sscsCaseData.setDocumentTypes(dynamicDocumentTypeList);
        sscsCaseData.setOriginalNoticeDocuments(originalNoticeTypeList);
        sscsCaseData.setSscsWelshPreviewDocuments(welshDocuments);
        sscsCaseData.setSscsDocument(sscsDocuments);

        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.VALID_APPEAL, sscsCaseData, LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), eventType, false);
    }

    private List<SscsDocument> buildSscsDocuments() {
        SscsDocument sscs1Doc = buildSscsDocument("english.pdf");
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscs1Doc);
        return sscsDocuments;
    }

    private List<SscsWelshDocument> buildSscsWelshDocuments(String documentType) {
        return Collections.singletonList(SscsWelshDocument.builder()
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

    private SscsDocument buildSscsDocument(String filename) {
        return SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder()
                                .documentUrl("/anotherUrl")
                                .documentFilename(filename)
                                .build())
                        .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED)
                        .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                        .build())
                .build();
    }

    private UploadResponse createUploadResponse() {
        UploadResponse response = mock(UploadResponse.class);
        UploadResponse.Embedded embedded = mock(UploadResponse.Embedded.class);
        when(response.getEmbedded()).thenReturn(embedded);
        Document document = createDocument();
        when(embedded.getDocuments()).thenReturn(Collections.singletonList(document));
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