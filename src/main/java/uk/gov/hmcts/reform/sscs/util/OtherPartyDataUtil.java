package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsType.SSCS2;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsType.SSCS5;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.getLocalDateTime;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNoUndetermined;

public class OtherPartyDataUtil {

    private static final Set<String> VALID_CONFIDENTIALITY_BENEFITS =
        Arrays.stream(Benefit.values())
            .filter(benefit ->
                SSCS2.equals(benefit.getSscsType()) || SSCS5.equals(benefit.getSscsType()))
            .map(Benefit::getShortName)
            .collect(toSet());

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
            .collect(toSet());
    }

    public static YesNo isConfidential(final SscsCaseData sscsCaseData) {
        return isConfidential(sscsCaseData, false);
    }

    public static YesNo isConfidential(final SscsCaseData sscsCaseData,
        final boolean cmOtherPartyConfidentialityEnabled) {
        if (sscsCaseData == null || sscsCaseData.getAppeal() == null) {
            return null;
        }
        if (!isValidBenefitTypeForConfidentiality(sscsCaseData.getAppeal().getBenefitType(),
            cmOtherPartyConfidentialityEnabled)) {
            return null;
        }
        final YesNoUndetermined appellantConfidentiality = sscsCaseData.getAppellantConfidentiality().orElse(null);
        if (appellantConfidentiality == YesNoUndetermined.YES || otherPartyHasConfidentiality(sscsCaseData)) {
            return YesNo.YES;
        }
        if (appellantConfidentiality == YesNoUndetermined.NO && allOtherPartiesDoNotWantConfidentiality(sscsCaseData)) {
            return YesNo.NO;
        }
        return null;
    }

    // TODO Why is the FF hard-coded as false? Can we remove this and use the method below, and if we do that we can use the predicate in sscs-common
    public static boolean isValidBenefitTypeForConfidentiality(final BenefitType benefitType) {
        return isValidBenefitTypeForConfidentiality(benefitType, false);
    }

    // TODO Can replace this with the predicate defined in sscs-common once cmOtherPartyConfidentialityEnabled is removed
    public static boolean isValidBenefitTypeForConfidentiality(
        final BenefitType benefitType,
        final boolean cmOtherPartyConfidentialityEnabled) {

        if (benefitType == null) {
            return false;
        }

        String benefitCode = benefitType.getCode();

        return VALID_CONFIDENTIALITY_BENEFITS.contains(benefitCode)
            || (cmOtherPartyConfidentialityEnabled
            && Benefit.UC.getShortName().equals(benefitCode));
    }

    public static boolean isOtherPartyPresent(SscsCaseData sscsCaseData) {
        return sscsCaseData.getOtherParties() != null && !sscsCaseData.getOtherParties().isEmpty();
    }

    private static boolean allOtherPartiesDoNotWantConfidentiality(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getOtherParties() != null) {
            return sscsCaseData.getOtherParties().stream()
                               .allMatch(op -> YesNoUndetermined.isNo(op.getValue().getConfidentialityRequirement()));
        }
        return true;
    }

    private static boolean otherPartyHasConfidentiality(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getOtherParties() != null) {
            return sscsCaseData.getOtherParties().stream()
                    .anyMatch(op -> YesNoUndetermined.isYes(op.getValue().getConfidentialityRequirement()));
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
                    .collect(toSet())
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
                .collect(toList());
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
            .map(CcdValue::getValue)
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
            .filter(benefit -> benefit.getSscsType().equals(SSCS2)).isPresent())
            .isPresent();
    }

    public static void updateOtherPartiesConfidentialityChangedDate(final List<CcdValue<OtherParty>> currentOtherParties,
                                                        final List<CcdValue<OtherParty>> previousOtherParties) {
        if (isEmpty(currentOtherParties)) {
            return;
        }
        final Map<String, YesNoUndetermined> confidentialityBefore = buildConfidentialityMap(previousOtherParties);
        currentOtherParties.stream()
            .filter(Objects::nonNull)
            .map(CcdValue::getValue)
            .filter(Objects::nonNull)
            .forEach(current -> {
                final YesNoUndetermined priorConfidentiality = confidentialityBefore.get(current.getId());
                if (nonNull(current.getConfidentialityRequirement())
                    && (priorConfidentiality == null
                        || !Objects.equals(priorConfidentiality, current.getConfidentialityRequirement()))) {
                    current.setConfidentialityRequiredChangedDate(getLocalDateTime());
                }
            });
    }

    private static Map<String, YesNoUndetermined> buildConfidentialityMap(final List<CcdValue<OtherParty>> otherParties) {
        if (isEmpty(otherParties)) {
            return Collections.emptyMap();
        }
        final Map<String, YesNoUndetermined> byId = new HashMap<>();
        otherParties.stream()
            .filter(Objects::nonNull)
            .map(CcdValue::getValue)
            .filter(Objects::nonNull)
            .forEach(prior -> byId.put(prior.getId(), prior.getConfidentialityRequirement()));
        return byId;
    }

}
