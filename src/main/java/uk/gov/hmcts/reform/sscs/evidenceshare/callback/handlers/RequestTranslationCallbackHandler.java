package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CASE_UPDATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.REQUEST_TRANSLATION_FROM_WLU;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.WelshException;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.RequestTranslationService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import java.util.List;

@Slf4j
@Service
public class RequestTranslationCallbackHandler implements CallbackHandler<SscsCaseData> {

    private final RequestTranslationService requestTranslationService;
    private final DispatchPriority dispatchPriority;
    private final UpdateCcdCaseService updateCcdCaseService;
    private final IdamService idamService;

    @Autowired
    public RequestTranslationCallbackHandler(RequestTranslationService requestTranslationService,
                                             UpdateCcdCaseService updateCcdCaseService,
                                             IdamService idamService) {
        this.requestTranslationService = requestTranslationService;
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
        this.dispatchPriority = DispatchPriority.EARLIEST;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && (callback.getEvent() == REQUEST_TRANSLATION_FROM_WLU);
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!callback.getCaseDetails().getCaseData().isLanguagePreferenceWelsh()) {
            throw new IllegalStateException("Error: This action is only available for Welsh cases");
        }

        log.info("Processing wlu translation for case id {} in evidence share service",
            callback.getCaseDetails().getId());

        try {
            log.info("sending email for case  id {}", callback.getCaseDetails().getId());
            if (requestTranslationService.sendCaseToWlu(callback.getCaseDetails()) && callback.getEvent() == REQUEST_TRANSLATION_FROM_WLU) {
                updateCcdCaseService.updateCaseV2(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId()),
                    CASE_UPDATED.getCcdType(), "Case translations sent to wlu", "Updated case with date sent to wlu",
                    idamService.getIdamTokens(), sscsCaseDetails -> {
                        updateDocumentStatus(sscsCaseDetails.getData().getSscsDocument());
                        updateDocumentStatus(sscsCaseDetails.getData().getDwpDocuments());
                    });
            }
        } catch (WelshException e) {
            log.error("Error when sending to request translation from wlu: {}", callback.getCaseDetails().getId(), e);
        }
    }

    private void updateDocumentStatus(List<? extends AbstractDocument> docs) {
        ListUtils.emptyIfNull(docs).stream().filter(doc -> SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(doc.getValue().getDocumentTranslationStatus()))
                .forEach(doc -> {
                    doc.getValue().setDocumentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED);
                });
    }

    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
