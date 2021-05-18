package uk.gov.hmcts.reform.sscs.ccd.presubmit.playbackaudiovideo;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.AUDIO_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.VIDEO_DOCUMENT;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public final class PlaybackAudioVideoActionHelper {

    private PlaybackAudioVideoActionHelper() {
        //not called
    }

    static List<AbstractDocument> combineAvLists(List<? extends AbstractDocument> sscsAvDocuments, List<? extends AbstractDocument> dwpAvDocuments) {
        List<AbstractDocument> combinedAvDocuments = new ArrayList<>(sscsAvDocuments);
        combinedAvDocuments.addAll(dwpAvDocuments);
        return combinedAvDocuments;
    }

    static void setSelectedAudioVideoEvidence(SscsCaseData sscsCaseData, List<? extends AbstractDocument> avDocuments) {
        List<DynamicListItem> listOptionsSscs = populateEvidenceListWithItems(avDocuments);

        if (listOptionsSscs.size() > 0) {
            sscsCaseData.setSelectedAudioVideoEvidence(new DynamicList(listOptionsSscs.get(0), listOptionsSscs));
        }
    }

    static List<DynamicListItem> populateEvidenceListWithItems(List<? extends AbstractDocument> avDocuments) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        if (avDocuments != null) {
            avDocuments.forEach(audioVideoEvidence -> {
                if (audioVideoEvidence.getValue() != null && audioVideoEvidence.getValue().getDocumentLink() != null) {
                    listOptions.add(new DynamicListItem(audioVideoEvidence.getValue().getDocumentLink().getDocumentUrl(),
                            audioVideoEvidence.getValue().getDocumentFileName()));
                }
            });
        }

        return listOptions;
    }

    static List<? extends AbstractDocument> getApprovedAudioVideoDocuments(List<? extends AbstractDocument> documentDetails) {
        if (documentDetails != null) {
            return documentDetails.stream().filter(e ->
                    e.getValue().getDocumentType().equals(AUDIO_DOCUMENT.getValue())
                            || e.getValue().getDocumentType().equals(VIDEO_DOCUMENT.getValue()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
