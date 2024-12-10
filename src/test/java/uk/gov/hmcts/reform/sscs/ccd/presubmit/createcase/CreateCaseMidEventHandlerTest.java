package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase.CreateCaseMidEventHandler.IBCA_REFERENCE_EMPTY_ERROR;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase.CreateCaseMidEventHandler.IBCA_REFERENCE_VALIDATION_ERROR;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
public class CreateCaseMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String IBCA_LABEL = "Infected Blood Compensation / 093";
    private static final String CHILD_SUPPORT_LABEL = "Child Support / 022";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @InjectMocks
    private CreateCaseMidEventHandler midEventHandler;

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {
        "VALID_APPEAL_CREATED",
        "NON_COMPLIANT",
        "INCOMPLETE_APPLICATION_RECEIVED",
        "CASE_UPDATED"
    })
    void canHandleTest(EventType eventType) {
        SscsCaseData caseData = SscsCaseData.builder()
            .benefitCode(IBCA_BENEFIT_CODE)
            .build();

        when(callback.getEvent()).thenReturn(eventType);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        assertTrue(midEventHandler.canHandle(MID_EVENT, callback));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {
        "VALID_APPEAL_CREATED",
        "NON_COMPLIANT",
        "INCOMPLETE_APPLICATION_RECEIVED",
        "CASE_UPDATED"
    })
    void canHandleTest_caseCreateBenefitSelectionFromList(EventType eventType) {
        DynamicList expectedList = new DynamicList(
            new DynamicListItem(IBCA_BENEFIT_CODE, IBCA_LABEL), new ArrayList<>());

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().descriptionSelection(expectedList).build())
                .build())
            .build();

        when(callback.getEvent()).thenReturn(eventType);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        assertTrue(midEventHandler.canHandle(MID_EVENT, callback));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {
        "VALID_APPEAL_CREATED",
        "NON_COMPLIANT",
        "INCOMPLETE_APPLICATION_RECEIVED",
        "CASE_UPDATED"
    })
    void cannotHandleTest(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(midEventHandler.canHandle(MID_EVENT, callback));
    }

    @Test
    void cannotHandleTest_caseCreateBenefitSelectionFromList() {
        DynamicList expectedList = new DynamicList(
            new DynamicListItem(CHILD_SUPPORT.getBenefitCode(), CHILD_SUPPORT_LABEL), new ArrayList<>());

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().descriptionSelection(expectedList).build())
                .build())
            .build();

        when(callback.getEvent()).thenReturn(EventType.INCOMPLETE_APPLICATION_RECEIVED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        assertFalse(midEventHandler.canHandle(MID_EVENT, callback));
    }

    @Test
    void shouldReturnErrorsOnMidEventErrorsForUkIbcaCase() {
        SscsCaseData caseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .address(Address.builder()
                                        .inMainlandUk(YES)
                                        .build())
                                .build()
                        )
                        .rep(Representative.builder()
                                .hasRepresentative("Yes")
                                .address(Address.builder().build())
                                .build()
                        )
                        .build()
                )
            .regionalProcessingCenter(RegionalProcessingCenter.builder().hearingRoute(HearingRoute.GAPS).build())
                .benefitCode(IBCA_BENEFIT_CODE)
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(3);
        assertThat(response.getErrors()).contains("You must enter address line 1 for the appellant");
        assertThat(response.getErrors()).contains("You must enter a valid UK postcode for the appellant");
        assertThat(response.getErrors()).contains("You must enter Living in the UK for the representative");
    }

    @Test
    void shouldReturnErrorsOnMidEventForCaseUpdateErrorsForUkIbcaCase() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .address(Address.builder()
                        .inMainlandUk(YES)
                        .build())
                    .build()
                )
                .rep(Representative.builder()
                    .hasRepresentative("Yes")
                    .address(Address.builder().build())
                    .build()
                )
                .build()
            )
            .regionalProcessingCenter(RegionalProcessingCenter.builder().hearingRoute(HearingRoute.GAPS).build())
            .benefitCode(IBCA_BENEFIT_CODE)
            .build();

        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(4);
        assertThat(response.getErrors()).contains("You must enter address line 1 for the appellant");
        assertThat(response.getErrors()).contains("You must enter a valid UK postcode for the appellant");
        assertThat(response.getErrors()).contains("You must enter Living in the UK for the representative");
        assertThat(response.getErrors()).contains("Hearing route must be List Assist");
    }

    @Test
    void shouldReturnErrorsOnMidEventErrorsForOverseasIbcaCase() {
        SscsCaseData caseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .address(Address.builder()
                                        .ukPortOfEntryList(
                                                new DynamicList(
                                                        new DynamicListItem("GB000434", "Aberdeen"),
                                                        new ArrayList<>()
                                                )
                                        )
                                        .inMainlandUk(NO)
                                        .build())
                                .build())
                        .rep(Representative.builder()
                                .hasRepresentative("Yes")
                                .address(Address.builder().build())
                                .build()
                        )
                        .build()
                )
                .regionalProcessingCenter(RegionalProcessingCenter.builder().hearingRoute(HearingRoute.LIST_ASSIST).build())
                .benefitCode(IBCA_BENEFIT_CODE)
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(3);
        assertThat(response.getErrors()).contains("You must enter address line 1 for the appellant");
        assertThat(response.getErrors()).contains("You must enter a valid country for the appellant");
        assertThat(response.getErrors()).contains("You must enter Living in the UK for the representative");
        assertThat(response.getWarnings()).contains(IBCA_REFERENCE_EMPTY_ERROR);
    }

    @Test
    void shouldReturnWarningsOnMidEventForInvalidIbcaReference() {
        SscsCaseData caseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .address(Address.builder()
                                        .line1("Test line 1")
                                        .country("Panama")
                                        .ukPortOfEntryList(
                                                new DynamicList(
                                                        new DynamicListItem("GB000434", "Aberdeen"),
                                                        new ArrayList<>()
                                                )
                                        )
                                        .inMainlandUk(NO)
                                        .build()
                                )
                                .identity(Identity.builder().ibcaReference("IBCA1234").build())
                                .build()
                        )
                        .rep(Representative.builder().build())
                        .build()
                )
                .benefitCode(IBCA_BENEFIT_CODE)
                .build();


        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).contains(IBCA_REFERENCE_VALIDATION_ERROR);
    }

    @Test
    void shouldNotReturnErrorsOnMidEventForIbcaCase() {
        SscsCaseData caseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .address(Address.builder()
                                        .line1("Test line 1")
                                        .country("Panama")
                                        .ukPortOfEntryList(
                                                new DynamicList(
                                                        new DynamicListItem("GB000434", "Aberdeen"),
                                                        new ArrayList<>()
                                                )
                                        )
                                        .inMainlandUk(NO)
                                        .build()
                                )
                                .identity(Identity.builder().ibcaReference("E24A45").build())
                                .build()
                        )
                        .rep(Representative.builder().build())
                        .build()
                )
                .benefitCode(IBCA_BENEFIT_CODE)
                .build();


        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrors()).isEmpty();
    }
}
