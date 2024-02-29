package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Entity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

public class OtherPartyDataUtil {

    private OtherPartyDataUtil() {
    }

    public static List<CcdValue<OtherParty>> clearOtherPartiesIfEmpty(final SscsCaseData sscsCaseData) {
        return isEmpty(sscsCaseData.getOtherParties()) ? null : sscsCaseData.getOtherParties();
    }

    public static String getOtherPartyUcb(final List<CcdValue<OtherParty>> otherPartyCcdValue) {
        if (otherPartyCcdValue != null) {
            return otherPartyCcdValue.stream()
                            .filter(otherParty -> isYes(otherParty.getValue().getUnacceptableCustomerBehaviour()))
                            .map(otherParty -> otherParty.getValue().getUnacceptableCustomerBehaviour().getValue())
                            .findAny()
                    .orElse(NO.getValue());
        }
        return null;
    }

    public static YesNo sendNewOtherPartyNotification(final CcdValue<OtherParty> otherPartyCcdValue) {
        OtherParty otherParty = otherPartyCcdValue.getValue();

        if (isNull(otherParty.getSendNewOtherPartyNotification())) {
            //All the newly added other parties will be sent a notification
            return YES;
        } else if (isYes(otherParty.getSendNewOtherPartyNotification())) {
            //Notification flag is set to no for the other parties that already been notified
            return NO;
        }
        return null;
    }

    public static boolean haveOtherPartiesChanged(List<CcdValue<OtherParty>> before, List<CcdValue<OtherParty>> after) {
        if (isEmpty(before) && isEmpty(after)) {
            return false;
        }
        return hasCollectionChanged(before, after)
            || hasOtherPartiesIdsChanged(before, after);
    }

    private static <T> boolean hasCollectionChanged(Collection<T> before, Collection<T> after) {
        return isNull(before) ^ isNull(after)
            || (nonNull(before) && before.size() != after.size());
    }

    private static boolean hasOtherPartiesIdsChanged(List<CcdValue<OtherParty>> before, List<CcdValue<OtherParty>> after) {
        return !getAllOtherPartyIds(before).containsAll(getAllOtherPartyIds(after));
    }

    @NotNull
    private static Set<String> getAllOtherPartyIds(List<CcdValue<OtherParty>> before) {
        return before.stream()
            .map(CcdValue::getValue)
            .map(Entity::getId)
            .collect(Collectors.toSet());
    }

    public static YesNo isConfidential(final SscsCaseData sscsCaseData) {
        if (isValidBenefitTypeForConfidentiality(sscsCaseData.getAppeal().getBenefitType())) {
            if ((sscsCaseData.getAppeal().getAppellant() != null
                    && sscsCaseData.getAppeal().getAppellant().getConfidentialityRequired() != null
                    && isYes(sscsCaseData.getAppeal().getAppellant().getConfidentialityRequired()))
                    || otherPartyHasConfidentiality(sscsCaseData)) {
                return YES;
            }
        }
        return null;
    }

    public static boolean isValidBenefitTypeForConfidentiality(final BenefitType benefitType) {
        return benefitType != null
                && (Arrays.stream(Benefit.values())
                .anyMatch(b -> (SscsType.SSCS2.equals(b.getSscsType()) || SscsType.SSCS5.equals(b.getSscsType()))
                && b.getShortName().equals(benefitType.getCode())));
    }

    public static boolean isOtherPartyPresent(SscsCaseData sscsCaseData) {
        return sscsCaseData.getOtherParties() != null && sscsCaseData.getOtherParties().size() > 0;
    }

    private static boolean otherPartyHasConfidentiality(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getOtherParties() != null) {
            return sscsCaseData.getOtherParties().stream()
                    .anyMatch(op -> isYes(op.getValue().getConfidentialityRequired()));
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
                            .toList());
        }
        return false;
    }

    public static List<CcdValue<OtherParty>> getOtherPartiesWithClearedRoles(
            final List<CcdValue<OtherParty>> otherParties) {
        return isNull(otherParties) ? null : otherParties.stream()
            .filter(otherPartyCcdValue -> nonNull(otherPartyCcdValue.getValue()))
            .map(OtherPartyDataUtil::clearRoleForOtherParty)
                .collect(Collectors.toList());
    }

    public static boolean roleExistsForOtherParties(final List<CcdValue<OtherParty>> otherParties) {
        return emptyIfNull(otherParties).stream()
            .filter(otherPartyCcdValue -> nonNull(otherPartyCcdValue.getValue()))
            .map(CcdValue::getValue)
            .anyMatch(otherParty -> otherParty.getRole() != null && otherParty.getRole().getName() != null);
    }

    public static boolean roleAbsentForOtherParties(final List<CcdValue<OtherParty>> otherParties) {
        return emptyIfNull(otherParties).stream()
            .filter(otherPartyCcdValue -> nonNull(otherPartyCcdValue.getValue()))
            .map(CcdValue::getValue)
            .anyMatch(otherParty -> otherParty.getRole() == null || otherParty.getRole().getName() == null);
    }

    public static boolean otherPartyWantsToAttendHearing(final List<CcdValue<OtherParty>> otherParties) {
        return emptyIfNull(otherParties).stream()
            .filter(otherPartyCcdValue -> nonNull(otherPartyCcdValue.getValue()))
            .map(otherParty -> otherParty.getValue())
            .filter(otherParty -> nonNull(otherParty.getHearingOptions()))
            .anyMatch(otherPartyHearing ->
                    otherPartyHearing.getHearingOptions().isWantsToAttendHearing().equals(Boolean.TRUE));
    }

    private static CcdValue<OtherParty> clearRoleForOtherParty(final CcdValue<OtherParty> otherParty) {
        otherParty.getValue().setShowRole(NO);
        otherParty.getValue().setRole(null);
        return otherParty;
    }

    public static boolean isSscs2Case(String benefitTypeCode) {
        return Optional.ofNullable(benefitTypeCode)
            .filter(b -> Benefit.findBenefitByShortName(b)
            .filter(benefit -> benefit.getSscsType().equals(SscsType.SSCS2)).isPresent())
            .isPresent();
    }
}
