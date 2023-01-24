package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuegenericletter;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_GENERIC_LETTER;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentSelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

public class IssueGenericLetterAboutToStartHandlerTest {

    private IssueGenericLetterAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String APPOINTEE_FIRST_NAME = "AppointeeFirstName";
    private static final String APPOINTEE_LAST_NAME = "AppointeeLastName";

    private static final String OTHER_PARTY_FIRST_NAME = "Ivan";
    private static final String OTHER_PARTY_LAST_NAME = "Ivanov";
    private static final String APPOINTEE_ID = "appointee_id";
    private static final String CASE_ID = "1111111111111111";


    @BeforeEach
    protected void setUp() {
        openMocks(this);
        handler = new IssueGenericLetterAboutToStartHandler();

        DwpDocument dwpDocument = DwpDocument.builder().value(DwpDocumentDetails.builder().documentFileName("DwpDocument").build()).build();
        SscsDocument sscsDocument = new SscsDocument(SscsDocumentDetails.builder().documentFileName("SscsDocument").build());

        Appointee appointee = Appointee.builder().name(getName("Mr", APPOINTEE_FIRST_NAME, APPOINTEE_LAST_NAME)).id(APPOINTEE_ID).build();
        Name name = getName("Mr", OTHER_PARTY_FIRST_NAME, OTHER_PARTY_LAST_NAME);
        OtherParty otherParty = OtherParty.builder().isAppointee(YesNo.YES.getValue()).appointee(appointee).name(name).id(APPOINTEE_ID).build();
        List<CcdValue<OtherParty>> otherParties = List.of(new CcdValue<>(otherParty));

        caseData = SscsCaseData.builder()
                .ccdCaseId(CASE_ID)
                .appeal(Appeal.builder().build())
                .sscsDocument(List.of())
                .otherParties(otherParties)
                .dwpDocuments(List.of(dwpDocument))
                .sscsDocument(List.of(sscsDocument))
                .genericLetterText("testtest")
                .build();
    }

    private Name getName(String title, String firstName, String lastName) {
        return Name.builder().title(title).firstName(firstName).lastName(lastName).build();
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void givenANonCreateBundleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        Assertions.assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(ISSUE_GENERIC_LETTER);
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isTrue();
    }

    @Test
    void givenNonEmptyLetterText_thenItShouldBeEmpty() {
        when(callback.getEvent()).thenReturn(ISSUE_GENERIC_LETTER);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(isEmpty(result.getData().getGenericLetterText())).isTrue();
    }

    @Test
    void givenOtherPartyHasAppointee_thenShouldReturnAppointee() {
        when(callback.getEvent()).thenReturn(ISSUE_GENERIC_LETTER);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        List<CcdValue<OtherPartySelectionDetails>> otherPartySelection = result.getData().getOtherPartySelection();
        Assertions.assertEquals(1, otherPartySelection.size());

        List<DynamicListItem> listItems = otherPartySelection.get(0).getValue().getOtherPartiesList().getListItems();
        Assertions.assertEquals(1, listItems.size());
        assertThat(listItems.get(0).getCode()).contains(APPOINTEE_ID);
        assertThat(listItems.get(0).getLabel())
                .contains(APPOINTEE_LAST_NAME, APPOINTEE_FIRST_NAME, OTHER_PARTY_FIRST_NAME, OTHER_PARTY_LAST_NAME);
    }

    @Test
    void givenDocumentsAreExist_thenShouldReturnDocumentsList() {
        when(callback.getEvent()).thenReturn(ISSUE_GENERIC_LETTER);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        List<CcdValue<DocumentSelectionDetails>> documentSelection = result.getData().getDocumentSelection();
        Assertions.assertEquals(1, documentSelection.size());
        Assertions.assertEquals(2, documentSelection.get(0).getValue().getDocumentsList().getListItems().size());
    }
}
