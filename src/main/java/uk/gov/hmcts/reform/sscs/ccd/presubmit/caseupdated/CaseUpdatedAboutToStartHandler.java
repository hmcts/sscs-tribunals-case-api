package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class CaseUpdatedAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final DynamicListLanguageUtil utils;

    private final VerbalLanguagesService verbalLanguagesService;
  
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

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        String caseId = sscsCaseData.getCcdCaseId();
        Appeal appeal = sscsCaseData.getAppeal();
        HearingOptions hearingOptions = appeal.getHearingOptions();

        if (hearingOptions != null) {
            DynamicList interpreterLanguages = utils.generateInterpreterLanguageFields(null);
            String existingLanguage = hearingOptions.getLanguages();

            if (!StringUtils.isEmpty(existingLanguage)) {
                Language language = verbalLanguagesService.getVerbalLanguage(existingLanguage);
                if (null != language) {
                    DynamicListItem dynamicListItem = utils.getLanguageDynamicListItem(language);
                    interpreterLanguages.setValue(dynamicListItem);
                }
            }
            hearingOptions.setLanguagesList(interpreterLanguages);
            log.info("Populated {} Languages in DynamicList for caseId {} for update to case data event",
                    interpreterLanguages.getListItems().size(), caseId);
        }
        setupBenefitSelection(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private static void setupBenefitSelection(SscsCaseData sscsCaseData) {
        BenefitType benefitType = sscsCaseData.getAppeal().getBenefitType();

        if (!isNull(benefitType)) {
            DynamicList benefitDescriptions = SscsUtil.getBenefitDescriptions();
            DynamicListItem selectedBenefit = getSelectedBenefit(benefitDescriptions.getListItems(), sscsCaseData.getBenefitCode());
            benefitDescriptions.setValue(selectedBenefit);
            benefitType.setDescriptionSelection(benefitDescriptions);
        }
    }

    private static DynamicListItem getSelectedBenefit(List<DynamicListItem> listItems, String benefitCode) {
        if (isNull(benefitCode) || isNull(listItems)) {
            return null;
        }

        return listItems.stream().filter(item -> benefitCode.equals(item.getCode())).findFirst().orElse(null);
    }
}
