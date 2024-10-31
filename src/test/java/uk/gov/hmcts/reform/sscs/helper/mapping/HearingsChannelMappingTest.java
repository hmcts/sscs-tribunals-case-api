package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.FACE_TO_FACE;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.PAPER;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.TELEPHONE;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;
import uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil;


@ExtendWith(MockitoExtension.class)
class HearingsChannelMappingTest {

    @Mock
    private ReferenceDataServiceHolder refData;

    private SscsCaseData caseData;

    private List<CcdValue<OtherParty>> otherParties;

    @BeforeEach
    void setUp() {
        Name name = Name.builder()
            .title("title")
            .firstName("first")
            .lastName("last")
            .build();

        HearingSubtype hearingSubtype = HearingSubtype.builder()
            .wantsHearingTypeTelephone(YES.getValue())
            .wantsHearingTypeVideo(YES.getValue())
            .wantsHearingTypeFaceToFace(YES.getValue())
            .hearingTelephoneNumber("1234")
            .hearingVideoEmail("email")
            .build();

        HearingOptions hearingOptions = HearingOptions.builder()
            .wantsToAttend(YES.getValue())
            .build();

        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .hearingOptions(hearingOptions)
                .hearingSubtype(hearingSubtype)
                .appellant(Appellant.builder()
                    .name(name)
                    .build())
                .build())
            .dwpIsOfficerAttending(NO.getValue())
            .build();

