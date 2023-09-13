package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

@Component
@Slf4j
public class CaseUpdatedAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.CASE_UPDATED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData caseData = caseDetails.getCaseData();

        Appeal appeal = getAppeal(caseData);
        BenefitType benefitType = appeal.getBenefitType();
        DynamicList benefitDescriptions = SscsUtil.getBenefitDescriptions();
        DynamicListItem selectedBenefit = getSelectedBenefit(benefitDescriptions.getListItems(), caseData.getBenefitCode());
        benefitDescriptions.setValue(selectedBenefit);
        benefitType.setDescriptionSelection(benefitDescriptions);

        log.info("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA {}", appeal);

        caseData.setAppeal(appeal);

        return new PreSubmitCallbackResponse<>(caseData);
    }

    private static DynamicListItem getSelectedBenefit(List<DynamicListItem> listItems, String benefitCode) {
        return listItems.stream().filter(item -> benefitCode.equals(item.getCode())).findFirst().orElse(null);
    }

    private Appeal getAppeal(SscsCaseData caseData) {
        Appeal appeal = caseData.getAppeal();
        if (isNull(appeal)) {
            BenefitType type = BenefitType.builder().build();
            appeal = Appeal.builder()
                    .benefitType(type)
                    .build();
            caseData.setAppeal(appeal);
        }
        return appeal;
    }
}
