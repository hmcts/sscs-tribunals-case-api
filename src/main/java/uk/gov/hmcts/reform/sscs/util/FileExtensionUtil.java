package uk.gov.hmcts.reform.sscs.util;

import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidenceDetails;

public class FileExtensionUtil {

    private FileExtensionUtil() {
        //
    }

    public static DocumentType getDocumentType(AudioVideoEvidenceDetails evidence) {
        if (evidence.getDocumentLink().getDocumentFilename().toLowerCase().contains("mp3")) {
            return DocumentType.AUDIO_DOCUMENT;
        } else if (evidence.getDocumentLink().getDocumentFilename().toLowerCase().contains("mp4")) {
            return DocumentType.VIDEO_DOCUMENT;
        }
        return null;
    }
}
