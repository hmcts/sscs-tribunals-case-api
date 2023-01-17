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

public class IssueGenericLetterAboutToSubmitHandlerTest {

    private IssueGenericLetterAboutToSubmitHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    private static final String USER_AUTHORISATION = "Bearer token";

    @BeforeEach
    protected void setUp() {
        openMocks(this);

        handler = new IssueGenericLetterAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(ISSUE_GENERIC_LETTER);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
    }

    @Test
    public void givenThereIsDuplicationInDocumentsSelection_thenRemoveItBeforeSubmit() {
        var list = new ArrayList<DynamicListItem>();

        list.add(new DynamicListItem("DocumentName", "DocumentName"));
        list.add(new DynamicListItem("DocumentName", "DocumentName"));

        caseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .appeal(Appeal.builder().build())
                .sscsDocument(List.of())
                .documentSelection(List.of(
                        new CcdValue<>(new DocumentSelectionDetails(new DynamicList(null, list))),
                        new CcdValue<>(new DocumentSelectionDetails(new DynamicList(null, list)))
                        ))
                .genericLetterText(" ")
                .build();

        when(caseDetails.getCaseData()).thenReturn(caseData);

        var result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, result.getData().getDocumentSelection().size());
    }

    @Test
    public void givenThereIsDuplicationIn_thenRemoveItBeforeSubmit() {
        var list = new ArrayList<DynamicListItem>();

        var item = new DynamicListItem("Name", "Name");

        caseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .appeal(Appeal.builder().build())
                .sscsDocument(List.of())
                .otherPartySelection(List.of(
                        new CcdValue<>(new OtherPartySelectionDetails(new DynamicList(item, list))),
                        new CcdValue<>(new OtherPartySelectionDetails(new DynamicList(item, list)))
                        ))
                .genericLetterText(" ")
                .build();

        when(caseDetails.getCaseData()).thenReturn(caseData);

        var result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(1, result.getData().getOtherPartySelection().size());
    }

    @Test
    public  void givenThereAreNoDuplicatesInDocuments_thenReturnListIntact() {
        var list = new ArrayList<DynamicListItem>();

        var item1 = new DynamicListItem("DocumentName_1", "DocumentName_1");
        var item2 = new DynamicListItem("DocumentName_2", "DocumentName_2");

        caseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .appeal(Appeal.builder().build())
                .sscsDocument(List.of())
                .documentSelection(List.of(
                        new CcdValue<>(new DocumentSelectionDetails(new DynamicList(item1, list))),
                        new CcdValue<>(new DocumentSelectionDetails(new DynamicList(item2, list)))))
                .genericLetterText(" ")
                .build();

        when(caseDetails.getCaseData()).thenReturn(caseData);

        var result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertEquals(2, result.getData().getDocumentSelection().size());
        Assertions.assertEquals("DocumentName_1", result.getData().getDocumentSelection().get(0).getValue().getDocumentsList().getValue().getCode());
    }
}
