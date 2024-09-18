package uk.gov.hmcts.reform.sscs.helper;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.ObjectUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.domain.validation.ValidationStatus.*;
import static uk.gov.hmcts.reform.sscs.service.CaseCodeService.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.CaseUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.FormType;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.domain.CaseEvent;
import uk.gov.hmcts.reform.sscs.domain.validation.ValidationStatus;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

@Component
@Slf4j
public class SscsDataHelper {

    private static final String CASE_MANAGEMENT_CATEGORY = "caseManagementCategory";

    private final CaseEvent caseEvent;
    private final AirLookupService airLookupService;
    private final DwpAddressLookupService dwpAddressLookupService;
    private final boolean caseAccessManagementFeature;

    public SscsDataHelper(CaseEvent caseEvent,
                          AirLookupService airLookupService,
                          DwpAddressLookupService dwpAddressLookupService,
                          @Value("${feature.case-access-management.enabled}")  boolean caseAccessManagementFeature) {
        this.caseEvent = caseEvent;
        this.airLookupService = airLookupService;
        this.dwpAddressLookupService = dwpAddressLookupService;
        this.caseAccessManagementFeature = caseAccessManagementFeature;
    }

    public void addSscsDataToMap(Map<String, Object> appealData, Appeal appeal, List<SscsDocument> sscsDocuments, Subscriptions subscriptions,
                                 FormType formType, String childMaintenanceNumber,
                                 List<CcdValue<OtherParty>> otherParties) {
        appealData.put("appeal", appeal);
        appealData.put("sscsDocument", sscsDocuments);
        appealData.put("evidencePresent", hasEvidence(sscsDocuments));
        appealData.put("subscriptions", subscriptions);
        appealData.put("formType", formType);
        log.info("Adding data for the a transformation");

        if (appeal != null) {
            if (appeal.getBenefitType() != null && isNotBlank(appeal.getBenefitType().getCode())) {
                String benefitCode = null;
                String addressName = null;
                if (appeal.getMrnDetails() != null) {
                    addressName = appeal.getMrnDetails().getDwpIssuingOffice();
                }
                benefitCode = generateBenefitCode(appeal.getBenefitType().getCode(), addressName).orElse(EMPTY);

                String issueCode = generateIssueCode();

                appealData.put("benefitCode", benefitCode);
                appealData.put("issueCode", issueCode);
                appealData.put("caseCode", generateCaseCode(benefitCode, issueCode));

                String dwpRegionCentre = setDwpRegionalCenter(appeal);
                if (dwpRegionCentre != null) {
                    appealData.put("dwpRegionalCentre", dwpRegionCentre);
                }

                setCaseAccessManagementCategories(appeal, appealData);
            } else {
                setCaseManagementCategory(formType, appealData);
            }

            setCaseAccessManagementNames(appeal, appealData, formType);

            appealData.put("createdInGapsFrom", READY_TO_LIST.getId());
            checkConfidentiality(formType,appealData, appeal);
        }

        if (FormType.SSCS2.equals(formType)) {
            appealData.put("childMaintenanceNumber", childMaintenanceNumber);
            if (otherParties != null) {
                appealData.put("otherParties", otherParties);
            }
        }
    }

    private void setCaseManagementCategory(FormType formType, Map<String, Object> appealData) {
        if (caseAccessManagementFeature && formType != null) {
            DynamicListItem caseManagementCategory = new DynamicListItem(
                formType.equals(FormType.SSCS5) ? "sscs5Unknown" : "sscs12Unknown",
                formType.equals(FormType.SSCS5) ? "SSCS5 Unknown" : "SSCS1/2 Unknown");
            appealData.put(CASE_MANAGEMENT_CATEGORY, new DynamicList(
                caseManagementCategory,
                List.of(caseManagementCategory)));
        }
    }

    private void setCaseAccessManagementNames(Appeal appeal, Map<String, Object> appealData, FormType formType) {
        if (caseAccessManagementFeature) {
            if (appeal.getAppellant() != null
                && appeal.getAppellant().getName() != null
                && appeal.getAppellant().getName().getFirstName() != null
                && appeal.getAppellant().getName().getLastName() != null) {
                Name name = appeal.getAppellant().getName();
                appealData.put("caseNameHmctsInternal", name.getFullNameNoTitle());
                appealData.put("caseNameHmctsRestricted", name.getFullNameNoTitle());
                appealData.put("caseNamePublic", name.getFullNameNoTitle());
            }

            if (appeal.getBenefitType() != null) {
                Optional<Benefit> benefit = Benefit.getBenefitOptionalByCode(appeal.getBenefitType().getCode());
                if (isHmrcBenefit(benefit, formType)) {
                    appealData.put("ogdType", "HMRC");
                } else {
                    appealData.put("ogdType", "DWP");
                }
            }
        }
    }

