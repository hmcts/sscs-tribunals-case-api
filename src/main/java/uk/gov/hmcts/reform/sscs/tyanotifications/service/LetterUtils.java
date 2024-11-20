package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.EVENTS_FOR_ACTION_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.hasAppointee;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;

public class LetterUtils {

    private LetterUtils() {
        // Hiding utility class constructor
    }

    public static Address getAddressToUseForLetter(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType) {
        if (SubscriptionType.REPRESENTATIVE.equals(subscriptionWithType.getSubscriptionType())) {
            return wrapper.getNewSscsCaseData().getAppeal().getRep().getAddress();
        } else if (SubscriptionType.JOINT_PARTY.equals(subscriptionWithType.getSubscriptionType())) {
            if (isYes(wrapper.getNewSscsCaseData().getJointParty().getJointPartyAddressSameAsAppellant())) {
                return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress();
            }
            return wrapper.getNewSscsCaseData().getJointParty().getAddress();
        } else if (SubscriptionType.OTHER_PARTY.equals(subscriptionWithType.getSubscriptionType())) {
            return getAddressForOtherParty(wrapper.getNewSscsCaseData(), subscriptionWithType.getPartyId());
        } else {
            if (hasAppointee(wrapper.getSscsCaseDataWrapper())) {
                return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getAddress();
            }

            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress();
        }
    }

