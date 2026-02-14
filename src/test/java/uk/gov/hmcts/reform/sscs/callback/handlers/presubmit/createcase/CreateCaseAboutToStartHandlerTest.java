package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.createcase;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.createcase.CreateCaseAboutToStartHandler;
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

    private CaseDetails<SscsCaseData> caseDetails;
    private Callback<SscsCaseData> callback;

    private CreateCaseAboutToStartHandler handler;

    @BeforeEach
    public void setUp() {
        var caseData = SscsCaseData.builder().build();
        caseDetails = new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_APPEAL_CREATED, false);
        handler = new CreateCaseAboutToStartHandler();
    }

    @ParameterizedTest
    @CsvSource({
        "VALID_APPEAL_CREATED",
        "NON_COMPLIANT",
        "INCOMPLETE_APPLICATION_RECEIVED"
    })
    void givenACreateHandlerEventForSyaCases_thenReturnTrue(EventType eventType) {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), eventType, false);

        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @ParameterizedTest
    @EnumSource(names = {"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    void givenANonValidCallbackType_thenReturnFalse(CallbackType callbackType) {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_APPEAL_CREATED, false);

        assertThrows(IllegalStateException.class, () -> handler.handle(callbackType, callback, USER_AUTHORISATION));
    }

    @Test
    void whenEventStart_shouldReturnNonEmptyDescriptionSelection() {
        BenefitType type = BenefitType.builder().build();
        Appeal initialAppeal = Appeal.builder().benefitType(type).build();
        SscsCaseData caseData = SscsCaseData.builder().appeal(initialAppeal).build();
        caseDetails = new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_APPEAL_CREATED, false);

        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        Appeal appeal = result.getData().getAppeal();
        BenefitType benefitType = appeal.getBenefitType();
        Appellant appellant = appeal.getAppellant();
        assertNotNull(appeal);
        assertNotNull(benefitType);
        assertNotNull(appellant);
        Address address = appellant.getAddress();
        assertNotNull(address);
        DynamicList ukPortOfEntryList = address.getUkPortOfEntryList();
        DynamicList descriptionSelection = benefitType.getDescriptionSelection();
        assertNotNull(ukPortOfEntryList);
        assertNotNull(descriptionSelection);
        assertThat(descriptionSelection.getListItems()).isNotEmpty();
    }

    @Test
    void whenCorrectEventAndCallbackType_shouldNotHaveErrors() {
        SscsCaseData caseData = SscsCaseData.builder().build();
        caseDetails = new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_APPEAL_CREATED, false);

        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(result.getErrors()).isEmpty();
    }
}
