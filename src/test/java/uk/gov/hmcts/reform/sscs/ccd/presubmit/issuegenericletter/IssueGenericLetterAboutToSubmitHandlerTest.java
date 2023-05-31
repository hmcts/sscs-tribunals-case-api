package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuegenericletter;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_GENERIC_LETTER;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentSelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

class IssueGenericLetterAboutToSubmitHandlerTest {

    private IssueGenericLetterAboutToSubmitHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;


    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String DOCUMENT_NAME_1 = "DocumentName_1";
    private static final String DOCUMENT_NAME_2 = "DocumentName_2";

    private static final String CASE_ID = "1111111111111111";

    @BeforeEach
    protected void setUp() {
        openMocks(this);

        handler = new IssueGenericLetterAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(ISSUE_GENERIC_LETTER);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        caseData = SscsCaseData.builder()
                .ccdCaseId(CASE_ID)
                .appeal(Appeal.builder().build())
                .sscsDocument(List.of())
                .genericLetterText(" ").build();
    }

    @Test
    void givenThereIsDuplicationInDocumentsSelection_thenRemoveItBeforeSubmit() {
        var list = new ArrayList<DynamicListItem>();

        DynamicListItem item1 = new DynamicListItem(DOCUMENT_NAME_1, DOCUMENT_NAME_1);
        DynamicListItem item2 = new DynamicListItem(DOCUMENT_NAME_1, DOCUMENT_NAME_1);
        list.add(item1);
        list.add(item2);

        List<CcdValue<DocumentSelectionDetails>> documentSelection = List.of(
                new CcdValue<>(new DocumentSelectionDetails(new DynamicList(null, list))),
                new CcdValue<>(new DocumentSelectionDetails(new DynamicList(null, list)))
        );

        caseData.setDocumentSelection(documentSelection);

        when(caseDetails.getCaseData()).thenReturn(caseData);

        var result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, result.getData().getDocumentSelection().size());
    }

    @Test
    void givenThereIsDuplicationInOtherPartySelection_thenRemoveItBeforeSubmit() {
        var list = new ArrayList<DynamicListItem>();

        var item = new DynamicListItem(DOCUMENT_NAME_1, DOCUMENT_NAME_1);

        List<CcdValue<OtherPartySelectionDetails>> otherPartySelection = List.of(
                new CcdValue<>(new OtherPartySelectionDetails(new DynamicList(item, list))),
                new CcdValue<>(new OtherPartySelectionDetails(new DynamicList(item, list)))
        );
        caseData.setOtherPartySelection(otherPartySelection);

        when(caseDetails.getCaseData()).thenReturn(caseData);

        var result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, result.getData().getOtherPartySelection().size());
    }

    @Test
    void givenThereAreNoDuplicatesInDocuments_thenReturnListIntact() {
        var list = new ArrayList<DynamicListItem>();

        var item1 = new DynamicListItem(DOCUMENT_NAME_1, DOCUMENT_NAME_1);
        var item2 = new DynamicListItem(DOCUMENT_NAME_2, DOCUMENT_NAME_2);

        List<CcdValue<DocumentSelectionDetails>> documentSelection = List.of(
                new CcdValue<>(new DocumentSelectionDetails(new DynamicList(item1, list))),
                new CcdValue<>(new DocumentSelectionDetails(new DynamicList(item2, list))));
        caseData.setDocumentSelection(documentSelection);

        when(caseDetails.getCaseData()).thenReturn(caseData);

        var result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        List<CcdValue<DocumentSelectionDetails>> documentSelectionDetails = result.getData().getDocumentSelection();

        Assertions.assertEquals(2, documentSelectionDetails.size());
        Assertions.assertEquals(DOCUMENT_NAME_1, getCode(documentSelectionDetails.get(0)));
    }

    private String getCode(CcdValue<DocumentSelectionDetails> documentSelection) {
        return documentSelection.getValue().getDocumentsList().getValue().getCode();
    }
}
