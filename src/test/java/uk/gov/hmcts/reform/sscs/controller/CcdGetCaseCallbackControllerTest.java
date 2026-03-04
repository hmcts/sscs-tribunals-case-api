package uk.gov.hmcts.reform.sscs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.client.model.GetCaseCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.metadatafields.CaseViewField;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.GetCaseCallbackService;
import uk.gov.hmcts.reform.sscs.service.exceptions.ClientAuthorisationException;

@ExtendWith(MockitoExtension.class)
class CcdGetCaseCallbackControllerTest {

    @Mock
    private AuthorisationService authorisationService;
    @Mock
    private GetCaseCallbackService getCaseCallbackService;
    @InjectMocks
    private CcdGetCaseCallbackController controller;

    @Test
    void shouldReturnGetCaseResponseWhenRequestIsValid() {
        final GetCaseCallbackResponse response = new GetCaseCallbackResponse();
        final CaseViewField metadataField = new CaseViewField();
        metadataField.setId("[INJECTED_DATA.ENABLE_ADD_OTHER_PARTY_DATA]");
        metadataField.setValue("Yes");
        response.setMetadataFields(List.of(metadataField));
        final Callback<SscsCaseData> callback = buildCallback();
        when(getCaseCallbackService.buildResponse(callback)).thenReturn(response);

        final ResponseEntity<GetCaseCallbackResponse> result = controller.ccdGetCase("s2s-token", callback);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(authorisationService).authorise("s2s-token");
        verify(getCaseCallbackService).buildResponse(callback);
    }

    @ParameterizedTest
    @CsvSource({
        "false,true",
        "true,false",
        "false,false"
    })
    void shouldThrowWhenRequiredInputsAreMissing(final boolean includeServiceAuthHeader,
                                                 final boolean includeBody) {
        final String serviceAuthHeader = includeServiceAuthHeader ? "s2s-token" : null;
        final Callback<SscsCaseData> callback = includeBody ? buildCallback() : null;

        assertThatThrownBy(() -> controller.ccdGetCase(serviceAuthHeader, callback))
            .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(authorisationService);
        verifyNoInteractions(getCaseCallbackService);
    }

    @Test
    void shouldPropagateExceptionWhenAuthorisationServiceThrows() {
        final Callback<SscsCaseData> callback = buildCallback();
        final ClientAuthorisationException exception = new ClientAuthorisationException(new Exception("Unauthorised"));
        doThrow(exception).when(authorisationService).authorise("s2s-token");

        assertThatThrownBy(() -> controller.ccdGetCase("s2s-token", callback))
            .isInstanceOf(ClientAuthorisationException.class);

        verify(authorisationService).authorise("s2s-token");
        verifyNoInteractions(getCaseCallbackService);
    }

    private Callback<SscsCaseData> buildCallback() {
        final SscsCaseData caseData = SscsCaseData.builder().build();
        final CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(
            1234L,
            "SSCS",
            State.WITH_DWP,
            caseData,
            LocalDateTime.now(),
            "Benefit"
        );
        return new Callback<>(caseDetails, Optional.empty(), EventType.ACTION_FURTHER_EVIDENCE, false);
    }
}