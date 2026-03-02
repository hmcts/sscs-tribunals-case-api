package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.GetCaseCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.metadatafields.CaseViewField;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.metadataprovider.MetadataFieldProvider;

@ExtendWith(MockitoExtension.class)
class GetCaseCallbackServiceTest {

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private MetadataFieldProvider providerA;

    @Mock
    private MetadataFieldProvider providerB;

    @Test
    void shouldReturnResponseWithNoMetadataFieldsWhenNoProviders() {
        final GetCaseCallbackService service = new GetCaseCallbackService(List.of());

        final GetCaseCallbackResponse response = service.buildResponse(callback);

        assertThat(response).isNotNull();
        assertThat(response.getMetadataFields()).isNull();
    }

    @Test
    void shouldNotSetMetadataFieldsWhenAllProvidersReturnEmpty() {
        when(providerA.provide(callback)).thenReturn(Optional.empty());
        when(providerB.provide(callback)).thenReturn(Optional.empty());
        final GetCaseCallbackService service = new GetCaseCallbackService(List.of(providerA, providerB));

        final GetCaseCallbackResponse response = service.buildResponse(callback);

        assertThat(response.getMetadataFields()).isNull();
    }

    @Test
    void shouldSetMetadataFieldsWhenProvidersReturnFields() {
        final CaseViewField fieldA = new CaseViewField();
        final CaseViewField fieldB = new CaseViewField();
        when(providerA.provide(callback)).thenReturn(Optional.of(fieldA));
        when(providerB.provide(callback)).thenReturn(Optional.of(fieldB));
        final GetCaseCallbackService service = new GetCaseCallbackService(List.of(providerA, providerB));

        final GetCaseCallbackResponse response = service.buildResponse(callback);

        assertThat(response.getMetadataFields()).containsExactly(fieldA, fieldB);
    }

    @Test
    void shouldExcludeEmptyProviderFromFieldsWhenOneProviderReturnsEmpty() {
        final CaseViewField fieldA = new CaseViewField();
        when(providerA.provide(callback)).thenReturn(Optional.of(fieldA));
        when(providerB.provide(callback)).thenReturn(Optional.empty());
        final GetCaseCallbackService service = new GetCaseCallbackService(List.of(providerA, providerB));

        final GetCaseCallbackResponse response = service.buildResponse(callback);

        assertThat(response.getMetadataFields()).containsExactly(fieldA);
    }
}