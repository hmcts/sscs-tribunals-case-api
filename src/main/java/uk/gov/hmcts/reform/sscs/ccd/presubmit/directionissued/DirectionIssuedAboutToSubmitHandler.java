package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import java.time.LocalDate;
import java.util.*;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsInterlocDirectionDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class DirectionIssuedAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
                && callback.getEvent() == EventType.DIRECTION_ISSUED
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData())
                && callback.getCaseDetails().getCaseData().isGenerateNotice()
                && (Objects.nonNull(callback.getCaseDetails().getCaseData().getPreviewDocument())
                    || Objects.nonNull(callback.getCaseDetails().getCaseData().getSscsInterlocDirectionDocument()));
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if (Objects.nonNull(callback.getCaseDetails().getCaseData().getPreviewDocument())) {
            SscsInterlocDirectionDocument document = SscsInterlocDirectionDocument.builder()
                    .documentFileName(caseData.getPreviewDocument().getDocumentFilename())
                    .documentLink(caseData.getPreviewDocument())
                    .documentDateAdded(Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now()))
                    .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                    .build();

            caseData.setSscsInterlocDirectionDocument(document);
            saveToHistory(caseData);
        } else {
            saveToHistory(caseData);
        }
        clearTransientFields(caseData);

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);


        return sscsCaseDataPreSubmitCallbackResponse;
    }

    private void saveToHistory(SscsCaseData caseData) {
        List<SscsInterlocDirectionDocument> historicDocs = new ArrayList<>(Optional.ofNullable(caseData.getHistoricSscsInterlocDirectionDocs()).orElse(Collections.emptyList()));
        historicDocs.add(caseData.getSscsInterlocDirectionDocument());
        caseData.setHistoricSscsInterlocDirectionDocs(historicDocs);
    }

    // Fields used for a short period in case progression are transient,
    // relevant for a short period of the case lifecycle.
    private void clearTransientFields(SscsCaseData caseData) {
        caseData.setPreviewDocument(null);
        caseData.setSignedBy(null);
        caseData.setSignedRole(null);
        caseData.setGenerateNotice(null);
        caseData.setDateAdded(null);
    }
}
