package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@ExtendWith(MockitoExtension.class)
public class CreateCaseAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    private CreateCaseAboutToStartHandler handler;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    void setUp() {
        handler = new CreateCaseAboutToStartHandler();
    }

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
        DynamicList descriptionSelection = benefitType.getDescriptionSelection();
        assertThat(appeal).isNotNull();
        assertThat(benefitType).isNotNull();
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
