package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing.FACE_TO_FACE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing.PAPER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingVenue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;

class AdjournCaseAboutToSubmitHandlerTest extends AdjournCaseAboutToSubmitHandlerTestBase {

    @BeforeEach
    void setUpMocks() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @DisplayName("Given draft adjournment notice already exists on case, then overwrite existing draft")
    @Test
    void givenAdjournmentNoticeAlreadyExistsOnCase_thenOverwriteExistingDraft() {
        SscsDocument doc = SscsDocument.builder().value(
                SscsDocumentDetails.builder()
                    .documentFileName(OLD_DRAFT_DOC)
                    .documentType(DRAFT_ADJOURNMENT_NOTICE.getValue())
                    .build())
            .build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(doc);
        sscsCaseData.setSscsDocument(docs);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(previewDocumentService, times(1)).writePreviewDocumentToSscsInternalDocument(
            sscsCaseData, DRAFT_ADJOURNMENT_NOTICE, null);
    }

    @DisplayName("Given type of next hearing is not face to face, then next hearing venue selected is cleared")
    @Test
    void givenTypeOfNextHearingNotFaceToFace_thenNextHearingVenueSelectedIsCleared() {
        sscsCaseData.getAdjournment().setTypeOfNextHearing(PAPER);
        sscsCaseData.getAdjournment().setNextHearingVenueSelected(new DynamicList("someVenue"));

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getAdjournment().getNextHearingVenueSelected()).isNull();
    }

    @DisplayName("Given type of next hearing is face to face but venue is not somewhere else, then next hearing venue selected is cleared")
    @Test
    void givenFaceToFaceHearingWithVenueNotSomewhereElse_thenNextHearingVenueSelectedIsCleared() {
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingVenue(AdjournCaseNextHearingVenue.SAME_VENUE);
        sscsCaseData.getAdjournment().setNextHearingVenueSelected(new DynamicList("someVenue"));
        Appellant appellant = new Appellant();
        appellant.setAddress(Address.builder().build());
        sscsCaseData.getAppeal().setAppellant(appellant);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getAdjournment().getNextHearingVenueSelected()).isNull();
    }

    @DisplayName("Given type of next hearing is face to face and venue is somewhere else, then next hearing venue selected is retained")
    @Test
    void givenFaceToFaceHearingWithVenueSomewhereElse_thenNextHearingVenueSelectedIsRetained() {
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingVenue(AdjournCaseNextHearingVenue.SOMEWHERE_ELSE);
        final DynamicList selectedVenue = new DynamicList("someVenue");
        sscsCaseData.getAdjournment().setNextHearingVenueSelected(selectedVenue);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getAdjournment().getNextHearingVenueSelected()).isEqualTo(selectedVenue);
    }

    @DisplayName("Given type of next hearing is paper, then interpreter language and requirement are cleared")
    @Test
    void givenTypeOfNextHearingIsPaper_thenInterpreterLanguageAndRequiredAreCleared() {
        sscsCaseData.getAdjournment().setTypeOfNextHearing(PAPER);
        sscsCaseData.getAdjournment().setInterpreterLanguage(new DynamicList("someLanguage"));
        sscsCaseData.getAdjournment().setInterpreterRequired(YES);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getAdjournment().getInterpreterLanguage()).isNull();
        assertThat(sscsCaseData.getAdjournment().getInterpreterRequired()).isEqualTo(NO);
    }

    @DisplayName("Given type of next hearing is not paper, then interpreter language and requirement are retained")
    @Test
    void givenTypeOfNextHearingIsNotPaper_thenInterpreterLanguageAndRequiredAreRetained() {
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingVenue(AdjournCaseNextHearingVenue.SOMEWHERE_ELSE);
        final DynamicList interpreterLanguage = new DynamicList("someLanguage");
        sscsCaseData.getAdjournment().setInterpreterLanguage(interpreterLanguage);
        sscsCaseData.getAdjournment().setInterpreterRequired(YES);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getAdjournment().getInterpreterLanguage()).isEqualTo(interpreterLanguage);
        assertThat(sscsCaseData.getAdjournment().getInterpreterRequired()).isEqualTo(YES);
    }

}
