package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.NAME;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderUtility.truncateAddressLine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;

@Service
@Slf4j
public class FurtherEvidencePlaceholderService {

    private final PlaceholderService placeholderService;

    @Autowired
    public FurtherEvidencePlaceholderService(PlaceholderService placeholderService) {
        this.placeholderService = placeholderService;
    }

    public Map<String, Object> populatePlaceholders(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String otherPartyId) {
        requireNonNull(caseData, "caseData must not be null");

        Map<String, Object> placeholders = new ConcurrentHashMap<>();
        Address address = PlaceholderUtility.getAddress(caseData, letterType, otherPartyId);
        placeholderService.build(caseData, placeholders, address, null);
        String name = PlaceholderUtility.getName(caseData, letterType, otherPartyId);

        if (name != null) {
            placeholders.put(NAME, truncateAddressLine(name));
        }

        return placeholders;
    }
}
