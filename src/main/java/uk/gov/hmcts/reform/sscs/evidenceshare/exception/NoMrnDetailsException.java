package uk.gov.hmcts.reform.sscs.evidenceshare.exception;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class NoMrnDetailsException extends RuntimeException {
    public static final long serialVersionUID = -3863598202357106145L;

    public NoMrnDetailsException(SscsCaseData caseData) {
        super("There is no Appeal Mrn details, for caseId %s.".formatted(caseData.getCcdCaseId()));
    }
}
