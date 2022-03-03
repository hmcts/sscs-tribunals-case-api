package uk.gov.hmcts.reform.sscs.util;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.*;

import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class PartiesOnCaseUtil {
    private PartiesOnCaseUtil() {
        //
    }

    public static List<DynamicListItem> getPartiesOnCaseWithDwpAndHmcts(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = getPartiesOnCase(sscsCaseData);

        listOptions.add(new DynamicListItem(DWP.getCode(), DWP.getLabel()));

        listOptions.add(new DynamicListItem(HMCTS.getCode(), HMCTS.getLabel()));

        return listOptions;
    }

    public static List<DynamicListItem> getPartiesOnCase(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        listOptions.add(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));

        if (sscsCaseData.isThereAJointParty()) {
            listOptions.add(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));
        }

        if (sscsCaseData.getAppeal().getRep() != null
                && isYes(sscsCaseData.getAppeal().getRep().getHasRepresentative())) {
            listOptions.add(new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel()));
        }

        if (isNotEmpty(sscsCaseData.getOtherParties())) {
            addOtherPartiesToListOptions(sscsCaseData, listOptions);
        }

        return listOptions;
    }

    private static void addOtherPartiesToListOptions(SscsCaseData sscsCaseData, List<DynamicListItem> listOptions) {
        for (int i = 0; i < sscsCaseData.getOtherParties().size(); i++) {
            OtherParty otherParty = sscsCaseData.getOtherParties().get(i).getValue();
            addOtherPartyOrOtherPartyAppointeeToListOptions(listOptions, i, otherParty);
            addOtherPartyRepresentativeToListOptions(listOptions, i, otherParty);
        }
    }

    private static void addOtherPartyRepresentativeToListOptions(List<DynamicListItem> listOptions, int i, OtherParty otherParty) {
        if (isYes(ofNullable(otherParty.getRep()).map(Representative::getHasRepresentative).orElse(NO)) && otherParty.getRep() != null && otherParty.getRep().getName() != null) {
            listOptions.add(new DynamicListItem(OTHER_PARTY_REPRESENTATIVE.getCode() + otherParty.getRep().getId(), format("%s %s - Representative - %s", OTHER_PARTY_REPRESENTATIVE.getLabel(), i + 1, otherParty.getRep().getName().getFullNameNoTitle())));
        }
    }

    private static void addOtherPartyOrOtherPartyAppointeeToListOptions(List<DynamicListItem> listOptions, int i, OtherParty otherParty) {
        if (isYes(otherParty.getIsAppointee()) && otherParty.getAppointee() != null && otherParty.getAppointee().getName() != null) {
            listOptions.add(new DynamicListItem(OTHER_PARTY.getCode() + otherParty.getAppointee().getId(), format("%s %s - %s / Appointee - %s", OTHER_PARTY.getLabel(), i + 1, otherParty.getName().getFullNameNoTitle(), otherParty.getAppointee().getName().getFullNameNoTitle())));
        } else {
            listOptions.add(new DynamicListItem(OTHER_PARTY.getCode() + otherParty.getId(), format("%s %s - %s", OTHER_PARTY.getLabel(), i + 1, otherParty.getName().getFullNameNoTitle())));
        }
    }

    public static boolean isChildSupportAppeal(SscsCaseData sscsCaseData) {
        return sscsCaseData.getBenefitType()
                .filter(f -> f == Benefit.CHILD_SUPPORT)
                .isPresent();
    }

    public static List<String> getAllOtherPartiesOnCase(SscsCaseData sscsCaseData) {
        List<String> otherParties = new ArrayList<>();
        if (sscsCaseData.getOtherParties() != null && sscsCaseData.getOtherParties().size() > 0) {
            for (CcdValue<OtherParty> otherParty : sscsCaseData.getOtherParties()) {

                otherParties.add(otherParty.getValue().getName().getFullName());

                if (isYes(otherParty.getValue().getIsAppointee())) {
                    otherParties.add(otherParty.getValue().getAppointee().getName().getFullName() + " - Appointee");
                }
                if (isYes(otherParty.getValue().getRep().getHasRepresentative())) {
                    otherParties.add(otherParty.getValue().getRep().getName().getFullName() + " - Representative");
                }
            }
        }
        return otherParties;
    }
}
