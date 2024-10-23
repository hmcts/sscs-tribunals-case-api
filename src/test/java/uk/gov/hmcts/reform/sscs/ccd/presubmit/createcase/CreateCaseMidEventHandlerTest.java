package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@ExtendWith(MockitoExtension.class)
public class CreateCaseMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

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
    void cannotHandleTest(EventType eventType) {
        SscsCaseData caseData = SscsCaseData.builder().build();

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
    void canHandleTest(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
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
    }

    @Test
    void shouldNotReturnErrorsOnMidEventForNonIbcaCase() {
        SscsCaseData caseData = SscsCaseData.builder().benefitCode("091").build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }
}
