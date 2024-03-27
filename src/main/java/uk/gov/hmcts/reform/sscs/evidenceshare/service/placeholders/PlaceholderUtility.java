package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.*;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderService.lines;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;

@Slf4j
public final class PlaceholderUtility {

    private static final String SIR_MADAM = "Sir/Madam";
    private static final String DWP = "DWP";

    private PlaceholderUtility() {
    }

    static String defaultToEmptyStringIfNull(String value) {
        return (value == null) ? StringUtils.EMPTY : value;
    }

    static String truncateAddressLine(String addressLine) {
        return addressLine != null && addressLine.length() > 45  ? addressLine.substring(0, 45) : addressLine;
    }

    public static Address getAddress(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String otherPartyId) {
        if (FurtherEvidenceLetterType.APPELLANT_LETTER.getValue().equals(letterType.getValue())) {
            return getAppellantAddress(caseData);
        } else if (FurtherEvidenceLetterType.JOINT_PARTY_LETTER.getValue().equals(letterType.getValue())) {
            return getJointPartyAddress(caseData);
        } else if (FurtherEvidenceLetterType.OTHER_PARTY_LETTER.getValue().equals(letterType.getValue()) || FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER.getValue().equals(letterType.getValue())) {
            return getOtherPartyAddress(caseData, otherPartyId);
        }
        return getRepsAddress(caseData);
    }

    private static Address getRepsAddress(SscsCaseData caseData) {
        return Optional.of(caseData.getAppeal())
            .map(Appeal::getRep)
            .map(Representative::getAddress)
            .orElseGet(PlaceholderUtility::getEmptyAddress);
    }

    private static Address getAppellantAddress(SscsCaseData caseData) {
        return Optional.of(caseData.getAppeal())
            .map(Appeal::getAppellant)
            .filter(appellant -> "yes".equalsIgnoreCase(appellant.getIsAppointee()))
            .map(Appellant::getAppointee)
            .map(Appointee::getAddress)
            .orElseGet(() -> defaultAddress(caseData.getAppeal()));
    }

    private static Address getJointPartyAddress(SscsCaseData caseData) {
        return isYes(caseData.getJointParty().getJointPartyAddressSameAsAppellant()) ? getAppellantAddress(caseData)
            : ofNullable(caseData.getJointParty().getAddress()).orElse(getEmptyAddress());
    }

    private static Address getOtherPartyAddress(SscsCaseData caseData, String otherPartyId) {
        if (otherPartyId != null) {
            for (CcdValue<OtherParty> otherParty : caseData.getOtherParties()) {
                if (otherPartyId.contains(otherParty.getValue().getId())) {
                    return otherParty.getValue().getAddress();
                } else if (otherParty.getValue().getAppointee() != null && otherPartyId.contains(otherParty.getValue().getAppointee().getId())) {
                    return otherParty.getValue().getAppointee().getAddress();
                } else if (otherParty.getValue().getRep() != null && otherPartyId.contains(otherParty.getValue().getRep().getId())) {
                    return otherParty.getValue().getRep().getAddress();
                }
            }
        }
        return getEmptyAddress();
    }

    private static Address defaultAddress(Appeal appeal) {
        return Optional.of(appeal)
            .map(Appeal::getAppellant)
            .map(Appellant::getAddress)
            .orElseGet(PlaceholderUtility::getEmptyAddress);
    }

    private static Address getEmptyAddress() {
        log.error("Sending out letter with empty address");

        return Address.builder()
            .line1(StringUtils.EMPTY)
            .line2(StringUtils.EMPTY)
            .county(StringUtils.EMPTY)
            .postcode(StringUtils.EMPTY)
            .build();
    }

