package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.APPELLANT_FULL_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.BENEFIT_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.GENERATED_DATE_LITERAL;
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
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.SSCS_URL_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.WELSH_CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.WELSH_GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.LetterType.PLACEHOLDER_SERVICE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.getAddressPlaceholders;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
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
        String description = appeal.getBenefitType() != null ? appeal.getBenefitType().getDescription() : null;

        if (description == null && appeal.getBenefitType() != null && appeal.getBenefitType().getCode() != null) {
            description = Benefit.getBenefitOptionalByCode(appeal.getBenefitType().getCode()).map(Benefit::getDescription).orElse(StringUtils.EMPTY);
        }
        String shouldHideNino = appeal.getBenefitType() != null && Benefit.CHILD_SUPPORT.getShortName().equals(appeal.getBenefitType().getCode()) ? YesNo.YES.getValue() : YesNo.NO.getValue();

        if (description != null) {
            description = description.toUpperCase();
        } else {
            description = StringUtils.EMPTY;
        }

        placeholders.put(SHOULD_HIDE_NINO, shouldHideNino);
        placeholders.put(BENEFIT_TYPE_LITERAL, description);
        placeholders.put(APPELLANT_FULL_NAME_LITERAL, appeal.getAppellant().getName().getAbbreviatedFullName());
        placeholders.put(CASE_ID_LITERAL, caseData.getCcdCaseId());
        placeholders.put(NINO_LITERAL, defaultToEmptyStringIfNull(appeal.getAppellant().getIdentity().getNino()));
        placeholders.put(SSCS_URL_LITERAL, PlaceholderConstants.SSCS_URL);
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
