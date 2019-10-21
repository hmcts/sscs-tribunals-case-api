package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class DirectionIssuedAboutToSubmitHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
                && callback.getEvent() == EventType.DIRECTION_ISSUED
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if (Objects.nonNull(callback.getCaseDetails().getCaseData().getPreviewDocument())) {
            SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(caseData.getPreviewDocument().getDocumentFilename())
                .documentLink(caseData.getPreviewDocument())
                .documentDateAdded(Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now()).format(DateTimeFormatter.ISO_DATE))
                .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                .build())
                .build();

            List<SscsDocument> documents = new ArrayList<>();
            if (caseData.getSscsDocument() != null) {
                documents.addAll(caseData.getSscsDocument());
            }
            documents.add(document);
            caseData.setSscsDocument(documents);
        }
        clearTransientFields(caseData);

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        log.info("Saved the new interloc direction document for case id: " + caseData.getCcdCaseId());

        return sscsCaseDataPreSubmitCallbackResponse;
    }
}
