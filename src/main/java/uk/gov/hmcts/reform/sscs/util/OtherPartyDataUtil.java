package uk.gov.hmcts.reform.sscs.util;

import static java.util.Collections.sort;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class OtherPartyDataUtil {

    private OtherPartyDataUtil() {
    }

    public static void updateOtherPartyUcb(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getOtherParties() != null) {
            sscsCaseData.setOtherPartyUcb(sscsCaseData.getOtherParties().stream()
                    .filter(o -> isYes(o.getValue().getUnacceptableCustomerBehaviour()))
                    .map(o -> o.getValue().getUnacceptableCustomerBehaviour().getValue())
                    .findAny()
                    .orElse(NO.getValue()));
        }
    }

    public static void assignNewOtherPartyData(List<CcdValue<OtherParty>> otherParties, EventType eventType) {
        int maxId = getMaxId(otherParties);
        for (CcdValue<OtherParty> ccdOtherParty : otherParties) {
            OtherParty otherParty = ccdOtherParty.getValue();
            if (otherParty.getId() == null) {
                otherParty.setId(Integer.toString(++maxId));
                //All the newly added other parties will be sent a notification
                otherParty.setSendNewOtherPartyNotification(YES);
            } else if (EventType.DWP_UPLOAD_RESPONSE.equals(eventType) && otherParty.getSendNewOtherPartyNotification() == null) {
                //The other party added at case creation time, by bulk-scan, will be notified in dwpUploadResponse event
                otherParty.setSendNewOtherPartyNotification(YES);
            } else if (YesNo.isYes(otherParty.getSendNewOtherPartyNotification())) {
                //Notification flag is set to no for the other parties that already been notified
                otherParty.setSendNewOtherPartyNotification(NO);
            }

            if (otherParty.getAppointee() != null && isYes(otherParty.getIsAppointee()) && otherParty.getAppointee().getId() == null) {
                otherParty.getAppointee().setId(Integer.toString(++maxId));
            }
            if (otherParty.getRep() != null && isYes(otherParty.getRep().getHasRepresentative()) && otherParty.getRep().getId() == null) {
                otherParty.getRep().setId(Integer.toString(++maxId));
            }
        }
    }

    @NotNull
    private static int getMaxId(List<CcdValue<OtherParty>> otherParties) {
        List<Integer> currentIds = new ArrayList<>();
        otherParties.stream().forEach(o -> {
            OtherParty otherParty = o.getValue();
            if (otherParty.getId() != null) {
                currentIds.add(Integer.parseInt(otherParty.getId()));
            }
            if (otherParty.getAppointee() != null && otherParty.getAppointee().getId() != null) {
                currentIds.add(Integer.parseInt(otherParty.getAppointee().getId()));
            }
            if (otherParty.getRep() != null && otherParty.getRep().getId() != null) {
                currentIds.add(Integer.parseInt(otherParty.getRep().getId()));
            }
        });
        return currentIds.stream().max(Comparator.naturalOrder()).orElse(0);
    }

    public static boolean haveOtherPartiesChanged(List<CcdValue<OtherParty>> before, List<CcdValue<OtherParty>> after) {
        if ((before == null || before.size() == 0) && (after == null || after.size() == 0)) {
            return false;
        }
        if (before == null ^ after == null) {
            return true;
        }
        if (before.size() != after.size()) {
            return true;
        }
        List<String> beforeIds = before.stream().map(ccdValue -> ccdValue.getValue().getId()).collect(Collectors.toList());
        List<String> afterIds = after.stream().map(ccdValue -> ccdValue.getValue().getId()).collect(Collectors.toList());
        sort(beforeIds);
        sort(afterIds);
        for (int i = 0; i < beforeIds.size(); i++) {
            if (!beforeIds.get(i).equals(afterIds.get(i))) {
                return true;
            }
        }
        return false;
    }

    public static void checkConfidentiality(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAppeal().getBenefitType() != null
                && Benefit.CHILD_SUPPORT.getShortName().equals(sscsCaseData.getAppeal().getBenefitType().getCode())) {
            if ((sscsCaseData.getAppeal().getAppellant() != null
                    && sscsCaseData.getAppeal().getAppellant().getConfidentialityRequired() != null
                    && YesNo.isYes(sscsCaseData.getAppeal().getAppellant().getConfidentialityRequired()))
                    || otherPartyHasConfidentiality(sscsCaseData)) {
                sscsCaseData.setIsConfidentialCase(YES);
            } else {
                sscsCaseData.setIsConfidentialCase(null);
            }
        }
    }

    private static boolean otherPartyHasConfidentiality(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getOtherParties() != null) {
            Optional otherParty = sscsCaseData.getOtherParties().stream().filter(op -> YesNo.isYes(op.getValue().getConfidentialityRequired())).findAny();
            if (otherParty.isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOtherPartyAppointee(SscsCaseData sscsCaseData, Predicate<Subscription> predicate) {
        return emptyIfNull(sscsCaseData.getOtherParties()).stream()
                .map(CcdValue::getValue)
                .filter(otherParty -> isYes(otherParty.getIsAppointee()))
                .filter(otherParty -> nonNull(otherParty.getAppointee()))
                .filter(otherParty -> nonNull(otherParty.getOtherPartyAppointeeSubscription()))
                .anyMatch(otherParty -> predicate.test(otherParty.getOtherPartyAppointeeSubscription()));
    }

    public static Predicate<Subscription> withEmailPredicate(String email) {
        return (Subscription sub) -> email.equalsIgnoreCase(sub.getEmail());
    }

    public static Predicate<Subscription> withTyaPredicate(String tya) {
        return (Subscription sub) -> tya.equalsIgnoreCase(sub.getTya());
    }

    public static  boolean isOtherPartyRep(SscsCaseData sscsCaseData, Predicate<Subscription> predicate) {
        return emptyIfNull(sscsCaseData.getOtherParties()).stream()
                .map(CcdValue::getValue)
                .filter(otherParty -> nonNull(otherParty.getRep()))
                .filter(otherParty -> isYes(otherParty.getRep().getHasRepresentative()))
                .filter(otherParty -> nonNull(otherParty.getOtherPartyRepresentativeSubscription()))
                .anyMatch(otherParty -> predicate.test(otherParty.getOtherPartyRepresentativeSubscription()));
    }

    public static boolean isOtherParty(SscsCaseData sscsCaseData, Predicate<Subscription> predicate) {
        return emptyIfNull(sscsCaseData.getOtherParties()).stream()
                .map(CcdValue::getValue)
                .filter(otherParty -> isNoOrNull(otherParty.getIsAppointee()))
                .filter(otherParty -> nonNull(otherParty.getOtherPartySubscription()))
                .anyMatch(otherParty -> predicate.test(otherParty.getOtherPartySubscription()));
    }


    public static String getOtherPartyName(SscsCaseData sscsCaseData, String otherPartyId) {
        return emptyIfNull(sscsCaseData.getOtherParties()).stream()
                .map(CcdValue::getValue)
                .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getAppointee().getName(), op.getAppointee().getId()) : Pair.of(op.getName(), op.getId()), (op.hasRepresentative()) ? Pair.of(op.getRep().getName(), op.getRep().getId()) : null))
                .filter(Objects::nonNull)
                .filter(p -> nonNull(p.getLeft()))
                .filter(p -> nonNull(p.getRight()))
                .filter(p -> p.getRight().equals(otherPartyId))
                .map(Pair::getLeft)
                .map(Name::getFullNameNoTitle)
                .findFirst()
                .orElse(null);

    }

    public static String getOtherPartyName(SscsCaseData sscsCaseData, Predicate<Subscription> predicate) {
        return emptyIfNull(sscsCaseData.getOtherParties()).stream()
                .map(CcdValue::getValue)
                .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getOtherPartyAppointeeSubscription(), op.getAppointee().getName()) : Pair.of(op.getOtherPartySubscription(), op.getName()), (op.hasRepresentative()) ? Pair.of(op.getOtherPartyRepresentativeSubscription(), op.getRep().getName()) : null))
                .filter(Objects::nonNull)
                .filter(p -> nonNull(p.getLeft()))
                .filter(p -> nonNull(p.getLeft().getEmail()))
                .filter(p -> nonNull(p.getRight()))
                .filter(p -> predicate.test(p.getLeft()))
                .map(Pair::getRight)
                .map(Name::getFullNameNoTitle)
                .findFirst()
                .orElse(null);
    }

    public static String getOtherPartyId(SscsCaseData sscsCaseData, Predicate<Subscription> predicate) {
        return emptyIfNull(sscsCaseData.getOtherParties()).stream()
                .map(CcdValue::getValue)
                .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getOtherPartyAppointeeSubscription(), op.getAppointee().getId()) : Pair.of(op.getOtherPartySubscription(), op.getId()), (op.hasRepresentative()) ? Pair.of(op.getOtherPartyRepresentativeSubscription(), op.getRep().getId()) : null))
                .filter(Objects::nonNull)
                .filter(p -> nonNull(p.getLeft()))
                .filter(p -> nonNull(p.getRight()))
                .filter(p -> predicate.test(p.getLeft()))
                .map(Pair::getRight)
                .findFirst()
                .orElse(null);
    }

    public static boolean hasNewOtherPartyAdded(List<CcdValue<OtherParty>> before, List<CcdValue<OtherParty>> after) {
        if (after != null && !after.isEmpty()) {
            if (before == null || before.isEmpty()) {
                return true;
            }

            return !before.stream()
                    .map(o -> o.getValue().getId())
                    .collect(Collectors.toSet())
                    .containsAll(after.stream()
                            .map(o -> o.getValue().getId())
                            .collect(Collectors.toList()));
        }
        return false;
    }
}
