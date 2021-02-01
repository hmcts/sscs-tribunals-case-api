package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;

import java.time.LocalDate;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
public class DwpDocumentService {

    public void addToDwpDocuments(SscsCaseData sscsCaseData, DwpResponseDocument dwpDocument, DwpDocumentType docType) {

        DwpDocumentDetails dwpDocumentDetails = new DwpDocumentDetails(docType.getValue(),
                docType.getLabel(),
                LocalDate.now().toString(),
                dwpDocument.getDocumentLink(), null, null, null);

        DwpDocument doc = new DwpDocument(dwpDocumentDetails);

        if (isNull(sscsCaseData.getDwpDocuments())) {
            sscsCaseData.setDwpDocuments(new ArrayList<>());
        }
        sscsCaseData.getDwpDocuments().add(doc);
        sscsCaseData.sortCollections();
    }
}
