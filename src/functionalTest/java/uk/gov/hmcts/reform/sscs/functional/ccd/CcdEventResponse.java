package uk.gov.hmcts.reform.sscs.functional.ccd;

import java.util.LinkedHashSet;
import java.util.Set;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

class CcdEventResponse {
    private SscsCaseData data;
    private Set<String> errors = new LinkedHashSet<>();
    private Set<String> warnings = new LinkedHashSet<>();

    private CcdEventResponse() {
        // noop -- for deserializer
    }

    public SscsCaseData getData() {
        return data;
    }

    public Set<String> getErrors() {
        return errors;
    }

    public Set<String> getWarnings() {
        return warnings;
    }
}
