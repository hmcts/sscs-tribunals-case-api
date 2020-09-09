package uk.gov.hmcts.reform.sscs.service;

import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;

public class FooterDetails {

    private final DocumentLink url;
    private final String bundleAddition;
    private String bundleFileName;

    public FooterDetails(DocumentLink url, String bundleAddition, String bundleFileName) {
        this.url = url;
        this.bundleAddition = bundleAddition;
        this.bundleFileName = bundleFileName;
    }


    public DocumentLink getUrl() {
        return url;
    }

    public String getBundleAddition() {
        return bundleAddition;
    }

    public String getBundleFileName() {
        return bundleFileName;
    }



}
