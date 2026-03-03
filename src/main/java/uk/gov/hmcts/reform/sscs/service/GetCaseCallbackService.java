package uk.gov.hmcts.reform.sscs.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.GetCaseCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.metadatafields.CaseViewField;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.metadataprovider.MetadataFieldProvider;

@Service
public class GetCaseCallbackService {

    private final List<MetadataFieldProvider> metadataFieldProviders;

    public GetCaseCallbackService(final List<MetadataFieldProvider> metadataFieldProviders) {
        this.metadataFieldProviders = metadataFieldProviders;
    }

    public GetCaseCallbackResponse buildResponse(final Callback<SscsCaseData> callback) {
        final GetCaseCallbackResponse response = new GetCaseCallbackResponse();
        final List<CaseViewField> fields = metadataFieldProviders.stream().map(provider -> provider.provide(callback))
            .filter(Optional::isPresent).map(Optional::get).toList();
        response.setMetadataFields(fields);
        return response;
    }
}
