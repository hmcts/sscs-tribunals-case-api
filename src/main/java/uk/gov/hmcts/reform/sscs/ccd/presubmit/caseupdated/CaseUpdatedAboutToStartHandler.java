package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isOtherPartyPresent;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
public class CaseUpdatedAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private static final List<Benefit> VALID_CONFIDENTIALITY_BENEFITS =
        List.of(Benefit.CHILD_SUPPORT, Benefit.TAX_CREDIT,
            Benefit.GUARDIANS_ALLOWANCE, Benefit.TAX_FREE_CHILDCARE,
            Benefit.HOME_RESPONSIBILITIES_PROTECTION, Benefit.CHILD_BENEFIT,
            Benefit.THIRTY_HOURS_FREE_CHILDCARE, Benefit.GUARANTEED_MINIMUM_PENSION,
            Benefit.NATIONAL_INSURANCE_CREDITS);

    private final DynamicListLanguageUtil utils;

    private final VerbalLanguagesService verbalLanguagesService;
    private final SignLanguagesService signLanguagesService;

    private final boolean cmOtherPartyConfidentialityEnabled;

    CaseUpdatedAboutToStartHandler(DynamicListLanguageUtil utils,
                                   VerbalLanguagesService verbalLanguagesService,
                                   SignLanguagesService signLanguagesService,
                                   @Value("${feature.cm-other-party-confidentiality.enabled}")
                                   boolean cmOtherPartyConfidentialityEnabled) {
        this.utils = utils;
        this.verbalLanguagesService = verbalLanguagesService;
        this.signLanguagesService = signLanguagesService;
        this.cmOtherPartyConfidentialityEnabled = cmOtherPartyConfidentialityEnabled;
    }

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

        sscsCaseData.getAppeal().setShowConfidentialityOption(showAppellantConfidentialityOption(sscsCaseData));

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private YesNo showAppellantConfidentialityOption(SscsCaseData sscsCaseData) {
        var benefitCode = sscsCaseData.getAppeal().getBenefitType().getCode();

        var isAValidBenefit = VALID_CONFIDENTIALITY_BENEFITS.stream()
            .anyMatch(b -> equalsIgnoreCase(b.getShortName(), benefitCode));

        if (isAValidBenefit) {
            return YES;
        } else if (cmOtherPartyConfidentialityEnabled && equalsIgnoreCase(Benefit.UC.getShortName(), benefitCode)) {
            return isOtherPartyPresent(sscsCaseData) ? YES : NO;
        }

        return NO;
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
