package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.*;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;

@Service
@Slf4j
public class SorPlaceholderService {
    private final PlaceholderService placeholderService;

    @Value("${helpline.telephone}")
    private String helplineTelephone;

    @Autowired
    public SorPlaceholderService(PlaceholderService placeholderService) {
        this.placeholderService = placeholderService;
    }

    public Map<String, Object> populatePlaceholders(SscsCaseData caseData, FurtherEvidenceLetterType letterType,
                                                    String entityType, String otherPartyId) {
        requireNonNull(caseData, "caseData must not be null");

        Map<String, Object> placeholders = new HashMap<>();
        Address address = PlaceholderUtility.getAddress(caseData, letterType, otherPartyId);
        placeholderService.build(caseData, placeholders, address, null);

        String name = PlaceholderUtility.getName(caseData, letterType, otherPartyId);
        placeholders.putAll(PlaceholderUtility.getAddressPlaceHolders(address));

        if (name != null) {
            placeholders.put(NAME, name);
            placeholders.put(ADDRESS_NAME, name);
        }
        placeholders.put(HMCTS2, HMCTS_IMG);
        placeholders.put(APPEAL_REF, caseData.getCcdCaseId());
        placeholders.put(ENTITY_TYPE, entityType);
        placeholders.put(APPELLANT_NAME, caseData.getAppeal().getAppellant().getName().getFullNameNoTitle());

        Hearing latestHearing = caseData.getLatestHearing();
        if (!isNull(latestHearing) && !isNull(latestHearing.getValue())) {
            placeholders.put(HEARING_DATE, latestHearing.getValue().getHearingDateTime().toLocalDate().toString());
        }

        RegionalProcessingCenter rpc = caseData.getRegionalProcessingCenter();
        if (!isNull(rpc)) {
            placeholders.put(PHONE_NUMBER, determinePhoneNumber(rpc));
        }

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
