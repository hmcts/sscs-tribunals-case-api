package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.caseupdated;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class CaseUpdatedAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DynamicListLanguageUtil utils;

    private final VerbalLanguagesService verbalLanguagesService;
    private final SignLanguagesService signLanguagesService;

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
        Appeal appeal = sscsCaseData.getAppeal();
        HearingOptions hearingOptions = appeal.getHearingOptions();

        if (hearingOptions != null) {
            DynamicList interpreterLanguages = utils.generateInterpreterLanguageFields(null);

            String existingLanguage = hearingOptions.getLanguages();

            if (!StringUtils.isEmpty(existingLanguage)) {
                Language language = verbalLanguagesService.getVerbalLanguage(existingLanguage);
                language = isNull(language) ? signLanguagesService.getSignLanguage(existingLanguage) : language;
                if (null != language) {
                    DynamicListItem dynamicListItem = utils.getLanguageDynamicListItem(language);
                    interpreterLanguages.setValue(dynamicListItem);
                }
            }

            hearingOptions.setLanguagesList(interpreterLanguages);
        }
        setupBenefitSelection(sscsCaseData);
        if (sscsCaseData.isIbcCase()) {
            setupUkPortsOfEntry(sscsCaseData);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setupBenefitSelection(SscsCaseData sscsCaseData) {
        BenefitType benefitType = sscsCaseData.getAppeal().getBenefitType();

        if (!isNull(benefitType)) {
            DynamicList benefitDescriptions = SscsUtil.getBenefitDescriptions();
            DynamicListItem selectedBenefit = getSelectedDynamicListItem(benefitDescriptions.getListItems(), sscsCaseData.getBenefitCode());
            benefitDescriptions.setValue(selectedBenefit);
            benefitType.setDescriptionSelection(benefitDescriptions);
        }
    }

    private void setupUkPortsOfEntry(SscsCaseData sscsCaseData) {
        DynamicList portOfEntryDynamicList = sscsCaseData.getAppeal().getAppellant().getAddress().getUkPortOfEntryList();
        if (portOfEntryDynamicList == null || portOfEntryDynamicList.getValue() == null) {
            final DynamicList ukPortOfEntries = SscsUtil.getPortsOfEntry();
            String portOfEntryCode = sscsCaseData.getAppeal().getAppellant().getAddress().getPortOfEntry();

            if (isNotEmpty(portOfEntryCode)) {
                DynamicListItem selectedPortOfEntry = getSelectedDynamicListItem(ukPortOfEntries.getListItems(), portOfEntryCode);
                ukPortOfEntries.setValue(selectedPortOfEntry);
            }

            sscsCaseData.getAppeal().getAppellant().getAddress().setUkPortOfEntryList(ukPortOfEntries);
        }
    }

    private DynamicListItem getSelectedDynamicListItem(List<DynamicListItem> listItems, String code) {
        if (isNull(code) || isNull(listItems)) {
            return null;
        }

        return listItems.stream().filter(item -> code.equals(item.getCode())).findFirst().orElse(null);
    }
}
