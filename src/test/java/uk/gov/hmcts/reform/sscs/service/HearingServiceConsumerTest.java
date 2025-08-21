package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.doThrow;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingSubtype;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingWindow;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.helper.mapping.OverridesMapping;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@ExtendWith(MockitoExtension.class)
public class HearingServiceConsumerTest {

    private static final long HEARING_REQUEST_ID = 12345;
    private static final long CASE_ID = 1625080769409918L;
    private static final String BENEFIT_CODE = "002";
    private static final String ISSUE_CODE = "DD";
    private static final String PROCESSING_VENUE = "Processing Venue";

    @Mock
    HmcUpdateResponse response;
    @Mock
    private ReferenceDataServiceHolder refData;
    @Mock
    private OverridesMapping overridesMapping;

    @InjectMocks
    private HearingServiceConsumer hearingServiceConsumer;

    private SscsCaseData caseData;

    @BeforeEach
    void setup() {
        caseData = SscsCaseData.builder()
            .ccdCaseId(String.valueOf(CASE_ID))
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .caseManagementLocation(CaseManagementLocation.builder().build())
            .adjournment(Adjournment.builder().adjournmentInProgress(YesNo.NO).nextHearingDateType(FIRST_AVAILABLE_DATE).build())
            .appeal(Appeal.builder()
                        .rep(Representative.builder().hasRepresentative("No").build())
                        .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                        .hearingType("test")
                        .hearingSubtype(HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace(
                            "yes").build())
                        .appellant(Appellant.builder()
                                       .name(Name.builder().firstName("first").lastName("surname").build())
                                       .build())
                        .build())
            .processingVenue(PROCESSING_VENUE)
            .build();

    }

