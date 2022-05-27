package uk.gov.hmcts.reform.sscs.ccd.presubmit.postponementrequest;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.PostponementRequestService;

@Service
public class PostponementRequestAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PostponementRequestService postponementRequestService;

    @Autowired
    public PostponementRequestAboutToSubmitHandler(PostponementRequestService postponementRequestService) {
        this.postponementRequestService = postponementRequestService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        Objects.requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.POSTPONEMENT_REQUEST
                && callback.getCaseDetails() != null;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        final PreSubmitCallbackResponse<SscsCaseData> response = validatePostponementRequest(sscsCaseData);

        final Optional<Hearing> optionalHearing = emptyIfNull(sscsCaseData.getHearings()).stream()
                .filter(h -> h.getValue().getHearingDateTime().isAfter(LocalDateTime.now()))
                .distinct()
                .findFirst();

        optionalHearing.ifPresentOrElse(hearing ->
                        postponementRequestService.setHearingDateAsExcludeDate(hearing, sscsCaseData),
                () -> response.addError("There are no hearing to postpone"));

        if (response.getErrors().isEmpty()) {
            postponementRequestService.processPostponementRequest(sscsCaseData, UploadParty.DWP);
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
