package uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsDetailsMapping.getHearingType;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.ADDRESS_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.APPEAL_REF;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.APPELLANT_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.ENTITY_TYPE;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.HEARING_DATE;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.HMCTS2;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.HMCTS_IMG;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.HMC_HEARING_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.NAME;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.PHONE_NUMBER;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.POSTPONEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderUtility.getPostponementRequestStatus;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.LetterUtils.LetterType.DOCMOSIS;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.LetterUtils.getAddressPlaceholders;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.domain.FurtherEvidenceLetterType;

@Service
@Slf4j
public class SorPlaceholderService {
    private final PlaceholderService placeholderService;

    private final String helplineTelephone;

    private final String helplineTelephoneIbc;

    private final String helplineTelephoneScotland;

    @Autowired
    public SorPlaceholderService(
        PlaceholderService placeholderService,
        @Value("${helpline.telephone}")
        String helplineTelephone,
        @Value("${helpline.telephoneIbc}")
        String helplineTelephoneIbc,
        @Value("${helpline.telephoneScotland}")
        String helplineTelephoneScotland) {
        this.placeholderService = placeholderService;
        this.helplineTelephone = helplineTelephone;
        this.helplineTelephoneIbc = helplineTelephoneIbc;
        this.helplineTelephoneScotland = helplineTelephoneScotland;
    }

    public Map<String, Object> populatePlaceholders(SscsCaseData caseData, FurtherEvidenceLetterType letterType,
                                                    String entityType, String otherPartyId) {
        requireNonNull(caseData, "caseData must not be null");
        Address address = PlaceholderUtility.getAddress(caseData, letterType, otherPartyId);

        Map<String, Object> placeholders = new HashMap<>(getAddressPlaceholders(address, true, DOCMOSIS));

        placeholderService.build(caseData, placeholders, address, null);

        String name = PlaceholderUtility.getName(caseData, letterType, otherPartyId);

        if (name != null) {
            placeholders.put(NAME, name);
            placeholders.put(ADDRESS_NAME, name);
        }
        placeholders.put(HMCTS2, HMCTS_IMG);
        placeholders.put(APPEAL_REF, caseData.getCcdCaseId());
        placeholders.put(ENTITY_TYPE, entityType);
        placeholders.put(APPELLANT_NAME, caseData.getAppeal().getAppellant().getName().getFullNameNoTitle());
        placeholders.put(HMC_HEARING_TYPE_LITERAL, getHearingType(caseData).getHmcReference());

        Hearing latestHearing = caseData.getLatestHearing();
        if (!isNull(latestHearing) && !isNull(latestHearing.getValue())) {
            placeholders.put(HEARING_DATE, latestHearing.getValue().getHearingDateTime().toLocalDate().toString());
        }

        RegionalProcessingCenter rpc = caseData.getRegionalProcessingCenter();
        if (caseData.isIbcCase()) {
            placeholders.put(
                PHONE_NUMBER,
                Objects.equals(caseData.getIsScottishCase(), "Yes")
                    ? helplineTelephoneScotland
                    : helplineTelephoneIbc
            );
        } else {
            placeholders.put(PHONE_NUMBER, determinePhoneNumber(rpc));
        }

        placeholders.put(POSTPONEMENT_REQUEST, getPostponementRequestStatus(caseData));

        return placeholders;
    }

    private String determinePhoneNumber(RegionalProcessingCenter rpc) {
        if (rpc != null) {
            return rpc.getPhoneNumber();
        } else {
            return helplineTelephone;
        }
    }
}