    private void setupResponse() {
        given(response.getHearingRequestId()).willReturn(123L);
        given(response.getVersionNumber()).willReturn(1234L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreateHearingCaseDetailsConsumer(boolean adjournmentFlagEnabled) throws ListingException {
        setupResponse();
        willAnswer(invocation -> {
            SscsCaseData sscsCaseData = (SscsCaseData) invocation.getArguments()[0];
            sscsCaseData.getSchedulingAndListingFields()
                    .setDefaultListingValues(OverrideFields.builder()
                            .autoList(NO)
                            .hearingWindow(HearingWindow.builder().dateRangeStart(LocalDate.now()).build())
                            .appellantHearingChannel(HearingChannel.FACE_TO_FACE)
                            .appellantInterpreter(HearingInterpreter.builder().isInterpreterWanted(NO).build())
                            .hearingVenueEpimsIds(List.of(CcdValue.<CcdValue<String>>builder().value(CcdValue.<String>builder().value("219164").build()).build()))
                            .duration(0).build());
            return sscsCaseData;
        }).given(overridesMapping).setDefaultListingValues(eq(caseData), eq(refData));

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();

        Consumer<SscsCaseDetails> sscsCaseDetailsConsumer = hearingServiceConsumer
                .getCreateHearingCaseDetailsConsumerV2(
                        PanelMemberComposition.builder().build(), response,HEARING_REQUEST_ID, false);
        //reset the DLVs
        caseData.getSchedulingAndListingFields().setDefaultListingValues(null);
        caseData.setAdjournment(Adjournment.builder().adjournmentInProgress(adjournmentFlagEnabled ? YES : NO)
                                    .nextHearingDateType(FIRST_AVAILABLE_DATE)
                                    .build());

        sscsCaseDetailsConsumer.accept(sscsCaseDetails);
        //if the mutator has been applied then a defaultListingValue should have been added
        OverrideFields defaultListingValues = sscsCaseDetails.getData().getSchedulingAndListingFields().getDefaultListingValues();
        assertThat(defaultListingValues).isNotNull();
        assertThat(defaultListingValues.getDuration()).isEqualTo(0);
        assertThat(defaultListingValues.getAutoList()).isEqualTo(NO);
        assertThat(defaultListingValues.getHearingWindow().getDateRangeStart()).isNotNull();
        assertThat(defaultListingValues.getAppellantHearingChannel()).isEqualTo(HearingChannel.FACE_TO_FACE);
        assertThat(defaultListingValues.getAppellantInterpreter().getIsInterpreterWanted()).isEqualTo(NO);
        assertThat(defaultListingValues.getHearingVenueEpimsIds().get(0)).isNotNull();
        //this flag is either reset to NO from YES, or keeps at NO
        assertThat(caseData.getAdjournment().getAdjournmentInProgress()).isEqualTo(NO);

        List<Hearing> hearings = caseData.getHearings();
        assertThat(hearings).isNotEmpty();
        assertEquals(1, hearings.size()); // hearing added
        assertEquals("123", hearings.get(0).getValue().getHearingId());
        assertEquals(1234L, hearings.get(0).getValue().getVersionNumber());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreateHearingCaseDetailsConsumerWithAdjournmentFlagEnabled(boolean adjournmentInProgress) throws ListingException {
        setupResponse();
        willAnswer(invocation -> {
            SscsCaseData sscsCaseData = (SscsCaseData) invocation.getArguments()[0];
            sscsCaseData.getSchedulingAndListingFields()
                    .setDefaultListingValues(OverrideFields.builder()
                            .autoList(NO)
                            .hearingWindow(HearingWindow.builder().dateRangeStart(LocalDate.now()).build())
                            .appellantHearingChannel(HearingChannel.FACE_TO_FACE)
                            .appellantInterpreter(HearingInterpreter.builder().isInterpreterWanted(NO).build())
                            .hearingVenueEpimsIds(List.of(CcdValue.<CcdValue<String>>builder().value(CcdValue.<String>builder().value("219164").build()).build()))
                            .duration(0).build());
            return sscsCaseData;
        }).given(overridesMapping).setDefaultListingValues(eq(caseData), eq(refData));

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();
        var panelMemberComposition = PanelMemberComposition.builder().panelCompositionJudge("58").build();

        Consumer<SscsCaseDetails> sscsCaseDetailsConsumer = hearingServiceConsumer
                .getCreateHearingCaseDetailsConsumerV2(
                        panelMemberComposition, response, HEARING_REQUEST_ID, false);
        //reset the DLVs
        caseData.getSchedulingAndListingFields().setDefaultListingValues(null);
        caseData.setAdjournment(Adjournment.builder().adjournmentInProgress(adjournmentInProgress ? YES : NO)
                                    .nextHearingDateType(FIRST_AVAILABLE_DATE)
                                    .build());

        sscsCaseDetailsConsumer.accept(sscsCaseDetails);

        assertEquals(panelMemberComposition, sscsCaseDetails.getData().getPanelMemberComposition());
        //if the mutator has been applied then a defaultListingValue should have been added
        OverrideFields defaultListingValues = sscsCaseDetails.getData().getSchedulingAndListingFields().getDefaultListingValues();
        assertThat(defaultListingValues).isNotNull();
        assertThat(defaultListingValues.getDuration()).isEqualTo(0);
        assertThat(defaultListingValues.getAutoList()).isEqualTo(NO);
        assertThat(defaultListingValues.getHearingWindow().getDateRangeStart()).isNotNull();
        assertThat(defaultListingValues.getAppellantHearingChannel()).isEqualTo(HearingChannel.FACE_TO_FACE);
        assertThat(defaultListingValues.getAppellantInterpreter().getIsInterpreterWanted()).isEqualTo(NO);
        assertThat(defaultListingValues.getHearingVenueEpimsIds().get(0)).isNotNull();
        //this flag is either reset to NO from YES, or keeps at NO
        assertThat(caseData.getAdjournment().getAdjournmentInProgress()).isEqualTo(NO);

        List<Hearing> hearings = caseData.getHearings();
        assertThat(hearings).isNotEmpty();
        assertEquals(1, hearings.size()); // hearing added
        assertEquals("123", hearings.get(0).getValue().getHearingId());
        assertEquals(1234L, hearings.get(0).getValue().getVersionNumber());
    }

    @Test
    public void testCreateHearingCaseDetailsConsumerWithListingExceptionMessage() throws ListingException {
        doThrow(new ListingException("Incorrect benefit/issue code combination"))
                .when(overridesMapping).setDefaultListingValues(eq(caseData), eq(refData));
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();

        Consumer<SscsCaseDetails> sscsCaseDetailsConsumer = hearingServiceConsumer
                .getCreateHearingCaseDetailsConsumerV2(
                        PanelMemberComposition.builder().build(), response, HEARING_REQUEST_ID, false);
        //reset the DLVs
        caseData.getSchedulingAndListingFields().setDefaultListingValues(null);
        RuntimeException re = assertThrows(RuntimeException.class, () -> sscsCaseDetailsConsumer.accept(sscsCaseDetails));
        assertThat(re.getMessage()).contains("Incorrect benefit/issue code combination");
        assertThat(caseData.getSchedulingAndListingFields().getDefaultListingValues()).isNull();
    }

    @Test
    public void testCreateHearingCaseDataConsumer() {
        setupResponse();

        Consumer<SscsCaseData> sscsCaseDataConsumer = hearingServiceConsumer.getCreateHearingCaseDataConsumer(
            response,
            HEARING_REQUEST_ID
        );
        sscsCaseDataConsumer.accept(caseData);

        List<Hearing> hearings = caseData.getHearings();
        assertThat(hearings).isNotEmpty();
        assertEquals(1, hearings.size()); // hearing added
        assertEquals("123", hearings.get(0).getValue().getHearingId());
        assertEquals(1234L, hearings.get(0).getValue().getVersionNumber());
    }

    @Test
    public void testCreateHearingCaseDataConsumerWithHearing() {
        setupResponse();

        caseData.setHearings(new ArrayList<>());
        caseData.getHearings().add(Hearing.builder().value(HearingDetails.builder().hearingId(String.valueOf(HEARING_REQUEST_ID)).build()).build());
        Consumer<SscsCaseData> sscsCaseDataConsumer = hearingServiceConsumer.getCreateHearingCaseDataConsumer(
            response,
            HEARING_REQUEST_ID
        );
        sscsCaseDataConsumer.accept(caseData);

        List<Hearing> hearings = caseData.getHearings();
        assertThat(hearings).isNotEmpty();
        assertEquals(1, hearings.size()); // hearing added
        assertEquals("123", hearings.get(0).getValue().getHearingId());
        assertEquals(1234L, hearings.get(0).getValue().getVersionNumber());
    }

    @Test
    void testCreateHearingCaseDataConsumerWithHearingUpdateAndWithOverrideFields() {
        setupResponse();

        caseData.setHearings(new ArrayList<>());
        caseData.getHearings().add(Hearing.builder().value(HearingDetails.builder().hearingId(String.valueOf(HEARING_REQUEST_ID)).build()).build());
        caseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();

        Consumer<SscsCaseDetails> sscsCaseDetailsConsumer = hearingServiceConsumer
                .getCreateHearingCaseDetailsConsumerV2(
                        PanelMemberComposition.builder().build(), response, HEARING_REQUEST_ID, true);
        sscsCaseDetailsConsumer.accept(sscsCaseDetails);

        List<Hearing> hearings = sscsCaseDetails.getData().getHearings();
        assertThat(hearings).isNotEmpty();
        assertEquals(1, hearings.size()); // hearing added
        assertEquals("123", hearings.get(0).getValue().getHearingId());
        assertEquals(1234L, hearings.get(0).getValue().getVersionNumber());
    }

    @Test
    void testCreateHearingCaseDataConsumerWithHearingUpdateAndWithOverrideFieldsIsNull() {
        setupResponse();

        caseData.setHearings(new ArrayList<>());
        Consumer<SscsCaseDetails> sscsCaseDetailsConsumer = hearingServiceConsumer
                .getCreateHearingCaseDetailsConsumerV2(
                        PanelMemberComposition.builder().build(), response, HEARING_REQUEST_ID, true);
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();
        sscsCaseDetailsConsumer.accept(sscsCaseDetails);

        List<Hearing> hearings = sscsCaseDetails.getData().getHearings();
        assertThat(hearings).isNotEmpty();
        assertEquals(1, hearings.size()); // hearing added
        assertEquals("123", hearings.get(0).getValue().getHearingId());
        assertEquals(1234L, hearings.get(0).getValue().getVersionNumber());
    }
}
