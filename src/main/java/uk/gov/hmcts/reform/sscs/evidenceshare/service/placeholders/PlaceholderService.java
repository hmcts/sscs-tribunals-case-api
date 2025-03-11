package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.APPELLANT_FULL_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.BENEFIT_NAME_ACRONYM_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.BENEFIT_NAME_ACRONYM_LITERAL_WELSH;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.BENEFIT_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.IBCA_REFERENCE_LABEL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.IBCA_URL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.LABEL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.NINO_LABEL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.NINO_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_COUNTY_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_FAX_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_PHONE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.SC_NUMBER_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.SHOULD_HIDE_NINO;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.SSCS_URL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.SSCS_URL_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.WELSH_CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.WELSH_GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.IBC_ACRONYM;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.IBC_ACRONYM_WELSH;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.LetterType.PLACEHOLDER_SERVICE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.getAddressPlaceholders;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.docmosis.config.PdfDocumentConfig;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.ExelaAddressConfig;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;

@Service
@Slf4j
public class PlaceholderService {

    private final PdfDocumentConfig pdfDocumentConfig;
    private final ExelaAddressConfig exelaAddressConfig;
    private final boolean scottishPoBoxEnabled;

    @Autowired
    public PlaceholderService(PdfDocumentConfig pdfDocumentConfig,
                              ExelaAddressConfig exelaAddressConfig,
                              @Value("${feature.scottish-po-box.enabled}") boolean scottishPoBoxEnabled) {
        this.pdfDocumentConfig = pdfDocumentConfig;
        this.exelaAddressConfig = exelaAddressConfig;
        this.scottishPoBoxEnabled = scottishPoBoxEnabled;
    }

    public void build(SscsCaseData caseData, Map<String, Object> placeholders, Address address, String caseCreatedDate) {
        Appeal appeal = caseData.getAppeal();
        Benefit benefit = Benefit.getBenefitOptionalByCode(appeal.getBenefitType().getCode()).orElse(null);
        String description = Optional.ofNullable(appeal.getBenefitType())
            .map(BenefitType::getDescription)
            .orElseGet(() -> (benefit != null) ? benefit.getDescription().toUpperCase() : StringUtils.EMPTY);

        String shouldHideNino = appeal.getBenefitType() != null && Benefit.CHILD_SUPPORT.getShortName().equals(appeal.getBenefitType().getCode()) ? YesNo.YES.getValue() : YesNo.NO.getValue();

        if (caseData.isIbcCase()) {
            placeholders.put(BENEFIT_NAME_ACRONYM_LITERAL, IBC_ACRONYM);
            placeholders.put(BENEFIT_NAME_ACRONYM_LITERAL_WELSH, IBC_ACRONYM_WELSH);
        } else if (benefit != null && benefit.isHasAcronym()) {
            placeholders.put(BENEFIT_NAME_ACRONYM_LITERAL, benefit.name());
            placeholders.put(BENEFIT_NAME_ACRONYM_LITERAL_WELSH, benefit.name());
        } else {
            placeholders.put(BENEFIT_NAME_ACRONYM_LITERAL, benefit.getDescription());
            placeholders.put(BENEFIT_NAME_ACRONYM_LITERAL_WELSH, benefit.getWelshDescription());
        }

        placeholders.put(SHOULD_HIDE_NINO, shouldHideNino);
        placeholders.put(BENEFIT_TYPE_LITERAL, description);
        placeholders.put(APPELLANT_FULL_NAME_LITERAL, appeal.getAppellant().getName().getAbbreviatedFullName());
        placeholders.put(CASE_ID_LITERAL, caseData.getCcdCaseId());
        String ninoLiteral = defaultToEmptyStringIfNull(caseData.isIbcCase() ? appeal.getAppellant().getIdentity().getIbcaReference() : appeal.getAppellant().getIdentity().getNino());
        placeholders.put(NINO_LITERAL, ninoLiteral);
        placeholders.put(LABEL, caseData.isIbcCase() ? IBCA_REFERENCE_LABEL : NINO_LABEL);
        placeholders.put(SSCS_URL_LITERAL, caseData.isIbcCase() ? IBCA_URL : SSCS_URL);
        placeholders.put(GENERATED_DATE_LITERAL, LocalDateTime.now().toLocalDate().toString());
        placeholders.put(pdfDocumentConfig.getHmctsImgKey(), pdfDocumentConfig.getHmctsImgVal());
        if (caseData.isLanguagePreferenceWelsh()) {
            if (caseCreatedDate != null) {
                placeholders.put(WELSH_CASE_CREATED_DATE_LITERAL,
                    LocalDateToWelshStringConverter.convert(caseCreatedDate));
            }
            placeholders.put(WELSH_GENERATED_DATE_LITERAL,
                LocalDateToWelshStringConverter.convert(LocalDateTime.now().toLocalDate()));
            placeholders.put(pdfDocumentConfig.getHmctsWelshImgKey(), pdfDocumentConfig.getHmctsWelshImgVal());
        }
        placeholders.put(SC_NUMBER_LITERAL, defaultToEmptyStringIfNull(caseData.getCaseReference()));

        if (caseCreatedDate != null) {
            placeholders.put(CASE_CREATED_DATE_LITERAL, caseCreatedDate);
        }

        buildExcelaAddress(caseData.getIsScottishCase(), placeholders);
        populateRpcPlaceHolders(caseData, placeholders);

        placeholders.putAll(getAddressPlaceholders(address, true, PLACEHOLDER_SERVICE));
    }

