package uk.gov.hmcts.reform.sscs.util;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.equalsAnyIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;

public class DocumentUtil {

    private DocumentUtil() {
        //
    }

    public static boolean isFileAPdf(DocumentLink docLink) {
        return docLink != null
                && isNotBlank(docLink.getDocumentUrl())
                && equalsAnyIgnoreCase("pdf", getExtension(docLink.getDocumentFilename()));
    }
}
