package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
public class CreateCaseAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    private final CreateCaseAboutToStartHandler handler = new CreateCaseAboutToStartHandler();

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @ParameterizedTest
    @CsvSource({
        "VALID_APPEAL_CREATED",
        "NON_COMPLIANT",
        "INCOMPLETE_APPLICATION_RECEIVED"
    })
    void givenACreateHandlerEventForSyaCases_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        Assertions.assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @ParameterizedTest
    @EnumSource(names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonValidCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertThrows(IllegalStateException.class, () -> handler.handle(callbackType, callback, USER_AUTHORISATION));
    }

    @Test
    void whenEventStart_shouldReturnNonEmptyDescriptionSelection() {
        BenefitType type = BenefitType.builder().build();
        Appeal initialAppeal = Appeal.builder().benefitType(type).build();
        SscsCaseData caseData = SscsCaseData.builder().appeal(initialAppeal).build();
        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Appeal appeal = result.getData().getAppeal();
        BenefitType benefitType = appeal.getBenefitType();
        Appellant appellant = appeal.getAppellant();
        Address address = appellant.getAddress();
        DynamicList ukPortOfEntryList = address.getUkPortOfEntryList();
        DynamicList descriptionSelection = benefitType.getDescriptionSelection();
        assertThat(appeal).isNotNull();
        assertThat(benefitType).isNotNull();
        assertThat(appellant).isNotNull();
        assertThat(address).isNotNull();
        assertThat(ukPortOfEntryList).isNotNull();
        assertThat(descriptionSelection).isNotNull();
        assertThat(descriptionSelection.getListItems()).isNotEmpty();
    }

    @Test
    void whenCorrectEventAndCallbackType_shouldNotHaveErrors() {
        SscsCaseData caseData = SscsCaseData.builder().build();
        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(result.getErrors()).isEmpty();
    }
}
