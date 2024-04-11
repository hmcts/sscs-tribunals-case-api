package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.*;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderUtility.defaultToEmptyStringIfNull;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderUtility.truncateAddressLine;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
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
        buildRecipientAddressPlaceholders(address, placeholders);
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

    private void buildRecipientAddressPlaceholders(Address address, Map<String, Object> placeholders) {
        String[] lines = lines(address);

        if (lines.length >= 1) {
            placeholders.put(RECIPIENT_ADDRESS_LINE_1_LITERAL, truncateAddressLine(defaultToEmptyStringIfNull(lines[0])));
        }
        if (lines.length >= 2) {
            placeholders.put(RECIPIENT_ADDRESS_LINE_2_LITERAL, truncateAddressLine(defaultToEmptyStringIfNull(lines[1])));
        }
        if (lines.length >= 3) {
            placeholders.put(RECIPIENT_ADDRESS_LINE_3_LITERAL, truncateAddressLine(defaultToEmptyStringIfNull(lines[2])));
        }
        if (lines.length >= 4) {
            placeholders.put(RECIPIENT_ADDRESS_LINE_4_LITERAL, truncateAddressLine(defaultToEmptyStringIfNull(lines[3])));
        }
        if (lines.length >= 5) {
            placeholders.put(RECIPIENT_ADDRESS_LINE_5_LITERAL, truncateAddressLine(defaultToEmptyStringIfNull(lines[4])));
        }
    }

    public static String[] lines(Address address) {
        return Stream.of(address.getLine1(), address.getLine2(), address.getTown(), address.getCounty(), address.getPostcode())
            .filter(Objects::nonNull)
            .toArray(String[]::new);
    }

}