        otherParties = new ArrayList<>();
        otherParties.add(new CcdValue<>(OtherParty.builder()
            .hearingOptions(hearingOptions)
            .hearingSubtype(hearingSubtype)
            .name(name)
            .build()));
    }

    @DisplayName("When a override hearing channel is given getIndividualPreferredHearingChannel "
        + "returns that hearing channel")
    @ParameterizedTest
    @EnumSource(value = HearingChannel.class)
    void testGetIndividualPreferredHearingChannel(HearingChannel value) {
        HearingSubtype hearingSubtype = HearingSubtype.builder().build();
        HearingOptions hearingOptions = HearingOptions.builder().build();
        OverrideFields overrideFields = OverrideFields.builder()
            .appellantHearingChannel(value)
            .build();
        HearingChannel result = HearingChannelUtil.getIndividualPreferredHearingChannel(
            hearingSubtype, hearingOptions, overrideFields);

        assertThat(result).isEqualTo(value);
    }

    @DisplayName("When a null appellant Hearing Channel is given getIndividualPreferredHearingChannel "
        + "returns the valid hearing channel")
    @Test
    void testGetIndividualPreferredHearingChannel() {
        HearingSubtype hearingSubtype = HearingSubtype.builder()
            .wantsHearingTypeFaceToFace(YES.getValue())
            .build();
        HearingOptions hearingOptions = HearingOptions.builder()
            .wantsToAttend(YES.getValue())
            .build();
        OverrideFields overrideFields = OverrideFields.builder()
            .appellantHearingChannel(null)
            .build();
        HearingChannel result = HearingChannelUtil.getIndividualPreferredHearingChannel(
            hearingSubtype, hearingOptions, overrideFields);

        assertThat(result).isEqualTo(FACE_TO_FACE);
    }

    @DisplayName("The resolved hearing channel should follow the hierarchy face to face > video > telephone")
    @ParameterizedTest
    @CsvSource(value = {
        "YES,NO,NO,FACE_TO_FACE",
        "YES,YES,YES,FACE_TO_FACE",
        "NO,YES,NO,VIDEO",
        "NO,YES,YES,VIDEO",
        "NO,NO,YES,TELEPHONE"
    })
    void getHearingChannels_whenOneOfTheParties_containsFaceToFace_selectFaceToFace_asPreferredValue(
        String faceToFace,
        String video,
        String telephone,
        HearingChannel resolvedHearingChannel
    ) {
        HearingSubtype hearingSubtype = caseData.getAppeal().getHearingSubtype();
        hearingSubtype.setWantsHearingTypeFaceToFace(faceToFace);
        hearingSubtype.setWantsHearingTypeVideo(video);
        hearingSubtype.setWantsHearingTypeTelephone(telephone);

        HearingSubtype otherPartiesHearingSubtype = otherParties.get(0).getValue().getHearingSubtype();
        otherPartiesHearingSubtype.setWantsHearingTypeFaceToFace(faceToFace);
        otherPartiesHearingSubtype.setWantsHearingTypeVideo(video);
        otherPartiesHearingSubtype.setWantsHearingTypeTelephone(telephone);
        caseData.setOtherParties(otherParties);

        List<HearingChannel> result = HearingsChannelMapping.getHearingChannels(caseData);
        assertThat(result)
            .hasSize(1)
            .containsOnly(resolvedHearingChannel);
    }

    @DisplayName("should throw HearingChannelNotFoundException if no party has a preference selected "
        + "but want to attend.")
    @Test
    void getHearingChannels_ifNoPartiesHaveAPreferenceSelected_throwException() {
        caseData.getAppeal().setHearingSubtype(null);

        List<HearingChannel> hearingChannels = HearingsChannelMapping.getHearingChannels(caseData);

        assertThat(hearingChannels)
            .hasSize(1)
            .containsOnly(PAPER);
    }

    @DisplayName("should return not attending if selected on the appeal")
    @Test
    void getHearingChannels_hearingOptionsPaper() {
        caseData.getAppeal().getHearingOptions().setWantsToAttend(NO.getValue());

        List<HearingChannel> result = HearingsChannelMapping.getHearingChannels(caseData);

        assertThat(result)
            .hasSize(1)
            .containsOnly(PAPER);
    }

    @DisplayName("should return face to face if wants to attend but no options selected")
    @Test
    void getHearingChannels_wantsToAttendWithNoHearingsSelection() {
        caseData.getAppeal().getHearingOptions().setWantsToAttend(YES.getValue());

        List<HearingChannel> result = HearingsChannelMapping.getHearingChannels(caseData);

        assertThat(result)
            .hasSize(1)
            .containsOnly(FACE_TO_FACE);
    }

    @DisplayName("When no one wants to attend, isPaperCase returns True")
    @Test
    void testIsPaperCaseNoOneAttend() {
        caseData.getAppeal().getHearingOptions().setWantsToAttend(NO.getValue());

        boolean result = HearingChannelUtil.isPaperCase(caseData);

        assertThat(result).isTrue();
    }

    @DisplayName("When someone wants to attend, isPaperCase returns False")
    @Test
    void testIsPaperCaseAttending() {
        boolean result = HearingChannelUtil.isPaperCase(caseData);

        assertThat(result).isFalse();
    }

    @DisplayName("When adjournment flag is enabled and adjournment is in progress returns next hearing channel")
    @Test
    void getHearingChannels_ifAdjournmentFlagEnabled_and_AdjournmentInProgress_getNextHearing() {

        caseData.getAppeal().getHearingOptions().setWantsToAttend(NO.getValue());
        caseData.setAdjournment(Adjournment.builder()
            .typeOfNextHearing(AdjournCaseTypeOfHearing.TELEPHONE)
            .adjournmentInProgress(YES)
            .build());

        List<HearingChannel> result = HearingsChannelMapping.getHearingChannels(caseData, true);
        assertThat(result)
            .hasSize(1)
            .containsOnly(TELEPHONE);
    }

    @DisplayName("When adjournment flag is false, returns hearing channel from the case")
    @Test
    void getHearingChannels_ifAdjournmentDisabled_returnDefaultHearingChannel() {

        caseData.getAppeal().getHearingOptions().setWantsToAttend(NO.getValue());
        caseData.setAdjournment(Adjournment.builder()
            .typeOfNextHearing(AdjournCaseTypeOfHearing.TELEPHONE)
            .build());

        List<HearingChannel> result = HearingsChannelMapping.getHearingChannels(caseData, false);
        assertThat(result)
            .hasSize(1)
            .containsOnly(PAPER);
    }

    @DisplayName("When adjournment flag is true and adjournment next hearing is null, "
        + "returns hearing channel from the case")
    @Test
    void getHearingChannels_ifAdjournmentDisabledAndNextHearingIsNull_returnDefaultHearingChannel() {

        caseData.getAppeal().getHearingOptions().setWantsToAttend(NO.getValue());
        caseData.setAdjournment(Adjournment.builder()
            .typeOfNextHearing(null)
            .adjournmentInProgress(YES)
            .build());

        List<HearingChannel> result = HearingsChannelMapping.getHearingChannels(caseData, true);
        assertThat(result)
            .hasSize(1)
            .containsOnly(PAPER);
    }

}
