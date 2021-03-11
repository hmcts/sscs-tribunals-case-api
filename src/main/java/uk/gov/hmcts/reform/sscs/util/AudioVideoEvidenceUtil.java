package uk.gov.hmcts.reform.sscs.util;

import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class AudioVideoEvidenceUtil {

    private AudioVideoEvidenceUtil() {
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

    public static boolean isSelectedEvidence(AudioVideoEvidence evidence, SscsCaseData caseData) {
        return evidence.getValue().getDocumentLink().getDocumentUrl().equals(caseData.getSelectedAudioVideoEvidence().getValue().getCode());
    }
}
