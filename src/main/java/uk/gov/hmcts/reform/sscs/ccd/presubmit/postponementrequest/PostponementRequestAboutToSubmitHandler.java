package uk.gov.hmcts.reform.sscs.ccd.presubmit.postponementrequest;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.DWP;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.PostponementRequestService;

@Service
public class PostponementRequestAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PostponementRequestService postponementRequestService;
    private final FooterService footerService;
    private final IdamService idamService;

    @Autowired
    public PostponementRequestAboutToSubmitHandler(PostponementRequestService postponementRequestService, FooterService footerService, IdamService idamService) {
        this.postponementRequestService = postponementRequestService;
        this.footerService = footerService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.POSTPONEMENT_REQUEST
                && callback.getCaseDetails() != null;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        final PreSubmitCallbackResponse<SscsCaseData> response = validatePostponementRequest(sscsCaseData);

        if (response.getErrors().isEmpty()) {

            UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
            Optional<UploadParty> uploadParty = Optional.empty();

            if (userDetails.hasRole(DWP)) {
                uploadParty = Optional.of(UploadParty.DWP);
            }

            postponementRequestService.processPostponementRequest(sscsCaseData, UploadParty.DWP, uploadParty);
            List<SscsDocument> documents = sscsCaseData.getSscsDocument();
            documents.get(documents.size() - 1).getValue().setBundleAddition(footerService.getNextBundleAddition(documents));
        }

        return response;
    }

    @NotNull
    private PreSubmitCallbackResponse<SscsCaseData> validatePostponementRequest(SscsCaseData sscsCaseData) {
        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (sscsCaseData.getPostponementRequest().getPostponementPreviewDocument() == null) {
            response.addError("There is no postponement request document");
        }
        return response;
    }

}