    public void buildExcelaAddress(String isScottish, Map<String, Object> placeholders) {
        placeholders.put(EXELA_ADDRESS_LINE1_LITERAL, exelaAddressConfig.getAddressLine1());
        placeholders.put(EXELA_ADDRESS_LINE3_LITERAL, exelaAddressConfig.getAddressLine3());

        if ("Yes".equalsIgnoreCase(isScottish) && scottishPoBoxEnabled) {
            placeholders.put(EXELA_ADDRESS_LINE2_LITERAL, exelaAddressConfig.getScottishAddressLine2());
            placeholders.put(EXELA_ADDRESS_POSTCODE_LITERAL, exelaAddressConfig.getScottishPostcode());
        } else {
            placeholders.put(EXELA_ADDRESS_LINE2_LITERAL, exelaAddressConfig.getAddressLine2());
            placeholders.put(EXELA_ADDRESS_POSTCODE_LITERAL, exelaAddressConfig.getAddressPostcode());
        }
    }

    private void populateRpcPlaceHolders(SscsCaseData caseData, Map<String, Object> placeholders) {
        if (hasRegionalProcessingCenter(caseData)) {
            RegionalProcessingCenter rpc = caseData.getRegionalProcessingCenter();
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL, defaultToEmptyStringIfNull(rpc.getAddress1()));
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL, defaultToEmptyStringIfNull(rpc.getAddress2()));
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL, defaultToEmptyStringIfNull(rpc.getAddress3()));
            placeholders.put(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL, defaultToEmptyStringIfNull(rpc.getAddress4()));
            placeholders.put(REGIONAL_OFFICE_COUNTY_LITERAL, defaultToEmptyStringIfNull(rpc.getCity()));
            placeholders.put(REGIONAL_OFFICE_PHONE_LITERAL, defaultToEmptyStringIfNull(rpc.getPhoneNumber()));
            placeholders.put(REGIONAL_OFFICE_FAX_LITERAL, defaultToEmptyStringIfNull(rpc.getFaxNumber()));
            placeholders.put(REGIONAL_OFFICE_POSTCODE_LITERAL, defaultToEmptyStringIfNull(rpc.getPostcode()));
        }
    }

    public boolean hasRegionalProcessingCenter(SscsCaseData ccdResponse) {
        return nonNull(ccdResponse.getRegionalProcessingCenter())
            && nonNull(ccdResponse.getRegionalProcessingCenter().getName());
    }
}
