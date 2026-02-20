package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCodeOrThrowException;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitOptionalByCode;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.ADDRESS_NAME;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.APPEAL_REF;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.APPELLANT_NAME;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.BENEFIT_NAME_ACRONYM_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.FIRST_TIER_AGENCY_ACRONYM;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.HMCTS2;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.HMCTS_IMG;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.NAME;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.OTHER_PARTIES_NAMES;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.PHONE_NUMBER;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.SSCS_URL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.SSCS_URL_LITERAL;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.DWP_ACRONYM;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.HMRC_ACRONYM;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.IBCA_ACRONYM;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.SSCS5;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.SSCS8;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.LetterType.DOCMOSIS;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.getAddressPlaceholders;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Entity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;

@Service
public class HearingEnquiryFormPlaceholderService {

    private final PlaceholderService placeholderService;
    private final String helplineTelephone;
    private final String helplineTelephoneScotland;

    @Autowired
    public HearingEnquiryFormPlaceholderService(PlaceholderService placeholderService,
        @Value("${helpline.telephone}") String helplineTelephone,
        @Value("${helpline.telephoneScotland}") String helplineTelephoneScotland) {
        this.placeholderService = placeholderService;
        this.helplineTelephone = helplineTelephone;
        this.helplineTelephoneScotland = helplineTelephoneScotland;
    }

    public Map<String, Object> populatePlaceholders(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String partyId) {
        final Address address = PlaceholderUtility.getAddress(caseData, letterType, partyId);
        final Map<String, Object> placeholders = new HashMap<>(getAddressPlaceholders(address, true, DOCMOSIS));
        placeholders.put(BENEFIT_NAME_ACRONYM_LITERAL, getBenefitAcronym(caseData));
        String name = PlaceholderUtility.getName(caseData, OTHER_PARTY_REP_LETTER, partyId);
        placeholders.put(NAME, name);
        placeholders.put(ADDRESS_NAME, name);
        placeholders.put(SSCS_URL_LITERAL, SSCS_URL);
        placeholders.put(GENERATED_DATE_LITERAL, LocalDateTime.now().toLocalDate().toString());
        final String appellantName = Optional.ofNullable(caseData.getAppeal()).map(Appeal::getAppellant).map(Entity::getName)
            .map(Name::getFullNameNoTitle).orElse("");
        placeholders.put(APPELLANT_NAME, appellantName);
        placeholders.put(APPEAL_REF, getAppealReference(caseData));
        placeholders.put(FIRST_TIER_AGENCY_ACRONYM, getFirstTierAgencyAcronym(caseData));
        placeholders.put(PHONE_NUMBER,
            Objects.equals(caseData.getIsScottishCase(), "Yes") ? helplineTelephoneScotland : helplineTelephone);
        placeholders.put(OTHER_PARTIES_NAMES, getOtherPartyNames(caseData.getOtherParties()));
        placeholderService.buildExcelaAddress(false, caseData.getIsScottishCase(), placeholders);
        placeholders.put(HMCTS2, HMCTS_IMG);
        placeholders.put(CASE_ID_LITERAL, caseData.getCcdCaseId());

        return placeholders;
    }

    private static String getBenefitAcronym(SscsCaseData caseData) {
        Benefit benefit = getBenefitByCodeOrThrowException(caseData.getAppeal().getBenefitType().getCode());
        return benefit.isHasAcronym() ? benefit.getShortName() : benefit.getDescription();
    }

    private static String getFirstTierAgencyAcronym(SscsCaseData caseData) {
        Optional<Benefit> benefit = getBenefitOptionalByCode(caseData.getAppeal().getBenefitType().getCode());
        final String type = benefit.isPresent() ? String.valueOf(benefit.get().getSscsType()) : String.valueOf(
            caseData.getFormType());
        switch (type) {
            case SSCS5 -> {
                return HMRC_ACRONYM;
            }
            case SSCS8 -> {
                return IBCA_ACRONYM;
            }
            default -> {
                return DWP_ACRONYM;
            }
        }
    }

    private String getOtherPartyNames(List<CcdValue<OtherParty>> otherParties) {
        return Optional.ofNullable(otherParties).orElse(emptyList()).stream().map(CcdValue::getValue).map(OtherParty::getName)
            .filter(Objects::nonNull).map(Name::getFullNameNoTitle).collect(Collectors.joining(", "));
    }

    private String getAppealReference(SscsCaseData caseData) {
        final String caseReference = caseData.getCaseReference();
        return isBlank(caseReference) || (caseData.getCreatedInGapsFrom() != null && caseData.getCreatedInGapsFrom()
            .equals(READY_TO_LIST.getCcdType())) ? caseData.getCcdCaseId() : caseReference;
    }


}
