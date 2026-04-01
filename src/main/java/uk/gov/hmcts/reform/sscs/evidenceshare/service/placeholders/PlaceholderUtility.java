package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;

@Slf4j
public final class PlaceholderUtility {

    private static final String SIR_MADAM = "Sir/Madam";
    private static final String DWP = "DWP";

    private PlaceholderUtility() {
    }

    public static String defaultToEmptyStringIfNull(String value) {
        return (value == null) ? StringUtils.EMPTY : value;
    }

    public static String truncateAddressLine(String addressLine) {
        return addressLine != null && addressLine.length() > 45  ? addressLine.substring(0, 45) : addressLine;
    }

    public static Address getAddress(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String otherPartyId) {
        Address address;
        if (FurtherEvidenceLetterType.APPELLANT_LETTER.getValue().equals(letterType.getValue())) {
            address = getAppellantAddress(caseData);
        } else if (FurtherEvidenceLetterType.JOINT_PARTY_LETTER.getValue().equals(letterType.getValue())) {
            address = getJointPartyAddress(caseData);
        } else if (FurtherEvidenceLetterType.OTHER_PARTY_LETTER.getValue().equals(letterType.getValue()) || FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER.getValue().equals(letterType.getValue())) {
            address = getOtherPartyAddress(caseData, otherPartyId);
        } else {
            address = getRepsAddress(caseData);
        }
        if (!isValidLetterAddress(address)) {
            log.error("Sending letter with invalid address for case {}, letterType={}, otherPartyId={}. Address: {}",
                    caseData.getCcdCaseId(), letterType.getValue(), otherPartyId, address);
        }
        return address;
    }

    public static boolean isValidLetterAddress(Address address) {
        return address != null
                && isNoneBlank(address.getLine1())
                && isNoneBlank(address.getPostcode());
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
        if (otherPartyId != null && caseData.getOtherParties() != null) {
            for (CcdValue<OtherParty> otherParty : caseData.getOtherParties()) {
                if (matchesId(otherPartyId, otherParty.getValue().getId())) {
                    return otherParty.getValue().getAddress();
                } else if (otherParty.getValue().getAppointee() != null && matchesId(otherPartyId, otherParty.getValue().getAppointee().getId())) {
                    return otherParty.getValue().getAppointee().getAddress();
                } else if (otherParty.getValue().getRep() != null && matchesId(otherPartyId, otherParty.getValue().getRep().getId())) {
                    return otherParty.getValue().getRep().getAddress();
                }
            }
            log.error("No matching other party found for otherPartyId={} in case {}", otherPartyId, caseData.getCcdCaseId());
        }
        return getEmptyAddress();
    }

    private static boolean matchesId(String searchId, String entityId) {
        if (searchId == null || entityId == null) {
            return false;
        }
        return searchId.equals(entityId) || searchId.contains(entityId);
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
        String name;
        if (FurtherEvidenceLetterType.APPELLANT_LETTER.getValue().equals(letterType.getValue())) {
            name = extractNameAppellant(caseData);
        } else if (FurtherEvidenceLetterType.REPRESENTATIVE_LETTER.getValue().equals(letterType.getValue())) {
            name = extractNameRep(caseData.getAppeal().getRep());
        } else if (FurtherEvidenceLetterType.JOINT_PARTY_LETTER.getValue().equals(letterType.getValue())) {
            name = extractNameJointParty(caseData);
        } else if (FurtherEvidenceLetterType.OTHER_PARTY_LETTER.getValue().equals(letterType.getValue())
                || FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER.getValue().equals(letterType.getValue())) {
            name = getOtherPartyName(caseData, otherPartyId);
        } else if (FurtherEvidenceLetterType.DWP_LETTER.getValue().equals(letterType.getValue())) {
            return DWP;
        } else {
            return null;
        }
        if (SIR_MADAM.equals(name)) {
            log.error("Recipient name resolved to Sir/Madam for case {}, letterType={}, otherPartyId={}",
                    caseData.getCcdCaseId(), letterType.getValue(), otherPartyId);
        }
        return name;
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
        if (otherPartyId != null && caseData.getOtherParties() != null) {
            for (CcdValue<OtherParty> otherParty : caseData.getOtherParties()) {
                OtherParty otherPartyValue = otherParty.getValue();

                if (matchesId(otherPartyId, otherPartyValue.getId())) {
                    if (isValidName(otherPartyValue.getName())) {
                        return otherPartyValue.getName().getFullNameNoTitle();
                    }
                    log.error("Other party matched by id={} but name is invalid for case {}", otherPartyId, caseData.getCcdCaseId());
                    return SIR_MADAM;
                } else if (otherPartyValue.getAppointee() != null
                        && matchesId(otherPartyId, otherPartyValue.getAppointee().getId())) {
                    if (isValidName(otherPartyValue.getAppointee().getName())) {
                        return otherPartyValue.getAppointee().getName().getFullNameNoTitle();
                    }
                    log.error("Other party appointee matched by id={} but name is invalid for case {}", otherPartyId, caseData.getCcdCaseId());
                    return SIR_MADAM;
                } else if (otherPartyValue.getRep() != null
                        && matchesId(otherPartyId, otherPartyValue.getRep().getId())) {
                    return extractNameRep(otherPartyValue.getRep());
                }
            }
            log.error("No matching other party found for name resolution, otherPartyId={} in case {}", otherPartyId, caseData.getCcdCaseId());
        }
        return SIR_MADAM;
    }

    private static boolean isValidName(Name name) {
        return isNoneBlank(name.getFirstName()) && isNoneBlank(name.getLastName());
    }

    public static String getPostponementRequestStatus(SscsCaseData caseData) {
        return (caseData.getPostponementRequest() == null
                || caseData.getPostponementRequest().getActionPostponementRequestSelected() == null)
                ? "" : caseData.getPostponementRequest().getActionPostponementRequestSelected();
    }
}

