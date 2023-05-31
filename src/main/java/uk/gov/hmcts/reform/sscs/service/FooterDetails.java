package uk.gov.hmcts.reform.sscs.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;

@Getter
@AllArgsConstructor
public class FooterDetails {
    private final DocumentLink url;
    private final String bundleAddition;
    private final String bundleFileName;
}
