package uk.gov.hmcts.reform.sscs.util;

import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isOtherPartyPresent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;


public class ReissueUtils {

    private ReissueUtils() {
    }

    public static void validateSelectedPartyOptions(SscsCaseData sscsCaseData, ArrayList<String> errors, boolean checkOtherParty) {
        boolean caseHasARepresentative = StringUtils.equalsIgnoreCase("YES", Optional.ofNullable(sscsCaseData.getAppeal().getRep()).map(Representative::getHasRepresentative).orElse("No"));

        if (!isAnyPartySelectedToResend(sscsCaseData, checkOtherParty)) {
            errors.add("Select a party to reissue.");
        }
        if (!caseHasARepresentative && YesNo.isYes(sscsCaseData.getReissueArtifactUi().getResendToRepresentative())) {
            errors.add("Cannot re-issue to the representative as there is no representative on the appeal.");
        }
    }

    private static boolean isAnyPartySelectedToResend(SscsCaseData sscsCaseData,  boolean checkOtherParty) {
        ReissueArtifactUi reissueArtifactUi = sscsCaseData.getReissueArtifactUi();
        return YesNo.isYes(reissueArtifactUi.getResendToAppellant())
                || YesNo.isYes(reissueArtifactUi.getResendToRepresentative())
                || (checkOtherParty && isOtherPartyPresent(sscsCaseData) && isAnyOtherPartySelected(sscsCaseData));
    }

    private static boolean isAnyOtherPartySelected(SscsCaseData sscsCaseData) {
        ReissueArtifactUi reissueArtifactUi = sscsCaseData.getReissueArtifactUi();
        return reissueArtifactUi.getOtherPartyOptions() != null
                && !reissueArtifactUi.getOtherPartyOptions().isEmpty()
                && reissueArtifactUi.getOtherPartyOptions().stream()
                .anyMatch(otherPartyOption -> otherPartyOption.getValue() != null
                        && YesNo.isYes(otherPartyOption.getValue().getResendToOtherParty()));
    }

    public static void setUpOtherPartyOptions(SscsCaseData sscsCaseData) {
        sscsCaseData.getReissueArtifactUi().setShowReissueToOtherPartyUiSection(YesNo.YES);
        sscsCaseData.getReissueArtifactUi().setOtherPartyOptions(getOtherPartyOptions(sscsCaseData));
    }

    private static List<OtherPartyOption> getOtherPartyOptions(SscsCaseData sscsCaseData) {
        List<OtherPartyOption> otherPartyOptions = new ArrayList<>();

        sscsCaseData.getOtherParties().forEach(otherParty -> addOtherPartyOption(otherPartyOptions, otherParty));

        return otherPartyOptions;
    }

    private static void addOtherPartyOption(List<OtherPartyOption> otherPartyOptions, CcdValue<OtherParty> otherParty) {
        OtherParty otherPartyDetail = otherParty.getValue();

        if (isOtherPartyWithAppointee(otherPartyDetail)) {
            otherPartyOptions.add(getOtherPartyElement(otherPartyDetail.getAppointee().getName().getFullNameNoTitle() + " - Appointee", otherPartyDetail.getAppointee().getId()));
        } else {
            otherPartyOptions.add(getOtherPartyElement(otherPartyDetail.getName().getFullNameNoTitle(), otherPartyDetail.getId()));
        }

        if (isOtherPartyWithRepresentative(otherPartyDetail)) {
            otherPartyOptions.add(getOtherPartyElement(otherPartyDetail.getRep().getName().getFullNameNoTitle() + " - Representative", otherPartyDetail.getRep().getId()));
        }
    }

    private static boolean isOtherPartyWithRepresentative(OtherParty otherPartyDetail) {
        return otherPartyDetail.getRep() != null && "Yes".equals(otherPartyDetail.getRep().getHasRepresentative());
    }

    private static boolean isOtherPartyWithAppointee(OtherParty otherPartyDetail) {
        return otherPartyDetail.getAppointee() != null && "Yes".equals(otherPartyDetail.getIsAppointee());
    }

    private static OtherPartyOption getOtherPartyElement(String name, String id) {
        return OtherPartyOption.builder()
                .value(OtherPartyOptionDetails.builder()
                        .otherPartyOptionName(name)
                        .otherPartyOptionId(id)
                        .build()).build();
    }
}
