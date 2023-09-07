package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import lombok.extern.slf4j.Slf4j;
import org.overviewproject.mime_types.App;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

import java.util.Arrays;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;


@Component
@Slf4j
public class CreateCaseAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && ((callback.getEvent() == EventType.VALID_APPEAL_CREATED
                || callback.getEvent() == EventType.DRAFT_TO_VALID_APPEAL_CREATED
                || callback.getEvent() == EventType.NON_COMPLIANT
                || callback.getEvent() == EventType.DRAFT_TO_NON_COMPLIANT
                || callback.getEvent() == EventType.INCOMPLETE_APPLICATION_RECEIVED
                || callback.getEvent() == EventType.DRAFT_TO_INCOMPLETE_APPLICATION)
                || callback.getEvent() == EventType.CREATE_APPEAL_PDF);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData caseData = caseDetails.getCaseData();
        log.info("Handling create about to start event for case [" + caseData.getCcdCaseId() + "]");

        Appeal appeal = caseData.getAppeal();
        if (isNull(appeal)) {
            BenefitType type = BenefitType.builder().build();
            appeal = Appeal.builder()
                    .benefitType(type)
                    .build();
            caseData.setAppeal(appeal);
        }
        appeal.getBenefitType().setDescriptionSelection(getBenefitDescriptions());

        return new PreSubmitCallbackResponse<>(caseData);
    }

    private DynamicList getBenefitDescriptions() {
        List<DynamicListItem> items = Arrays.stream(Benefit.values())
                .map(benefit -> new DynamicListItem(benefit.getBenefitCode(), benefit.getDescription()))
                .toList();

        return new DynamicList(null, items);
    }
}