    private boolean isHmrcBenefit(Optional<Benefit> benefit, FormType formType) {
        if (benefit.isEmpty()) {
            return FormType.SSCS5.equals(formType);
        }
        return SscsType.SSCS5.equals(benefit.get().getSscsType());
    }

    private void setCaseAccessManagementCategories(Appeal appeal, Map<String, Object> appealData) {
        if (caseAccessManagementFeature) {
            Optional<Benefit> benefit = Benefit.getBenefitOptionalByCode(appeal.getBenefitType().getCode());
            if (benefit.isPresent()) {
                appealData.put("CaseAccessCategory", CaseUtils.toCamelCase(benefit.get().getDescription(), false, ' '));
                DynamicListItem caseManagementCategory = new DynamicListItem(benefit.get().getShortName(), benefit.get().getDescription());
                List<DynamicListItem> listItems = List.of(caseManagementCategory);
                appealData.put(CASE_MANAGEMENT_CATEGORY, new DynamicList(caseManagementCategory, listItems));
            }
        }
    }

    private void checkConfidentiality(FormType formType, Map<String, Object> appealData, Appeal appeal) {
        if ((FormType.SSCS2.equals(formType) || FormType.SSCS5.equals(formType)) && appeal.getAppellant() != null && YesNo.isYes(appeal.getAppellant().getConfidentialityRequired())) {
            appealData.put("isConfidentialCase", YesNo.YES.getValue());
        }
    }

    private String setDwpRegionalCenter(Appeal appeal) {
        String dwpRegionCentre = null;
        if (appeal.getMrnDetails() != null && appeal.getMrnDetails().getDwpIssuingOffice() != null) {
            dwpRegionCentre = dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice(
                appeal.getBenefitType().getCode(),
                appeal.getMrnDetails().getDwpIssuingOffice());
            log.info("DwpHandling office set as " + dwpRegionCentre);
        } else if (appeal.getMrnDetails() == null || appeal.getMrnDetails().getDwpIssuingOffice() == null) {
            Optional<OfficeMapping> defaultOfficeMapping = dwpAddressLookupService.getDefaultDwpMappingByBenefitType(appeal.getBenefitType().getCode());
            if (defaultOfficeMapping.isPresent()) {
                String defaultDwpIssuingOffice = defaultOfficeMapping.get().getMapping().getCcd();
                dwpRegionCentre = dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice(
                    appeal.getBenefitType().getCode(),
                    defaultDwpIssuingOffice);
                log.info("Default dwpHandling office set as " + dwpRegionCentre);
            }
        }
        return dwpRegionCentre;
    }

    public String findEventToCreateCase(CaseResponse caseValidationResponse) {
        LocalDate mrnDate = findMrnDateTime(((Appeal) caseValidationResponse.getTransformedCase()
            .get("appeal")).getMrnDetails());

        if (!isEmpty(caseValidationResponse.getWarnings())) {
            return caseEvent.getIncompleteApplicationEventId();
        } else if (mrnDate != null && mrnDate.plusMonths(13L).isBefore(LocalDate.now())) {
            return caseEvent.getNonCompliantEventId();
        } else {
            return caseEvent.getValidAppealCreatedEventId();
        }
    }

    private LocalDate findMrnDateTime(MrnDetails mrnDetails) {
        if (mrnDetails != null && mrnDetails.getMrnDate() != null) {
            return LocalDate.parse(mrnDetails.getMrnDate());
        }
        return null;
    }

    public String hasEvidence(List<SscsDocument> sscsDocuments) {
        return (null == sscsDocuments || sscsDocuments.isEmpty()) ? "No" : "Yes";
    }

    public static ValidationStatus getValidationStatus(List<String> errors, List<String> warnings) {
        if (isNotEmpty(errors)) {
            return ERRORS;
        }
        if (isNotEmpty(warnings)) {
            return WARNINGS;
        }
        return SUCCESS;
    }

    public String findProcessingVenue(String postcode, BenefitType benefitType) {
        if (isNotBlank(postcode)
            && nonNull(benefitType)
            && isNotBlank(benefitType.getCode())) {
            return airLookupService.lookupAirVenueNameByPostCode(postcode, benefitType);
        }
        return null;
    }

}
