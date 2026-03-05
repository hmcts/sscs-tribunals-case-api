package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentiality;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.updateAppellantConfidentialityRequiredChangedDate;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.updateOtherPartiesConfidentialityChangedDate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class ConfidentialityTabAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm:ss a", Locale.UK);
    private final boolean cmOtherPartyConfidentialityEnabled;

    public ConfidentialityTabAboutToSubmitHandler(
        @Value("${feature.cm-other-party-confidentiality.enabled}") boolean cmOtherPartyConfidentialityEnabled) {
        this.cmOtherPartyConfidentialityEnabled = cmOtherPartyConfidentialityEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        // Runs for every event type to ensure that any changes to data are reflected in the tab and also to future-proof against other new or modified events that may update confidentiality data
        return callbackType == CallbackType.ABOUT_TO_SUBMIT && (cmOtherPartyConfidentialityEnabled && (callback.getCaseDetails()
            .getCaseData().isBenefitType(Benefit.CHILD_SUPPORT) || callback.getCaseDetails()
            .getCaseData().isBenefitType(Benefit.UC)));
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
        String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        updateAppellantConfidentialityRequiredChangedDate(callback);
        updateOtherPartiesConfidentialityRequiredChangedDate(callback);
        sscsCaseData.getExtendedSscsCaseData()
            .setConfidentialityTab(getConfidentialitySummaryEntries(sscsCaseData.getOtherParties(), sscsCaseData.getAppeal()));

        if (sscsCaseData.isBenefitType(Benefit.CHILD_SUPPORT)
            || (sscsCaseData.isBenefitType(Benefit.UC)) && isNotEmpty(sscsCaseData.getOtherParties())) {
            sscsCaseData.getExtendedSscsCaseData().setShowConfidentialityTab(YES);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private static String getConfidentialitySummaryEntries(List<CcdValue<OtherParty>> otherParties, Appeal appeal) {

        final List<CcdValue<ConfidentialitySummaryEntry>> results = new ArrayList<>();

        addIfNotNull(results, buildAppellantConfidentialityTabEntry(appeal));
        addIfNotNull(results, buildAppellantAppointeeConfidentialityTabEntry(appeal));

        final AtomicInteger atomicInteger = new AtomicInteger(1);

        emptyIfNull(otherParties).stream().filter(Objects::nonNull).map(CcdValue::getValue).filter(Objects::nonNull)
            .map(otherParty -> buildOtherPartyEntry(otherParty, atomicInteger.getAndIncrement()))
            .forEach(entry -> results.add(CcdValue.<ConfidentialitySummaryEntry>builder().value(entry).build()));

        var confidentialityMarkdown = new StringBuilder();
        results.forEach(entry -> confidentialityMarkdown.append(
            String.format("%s | %s | %s | %s\r\n", entry.getValue().getParty(), entry.getValue().getName(),
                entry.getValue().getConfidentialityRequired(), entry.getValue().getConfidentialityRequiredChangedDate())));

        return """
            Party | Name | Confidentiality Status | Confidentiality Status Confirmed
            -|-|-|-
            %s
            """.formatted(confidentialityMarkdown.toString());

    }

    private static void addIfNotNull(List<CcdValue<ConfidentialitySummaryEntry>> list, ConfidentialitySummaryEntry entry) {
        if (entry != null) {
            list.add(CcdValue.<ConfidentialitySummaryEntry>builder().value(entry).build());
        }
    }

    private static String getConfidentialityStatus(YesNo confidentialityRequired) {
        return confidentialityRequired != null ? confidentialityRequired.getValue() : "Undetermined";
    }

    private static String extractFullName(Name name) {
        return name != null ? name.getFullNameNoTitle() : "";
    }

    private static ConfidentialitySummaryEntry buildAppellantConfidentialityTabEntry(Appeal appeal) {
        if (missingAppellant(appeal)) {
            return null;
        }
        final Appellant appellant = appeal.getAppellant();
        return ConfidentialitySummaryEntry.builder().name(extractFullName(appellant.getName())).party("Appellant")
            .confidentialityRequired(getConfidentialityStatus(appellant.getConfidentialityRequired()))
            .confidentialityRequiredChangedDate(formatDate(appellant.getConfidentialityRequiredChangedDate())).build();
    }

    private static ConfidentialitySummaryEntry buildAppellantAppointeeConfidentialityTabEntry(Appeal appeal) {
        if (missingAppellant(appeal) || appeal.getAppellant().getAppointee() == null || !YesNo.isYes(
            appeal.getAppellant().getIsAppointee())) {
            return null;
        }
        final Appointee appointee = appeal.getAppellant().getAppointee();

        return ConfidentialitySummaryEntry.builder().name(extractFullName(appointee.getName())).party("Appointee")
            .confidentialityRequired(getConfidentialityStatus(appeal.getAppellant().getConfidentialityRequired()))
            .confidentialityRequiredChangedDate(formatDate(appeal.getAppellant().getConfidentialityRequiredChangedDate()))
            .build();
    }

    private static boolean missingAppellant(Appeal appeal) {
        return appeal.getAppellant() == null;
    }

    private static String formatDate(final LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(FORMATTER) : "";
    }

    private static ConfidentialitySummaryEntry buildOtherPartyEntry(OtherParty otherParty, int displayIndex) {
        return ConfidentialitySummaryEntry.builder().name(extractFullName(otherParty.getName()))
            .party("Other Party " + displayIndex)
            .confidentialityRequired(getConfidentialityStatus(otherParty.getConfidentialityRequired()))
            .confidentialityRequiredChangedDate(formatDate(otherParty.getConfidentialityRequiredChangedDate())).build();
    }

    @Builder
    @Getter
    private static class ConfidentialitySummaryEntry {
        private String party;
        private String name;
        private String confidentialityRequired;
        private String confidentialityRequiredChangedDate;
    }

    private void updateOtherPartiesConfidentialityRequiredChangedDate(Callback<SscsCaseData> callback) {
        final List<CcdValue<OtherParty>> previousOtherParties = callback.getCaseDetailsBefore().map(CaseDetails::getCaseData)
            .map(SscsCaseData::getOtherParties).orElse(null);
        updateOtherPartiesConfidentialityChangedDate(callback.getCaseDetails().getCaseData().getOtherParties(),
            previousOtherParties);
    }
}
