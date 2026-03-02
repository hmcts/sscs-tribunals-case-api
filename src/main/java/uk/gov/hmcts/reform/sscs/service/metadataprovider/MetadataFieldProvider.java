package uk.gov.hmcts.reform.sscs.service.metadataprovider;

import java.util.Optional;
import uk.gov.hmcts.reform.ccd.client.model.metadatafields.CaseViewField;
import uk.gov.hmcts.reform.ccd.client.model.metadatafields.definition.FieldTypeDefinition;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

public interface MetadataFieldProvider {

    Optional<CaseViewField> provide(Callback<SscsCaseData> callback);

    static CaseViewField createTextField(final String fieldId, final YesNo value) {
        final CaseViewField field = new CaseViewField();
        field.setId(fieldId);
        field.setValue(value);
        field.setMetadata(true);
        final FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setId("Text");
        fieldTypeDefinition.setType("Label");
        field.setFieldTypeDefinition(fieldTypeDefinition);
        return field;
    }
}
