package uk.gov.hmcts.reform.sscs.ccd.presubmit.writestatementofreasons;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.IN_CHAMBERS;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getLastValidHearing;

import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueNoticeHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.service.JudicialRefDataService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

@Component
@Slf4j
public class WriteStatementOfReasonsPreviewService extends IssueNoticeHandler {
    private final VenueDataLoader venueDataLoader;
    private final JudicialRefDataService judicialRefDataService;

    public WriteStatementOfReasonsPreviewService(GenerateFile generateFile, UserDetailsService userDetailsService,
                                                 @Value("${documents.english.SOR_WRITE}") String templateId,
                                                 DocumentConfiguration documentConfiguration, VenueDataLoader venueDataLoader,
                                                 JudicialRefDataService judicialRefDataService) {
        super(generateFile, userDetailsService, languagePreference -> templateId, documentConfiguration);
        this.venueDataLoader = venueDataLoader;
        this.judicialRefDataService = judicialRefDataService;
    }

    @Override
    protected NoticeIssuedTemplateBody createPayload(PreSubmitCallbackResponse<SscsCaseData> response,
                                                     SscsCaseData caseData, String documentTypeLabel,
                                                     LocalDate dateAdded, LocalDate generatedDate,
                                                     boolean isScottish, boolean isPostHearingsEnabled,
                                                     boolean isPostHearingsBEnabled,
                                                     String userAuthorisation) {
        NoticeIssuedTemplateBody formPayload = super.createPayload(
                response,
                caseData,
                documentTypeLabel,
                dateAdded,
                LocalDate.now(),
                isScottish,
                isPostHearingsEnabled,
                isPostHearingsBEnabled,
                userAuthorisation);

        NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder builder = formPayload.toBuilder();

        setHearings(builder, caseData);

        builder.dateIssued(LocalDate.now());

        return builder.build();
    }

    protected void setHearings(NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder noticeBuilder, SscsCaseData caseData) {
        HearingDetails finalHearing = getLastValidHearing(caseData);
        if (nonNull(finalHearing)) {
            if (nonNull(finalHearing.getHearingDate())) {
                noticeBuilder.heldOn(LocalDate.parse(finalHearing.getHearingDate()));
            }

            if (nonNull(finalHearing.getVenue())) {
                String venueName = venueDataLoader.getGapVenueName(finalHearing.getVenue(), finalHearing.getVenueId());
                if (nonNull(venueName)) {
                    noticeBuilder.heldAt(venueName);
                }
            }

            JudicialUserPanel panelMembers = finalHearing.getPanel();
            if (nonNull(panelMembers)) {
                List<String> panelMemberNames = judicialRefDataService.getAllJudicialUsersFullNames(panelMembers.getAllPanelMembers());

                if (isNotEmpty(panelMemberNames)) {
                    noticeBuilder.heldBefore(StringUtils.getGramaticallyJoinedStrings(panelMemberNames));
                }
            }
        } else {
            setInChambers(noticeBuilder);
        }
    }

    private void setInChambers(NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder noticeBuilder) {
        noticeBuilder.heldOn(LocalDate.now());
        noticeBuilder.heldAt(IN_CHAMBERS);
    }

    @Override
    protected void setGeneratedDateIfRequired(SscsCaseData caseData, EventType eventType) {
        //Don't need to set anything for the generated date
    }
}
