package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${feature.infected-blood-compensation.enabled}")
    private boolean isInfectedBloodCompensationEnabled;

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

            log.info("Existing language {}", existingLanguage);

            if (!StringUtils.isEmpty(existingLanguage)) {
                Language language = verbalLanguagesService.getVerbalLanguage(existingLanguage);

                if (null != language) {
                    log.info("Verbal language dialect {}, dialect reference {} ", language.getDialectEn(), language.getDialectReference());
                    log.info("Verbal language full reference {}, mrd reference {} ", language.getReference(), language.getMrdReference());

                    DynamicListItem dynamicListItem = utils.getLanguageDynamicListItem(language);

                    log.info("Dynamic List item code {} , label {} ", dynamicListItem.getCode(), dynamicListItem.getLabel());

                    interpreterLanguages.setValue(dynamicListItem);
                }
            }

            interpreterLanguages.getListItems().forEach(li -> log.info("interpreter language list item code {}, list item label {}", li.getCode(), li.getLabel()));
            hearingOptions.setLanguagesList(interpreterLanguages);
            log.info("Populated {} Languages in DynamicList for caseId {} for update to case data event",
                    interpreterLanguages.getListItems().size(), caseId);
        }
        setupBenefitSelection(sscsCaseData);
        setupUkPortsOfEntry(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setupBenefitSelection(SscsCaseData sscsCaseData) {
        BenefitType benefitType = sscsCaseData.getAppeal().getBenefitType();

        if (!isNull(benefitType)) {
            DynamicList benefitDescriptions = SscsUtil.getBenefitDescriptions(isInfectedBloodCompensationEnabled);
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