    public static String getName(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String otherPartyId) {
        if (FurtherEvidenceLetterType.APPELLANT_LETTER.getValue().equals(letterType.getValue())) {
            return extractNameAppellant(caseData);
        } else if (FurtherEvidenceLetterType.REPRESENTATIVE_LETTER.getValue().equals(letterType.getValue())) {
            return extractNameRep(caseData.getAppeal().getRep());
        } else if (FurtherEvidenceLetterType.JOINT_PARTY_LETTER.getValue().equals(letterType.getValue())) {
            return extractNameJointParty(caseData);
        } else if (FurtherEvidenceLetterType.OTHER_PARTY_LETTER.getValue().equals(letterType.getValue())
            || FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER.getValue().equals(letterType.getValue())) {
            return getOtherPartyName(caseData, otherPartyId);
        } else if (FurtherEvidenceLetterType.DWP_LETTER.getValue().equals(letterType.getValue())) {
            return DWP;
        }
        return null;
    }

    private static String extractNameAppellant(SscsCaseData caseData) {
        return Optional.of(caseData.getAppeal())
            .map(Appeal::getAppellant)
            .filter(appellant -> "yes".equalsIgnoreCase(appellant.getIsAppointee()))
            .map(Appellant::getAppointee)
            .map(Appointee::getName)
            .filter(PlaceholderUtility::isValidName)
            .map(Name::getFullNameNoTitle)
            .orElseGet(() -> Optional.of(caseData.getAppeal())
                .map(Appeal::getAppellant)
                .map(Appellant::getName)
                .filter(PlaceholderUtility::isValidName)
                .map(Name::getFullNameNoTitle)
                .orElse(SIR_MADAM));
    }

    private static String extractNameRep(Representative representative) {
        return Optional.of(representative)
            .map(Representative::getName)
            .filter(PlaceholderUtility::isValidName)
            .map(Name::getFullNameNoTitle)
            .orElseGet(() -> Optional.of(representative)
                .map(Representative::getOrganisation)
                .filter(StringUtils::isNoneBlank)
                .orElse(SIR_MADAM));
    }

    private static String extractNameJointParty(SscsCaseData caseData) {
        return ofNullable(caseData.getJointParty().getName())
            .filter(jpn -> isValidName(Name.builder().firstName(jpn.getFirstName()).lastName(jpn.getLastName()).build()))
            .map(Name::getFullNameNoTitle)
            .orElse(SIR_MADAM);
    }

    private static String getOtherPartyName(SscsCaseData caseData, String otherPartyId) {
        if (otherPartyId != null) {
            for (CcdValue<OtherParty> otherParty : caseData.getOtherParties()) {
                OtherParty otherPartyValue = otherParty.getValue();

                if (otherPartyId.contains(otherPartyValue.getId())
                    && isValidName(otherPartyValue.getName())) {
                    return otherPartyValue.getName().getFullNameNoTitle();
                } else if (otherPartyValue.getAppointee() != null
                    && otherPartyId.contains(otherPartyValue.getAppointee().getId())
                    && isValidName(otherPartyValue.getAppointee().getName())) {
                    return otherPartyValue.getAppointee().getName().getFullNameNoTitle();
                } else if (otherPartyValue.getRep() != null
                    && otherPartyId.contains(otherPartyValue.getRep().getId())
                    && isValidName(otherPartyValue.getRep().getName())) {
                    return extractNameRep(otherPartyValue.getRep());
                }
            }
        }
        return SIR_MADAM;
    }

    private static boolean isValidName(Name name) {
        return isNoneBlank(name.getFirstName()) && isNoneBlank(name.getLastName());
    }

    static Map<String, Object> getAddressPlaceHolders(Address address) {
        var addressPlaceHolders = new HashMap<String, Object>();
        String[] lines = lines(address);
        String[] addressConstants = {LETTER_ADDRESS_LINE_1, LETTER_ADDRESS_LINE_2, LETTER_ADDRESS_LINE_3,
            LETTER_ADDRESS_LINE_4, LETTER_ADDRESS_POSTCODE};

        for (int i = 0; i < lines.length; i++) {
            addressPlaceHolders.put(addressConstants[i], truncateAddressLine(defaultToEmptyStringIfNull(lines[i])));
        }

        return addressPlaceHolders;
    }
}
