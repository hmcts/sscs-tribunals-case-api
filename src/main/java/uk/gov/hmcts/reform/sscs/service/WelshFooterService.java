package uk.gov.hmcts.reform.sscs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.pdf.PdfWatermarker;

@Component
@Slf4j
public class WelshFooterService extends AbstractFooterService<SscsWelshDocument> {

    @Autowired
    public WelshFooterService(PdfStoreService pdfStoreService, PdfWatermarker alter) {
        super(pdfStoreService, alter);
    }

}