    private static Address getAddressForOtherParty(final SscsCaseData sscsCaseData, final String partyId) {
        return emptyIfNull(sscsCaseData.getOtherParties()).stream()
            .map(CcdValue::getValue)
            .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getAppointee().getId(), op.getAppointee().getAddress()) : null, Pair.of(op.getId(), op.getAddress()), (op.hasRepresentative()) ? Pair.of(op.getRep().getId(), op.getRep().getAddress()) : null))
            .filter(Objects::nonNull)
            .filter(p -> p.getLeft() != null && p.getRight() != null)
            .filter(p -> p.getLeft().equals(String.valueOf(partyId)))
            .map(Pair::getRight)
            .findFirst()
            .orElse(null);
    }

    public static Optional<Name> getNameForOtherParty(SscsCaseData sscsCaseData, final String partyId) {
        return emptyIfNull(sscsCaseData.getOtherParties()).stream()
            .map(CcdValue::getValue)
            .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getAppointee().getId(), op.getAppointee().getName()) : null, Pair.of(op.getId(), op.getName()), (op.hasRepresentative()) ? Pair.of(op.getRep().getId(), op.getRep().getName()) : null))
            .filter(Objects::nonNull)
            .filter(p -> p.getLeft() != null && p.getRight() != null)
            .filter(p -> p.getLeft().equals(partyId.toString()))
            .map(Pair::getRight)
            .findFirst();
    }

    public static String getNameToUseForLetter(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType) {
        if (SubscriptionType.REPRESENTATIVE.equals(subscriptionWithType.getSubscriptionType())) {
            return SendNotificationHelper.getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false);
        } else if (SubscriptionType.JOINT_PARTY.equals(subscriptionWithType.getSubscriptionType())) {
            return "%s %s".formatted(wrapper.getNewSscsCaseData().getJointParty().getName().getFirstName(), wrapper.getNewSscsCaseData().getJointParty().getName().getLastName());
        } else {
            if (nonNull(subscriptionWithType.getPartyId()) && isNotEmpty(wrapper.getNewSscsCaseData().getOtherParties())) {
                return getNameForOtherParty(wrapper.getNewSscsCaseData(), subscriptionWithType.getPartyId()).map(Name::getFullNameNoTitle).orElse("");
            }
            if (hasAppointee(wrapper.getSscsCaseDataWrapper())) {
                return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle();
            } else {
                return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getName().getFullNameNoTitle();
            }
        }
    }

    public static byte[] addBlankPageAtTheEndIfOddPage(byte[] letter) throws IOException {
        if (ArrayUtils.isNotEmpty(letter)) {
            PDDocument loadDoc = PDDocument.load(letter);
            if (loadDoc.getNumberOfPages() % 2 != 0) {
                final PDPage blankPage = new PDPage(PDRectangle.A4);
                // need to add PDPageContentStream here to pass gov notify validation!
                PDPageContentStream contents = new PDPageContentStream(loadDoc, blankPage);
                contents.beginText();
                contents.endText();
                contents.close();
                loadDoc.addPage(blankPage);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                loadDoc.save(baos);
                loadDoc.close();
                byte[] bytes = baos.toByteArray();
                baos.close();
                return bytes;
            }
        }
        return letter;
    }

    public static byte[] buildBundledLetter(byte[] coveringLetter, byte[] directionText) throws IOException {
        if (coveringLetter != null && directionText != null) {
            PDDocument bundledLetter = PDDocument.load(coveringLetter);

            PDDocument loadDoc = PDDocument.load(directionText);

            final PDFMergerUtility merger = new PDFMergerUtility();
            merger.appendDocument(bundledLetter, loadDoc);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bundledLetter.save(baos);
            bundledLetter.close();

            return baos.toByteArray();
        } else {
            throw new NotificationClientRuntimeException("Can not bundle empty documents");
        }
    }

    public static boolean isAlternativeLetterFormatRequired(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType) {
        YesNo wantsReasonableAdjustment = YesNo.NO;
        ReasonableAdjustments resAdj = wrapper.getNewSscsCaseData().getReasonableAdjustments();

        switch (subscriptionWithType.getSubscriptionType()) {
            case APPELLANT:
                if (resAdj != null) {
                    wantsReasonableAdjustment = resAdj.getAppellant() != null && resAdj.getAppellant().getWantsReasonableAdjustment() != null ? resAdj.getAppellant().getWantsReasonableAdjustment() : YesNo.NO;
                }
                break;
            case JOINT_PARTY:
                if (resAdj != null) {
                    wantsReasonableAdjustment = resAdj.getJointParty() != null && resAdj.getJointParty().getWantsReasonableAdjustment() != null ? resAdj.getJointParty().getWantsReasonableAdjustment() : YesNo.NO;
                }
                break;
            case APPOINTEE:
                if (resAdj != null) {
                    wantsReasonableAdjustment = resAdj.getAppointee() != null && resAdj.getAppointee().getWantsReasonableAdjustment() != null ? resAdj.getAppointee().getWantsReasonableAdjustment() : YesNo.NO;
                }
                break;
            case REPRESENTATIVE:
                if (resAdj != null) {
                    wantsReasonableAdjustment = resAdj.getRepresentative() != null && resAdj.getRepresentative().getWantsReasonableAdjustment() != null ? resAdj.getRepresentative().getWantsReasonableAdjustment() : YesNo.NO;
                }
                break;
            case OTHER_PARTY:
                wantsReasonableAdjustment = emptyIfNull(wrapper.getNewSscsCaseData().getOtherParties()).stream()
                    .map(CcdValue::getValue)
                    .flatMap(LetterUtils::buildOtherPartiesForReasonableAdjustment)
                    .filter(Objects::nonNull)
                    .filter(p -> p.getLeft() != null && p.getRight() != null)
                    .filter(p -> p.getLeft().equals(String.valueOf(subscriptionWithType.getPartyId())))
                    .map(Pair::getRight)
                    .findFirst()
                    .orElse(YesNo.NO);
                break;
            default:
                wantsReasonableAdjustment = YesNo.NO;
        }
        return wantsReasonableAdjustment.equals(YesNo.YES);
    }

    public static Stream<Pair<String, YesNo>> buildOtherPartiesForReasonableAdjustment(OtherParty op) {
        List<Pair<String, YesNo>> otherPartyReasonableAdjustmentList = new ArrayList<>();

        if (op.hasAppointee() && null != op.getAppointeeReasonableAdjustment()) {
            otherPartyReasonableAdjustmentList.add(Pair.of(op.getAppointee().getId(), op.getAppointeeReasonableAdjustment().getWantsReasonableAdjustment()));
        }
        if (null != op.getReasonableAdjustment()) {
            otherPartyReasonableAdjustmentList.add(Pair.of(op.getId(), op.getReasonableAdjustment().getWantsReasonableAdjustment()));
        }
        if (op.hasRepresentative() && null != op.getRepReasonableAdjustment()) {
            otherPartyReasonableAdjustmentList.add(Pair.of(op.getRep().getId(), op.getRepReasonableAdjustment().getWantsReasonableAdjustment()));
        }
        return otherPartyReasonableAdjustmentList.stream();
    }

    public static String getNameForSender(SscsCaseData sscsCaseData) {
        if (nonNull(sscsCaseData.getOriginalSender())) {
            final String originalSenderCode = sscsCaseData.getOriginalSender().getValue().getCode();
            if (originalSenderCode.equalsIgnoreCase(APPELLANT.getCode())) {
                return sscsCaseData.getAppeal().getAppellant().getName().getFullNameNoTitle();
            } else if (originalSenderCode.equalsIgnoreCase(PartyItemList.REPRESENTATIVE.getCode())) {
                return SendNotificationHelper.getRepSalutation(sscsCaseData.getAppeal().getRep(), false);
            } else if (originalSenderCode.equalsIgnoreCase(PartyItemList.JOINT_PARTY.getCode())) {
                return sscsCaseData.getJointParty().getName().getFullNameNoTitle();
            } else if (originalSenderCode.equalsIgnoreCase(DWP.getCode())) {
                return DWP.getLabel();
            } else if (originalSenderCode.equalsIgnoreCase(HMCTS.getCode())) {
                return HMCTS.getLabel();
            } else {
                return getOtherPartyName(sscsCaseData)
                    .map(Name::getFullNameNoTitle).orElse("");
            }
        }
        return "";
    }

    private static Optional<Representative> getRepresentativeOfOtherParty(SscsCaseData sscsCaseData) {
        return sscsCaseData.getOtherParties().stream()
            .map(CcdValue::getValue)
            .filter(OtherParty::hasRepresentative)
            .map(OtherParty::getRep)
            .filter(rep -> sscsCaseData.getOriginalSender().getValue().getCode().contains(rep.getId()))
            .findFirst();
    }

    public static Optional<Name> getOtherPartyName(SscsCaseData sscsCaseData) {
        String originalSenderCode = sscsCaseData.getOriginalSender().getValue().getCode();
        boolean otherPartyRep = originalSenderCode.contains("otherPartyRep");

        if (otherPartyRep) {
            Optional<Representative> representative = getRepresentativeOfOtherParty(sscsCaseData);
            return representative.map(Representative::getName);
        } else {
            return sscsCaseData.getOtherParties().stream()
                .filter(op -> originalSenderCode.contains(op.getValue().getId()))
                .findFirst()
                .map(op -> op.getValue().getName());
        }
    }

    public static String getNotificationTypeForActionFurtherEvidence(NotificationSscsCaseDataWrapper caseDataWrapper, SubscriptionWithType subscriptionWithType) {
        boolean isValidActionForNotification = EVENTS_FOR_ACTION_FURTHER_EVIDENCE.contains(caseDataWrapper.getNotificationEventType());
        if (isValidActionForNotification) {
            if (isValidForSetAsideRequest(caseDataWrapper, subscriptionWithType)) {
                return "confirmation";
            } else {
                return "notice";
            }
        }
        return "";
    }

    static boolean isValidForSetAsideRequest(NotificationSscsCaseDataWrapper caseDataWrapper, SubscriptionWithType subscriptionWithType) {
        return isValidAppellantForSetAsideRequest(caseDataWrapper, subscriptionWithType)
            || isValidAppellantRepresentativeForSetAsideRequest(caseDataWrapper, subscriptionWithType)
            || isValidJointPartyForSetAsideRequest(caseDataWrapper, subscriptionWithType)
            || isValidOtherParty(caseDataWrapper, subscriptionWithType);
    }

    static boolean isValidAppellantRepresentativeForSetAsideRequest(NotificationSscsCaseDataWrapper caseDataWrapper, SubscriptionWithType subscriptionWithType) {
        boolean senderIsRepresentative = caseDataWrapper.getNewSscsCaseData().getOriginalSender().getValue().getCode().equalsIgnoreCase("representative");
        if (senderIsRepresentative) {
            boolean isValidAppellant = subscriptionWithType.getParty().getId().equalsIgnoreCase(caseDataWrapper.getNewSscsCaseData().getAppeal().getAppellant().getId());
            boolean isValidRepresentative = subscriptionWithType.getParty().getId().equalsIgnoreCase(caseDataWrapper.getNewSscsCaseData().getAppeal().getRep().getId());
            return isValidAppellant || isValidRepresentative;
        }
        return false;
    }

    private static boolean isValidAppellantForSetAsideRequest(NotificationSscsCaseDataWrapper caseDataWrapper, SubscriptionWithType subscriptionWithType) {
        boolean senderIsAppellant = caseDataWrapper.getNewSscsCaseData().getOriginalSender().getValue().getCode().equalsIgnoreCase("appellant");
        if (senderIsAppellant) {
            return subscriptionWithType.getParty().getId().equalsIgnoreCase(caseDataWrapper.getNewSscsCaseData().getAppeal().getAppellant().getId());
        }
        return false;
    }

    private static boolean isValidJointPartyForSetAsideRequest(NotificationSscsCaseDataWrapper caseDataWrapper, SubscriptionWithType subscriptionWithType) {
        boolean isJointParty = caseDataWrapper.getNewSscsCaseData().getOriginalSender().getValue().getCode().equalsIgnoreCase("jointParty");
        if (isJointParty) {
            return subscriptionWithType.getParty().getId().equalsIgnoreCase(caseDataWrapper.getNewSscsCaseData().getJointParty().getId());
        }
        return false;
    }

    static boolean isValidOtherParty(NotificationSscsCaseDataWrapper caseDataWrapper, SubscriptionWithType subscriptionWithType) {
        String subscriptionPartyId = subscriptionWithType.getPartyId();
        String requesterId = caseDataWrapper.getNewSscsCaseData().getOriginalSender().getValue().getCode();

        if (nonNull(subscriptionPartyId) && requesterId.contains("otherParty")) {
            List<CcdValue<OtherParty>> otherParties = caseDataWrapper.getNewSscsCaseData().getOtherParties();
            return nonNull(otherParties) && isValidOtherPartyForSubscription(subscriptionPartyId, requesterId, otherParties);
        }
        return false;
    }

    private static boolean isValidOtherPartyForSubscription(String subscriptionPartyId, String requesterId, List<CcdValue<OtherParty>> otherParties) {
        for (CcdValue<OtherParty> op : otherParties) {
            if (subscriptionPartyId.equals(op.getValue().getId()) && requesterId.contains(op.getValue().getId())) {
                return true;
            }
            if (isValidOtherPartyRepresentative(subscriptionPartyId, requesterId, op)) {
                return true;
            }
        }
        return false;
    }

    static boolean isValidOtherPartyRepresentative(String subscriptionPartyId, String requesterId, CcdValue<OtherParty> op) {
        if (op.getValue().hasRepresentative()) {
            String representativeId = op.getValue().getRep().getId();
            if (subscriptionPartyId.contains(representativeId)
                && requesterId.contains(op.getValue().getId())) {
                return true;
            }
            if (requesterId.contains(representativeId)
                && (subscriptionPartyId.equals(representativeId)
                || subscriptionPartyId.contains(op.getValue().getId()))) {
                return true;
            }
        }
        return false;
    }
}
