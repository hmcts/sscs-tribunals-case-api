package uk.gov.hmcts.reform.sscs.ccd.presubmit.playbackaudiovideo;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.playbackaudiovideo.PlaybackAudioVideoActionHelper.*;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class PlaybackAudioVideoEvidenceMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private String dmGatewayUrl;
    private String documentManagementUrl;

    @Autowired
    public PlaybackAudioVideoEvidenceMidEventHandler(@Value("${dm_gateway.url}") String dmGatewayUrl,
                                                     @Value("${document_management.url}") String documentManagementUrl) {
        this.dmGatewayUrl = dmGatewayUrl;
        this.documentManagementUrl = documentManagementUrl;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
                && callback.getEvent() == EventType.PLAYBACK_AUDIO_VIDEO_EVIDENCE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();


        List<? extends AbstractDocument> sscsAvDocuments = getApprovedAudioVideoDocuments(sscsCaseData.getSscsDocument());
        List<? extends AbstractDocument> dwpAvDocuments = getApprovedAudioVideoDocuments(sscsCaseData.getDwpDocuments());

        List<AbstractDocument> combinedAvDocuments = new ArrayList<>(sscsAvDocuments);
        combinedAvDocuments.addAll(dwpAvDocuments);

        setSelectedDropdown(sscsCaseData, combinedAvDocuments);

        AbstractDocument selectedAudioVideoEvidence = combinedAvDocuments.stream().filter(evidence -> isSelectedApprovedEvidence(evidence, sscsCaseData)).findFirst().orElse(null);

        if (nonNull(selectedAudioVideoEvidence)) {

            setSelectedAudioVideoEvidenceLink(sscsCaseData, selectedAudioVideoEvidence.getValue());

            sscsCaseData.setSelectedAudioVideoEvidenceDetails(
                    AudioVideoEvidenceDetails.builder()
                    .documentType(DocumentType.fromValue(selectedAudioVideoEvidence.getValue().getDocumentType()).getLabel())
                    .dateAdded(LocalDate.parse(selectedAudioVideoEvidence.getValue().getDocumentDateAdded()))
                    .partyUploaded(selectedAudioVideoEvidence.getValue().getPartyUploaded())
                    .build());
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private static boolean isSelectedApprovedEvidence(AbstractDocument evidence, SscsCaseData caseData) {
        return evidence.getValue().getDocumentLink().getDocumentUrl().equals(caseData.getSelectedAudioVideoEvidence().getValue().getCode());
    }

    private void setSelectedAudioVideoEvidenceLink(SscsCaseData caseData, AbstractDocumentDetails selected) {
        String binaryDocUrl = selected.getDocumentLink().getDocumentBinaryUrl().replace(documentManagementUrl, dmGatewayUrl);
        String tempDocumentLink = "<a target=\"_blank\" href=\"" + binaryDocUrl + "\">" + selected.getDocumentLink().getDocumentFilename() + "</a>";
        caseData.setTempMediaUrl(tempDocumentLink);
    }

    private void setSelectedDropdown(SscsCaseData caseData, List<AbstractDocument> combinedAvDocuments) {
        final DynamicList evidenceDL = caseData.getSelectedAudioVideoEvidence();

        if (nonNull(evidenceDL) && nonNull(evidenceDL.getValue())) {
            List<DynamicListItem> evidenceList = populateEvidenceListWithItems(combinedAvDocuments);
            DynamicList updatedEvidences = new DynamicList(evidenceDL.getValue(), evidenceList);
            caseData.setSelectedAudioVideoEvidence(updatedEvidences);
        } else {
            setSelectedAudioVideoEvidence(caseData, combinedAvDocuments);
        }
    }
}
