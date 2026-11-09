package uk.gov.hmcts.reform.sscs.callback;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingSubtype;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.CcdCaseService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApi;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration")
@TestPropertySource(locations = "classpath:config/application_it.properties")
class ReadyToListIt extends AbstractEventIt {

    private static final String CASE_ID = "1234567890123456";
    private static final String PIP_NEW_CLAIM_BENEFIT_CODE = "002";
    private static final String ISSUE_CODE_DD = "DD";
    private static final long ASYNC_TIMEOUT_MILLIS = 10_000L;
    private static final String CHILD_SUPPORT = "016";
    private static final String ISSUE_CODE_OX = "OX";

    @MockitoBean
    private IdamService idamService;

    @MockitoBean
    private CcdCaseService ccdCaseService;

    @MockitoBean
    private UpdateCcdCaseService updateCcdCaseService;

    @MockitoBean
    private HmcHearingApi hmcHearingApi;

    static Stream<Arguments> hearingDurations() {
        return Stream.of(
            Arguments.of("pip - face to face without interpreter", faceToFaceHearingOptions(YesNo.NO.toString(), null),
                faceToFaceSubtype(), 75, HearingChannel.FACE_TO_FACE, PIP_NEW_CLAIM_BENEFIT_CODE, ISSUE_CODE_DD),
            Arguments.of("pip - face to face with interpreter", faceToFaceHearingOptions(YesNo.YES.toString(), "Lithuanian"),
                faceToFaceSubtype(), 105, HearingChannel.FACE_TO_FACE, PIP_NEW_CLAIM_BENEFIT_CODE, ISSUE_CODE_DD),
            Arguments.of("pip - paper", paperHearingOptions(), HearingSubtype.builder().build(), 45, HearingChannel.PAPER,
                PIP_NEW_CLAIM_BENEFIT_CODE, ISSUE_CODE_DD),
            Arguments.of("child support - face to face without interpreter", faceToFaceHearingOptions(YesNo.NO.toString(), null),
                faceToFaceSubtype(), null, HearingChannel.FACE_TO_FACE, CHILD_SUPPORT, ISSUE_CODE_OX),
            Arguments.of("child support - face to face with interpreter",
                faceToFaceHearingOptions(YesNo.YES.toString(), "Lithuanian"), faceToFaceSubtype(), null,
                HearingChannel.FACE_TO_FACE, CHILD_SUPPORT, ISSUE_CODE_OX),
            Arguments.of("child support - paper", paperHearingOptions(), HearingSubtype.builder().build(), 45,
                HearingChannel.PAPER, CHILD_SUPPORT, ISSUE_CODE_OX));
    }

    @BeforeEach
    void setUp() throws Exception {
        setup();
        when(idamService.getIdamTokens()).thenReturn(
            IdamTokens.builder().idamOauth2Token("token").serviceAuthorization("s2s").build());
        when(hmcHearingApi.createHearingRequest(any(), any(), any(), any(), any(), any())).thenReturn(
            HmcUpdateResponse.builder().hearingRequestId(2000012345L).versionNumber(1L).build());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("hearingDurations")
    void givenPipCase_whenReadyToList_thenCreateHearingRequestSentWithReferenceDataDuration(final String scenario,
        final HearingOptions hearingOptions, final HearingSubtype hearingSubtype, final Integer expectedDuration,
        final HearingChannel expectedChannel, final String benefitCode, final String issueCode) throws Exception {
        final SscsCaseData caseData = createCaseData(hearingOptions, hearingSubtype, benefitCode, issueCode);
        when(ccdCaseService.getStartEventResponse(anyLong(), any())).thenReturn(SscsCaseDetails
            .builder()
            .id(Long.parseLong(CASE_ID))
            .state(State.READY_TO_LIST.getId())
            .data(createCaseData(hearingOptions, hearingSubtype, benefitCode, issueCode))
            .build());
        setJson(caseData, EventType.READY_TO_LIST);

        final PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(CallbackType.ABOUT_TO_SUBMIT);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getSchedulingAndListingFields().getHearingRoute()).isEqualTo(HearingRoute.LIST_ASSIST);
        assertThat(response.getData().getSchedulingAndListingFields().getHearingState()).isEqualTo(HearingState.CREATE_HEARING);

        final ArgumentCaptor<HearingRequestPayload> payloadCaptor = ArgumentCaptor.forClass(HearingRequestPayload.class);

        if (nonNull(expectedDuration)) {
            verify(hmcHearingApi, timeout(ASYNC_TIMEOUT_MILLIS)).createHearingRequest(any(), any(), any(), any(), any(),
                payloadCaptor.capture());
            verify(updateCcdCaseService, timeout(ASYNC_TIMEOUT_MILLIS)).updateCaseV2(eq(Long.parseLong(CASE_ID)), any(), any(),
                any(), any(), any());
            final HearingRequestPayload payload = payloadCaptor.getValue();
            assertThat(payload.getCaseDetails().getCaseId()).isEqualTo(CASE_ID);
            assertThat(payload.getHearingDetails().getDuration()).isEqualTo(expectedDuration);
            assertThat(payload.getHearingDetails().getHearingChannels()).containsExactly(expectedChannel);
            assertThat(payload.getHearingDetails().getHearingLocations()).isNotEmpty();
        } else {
            verify(hmcHearingApi, never()).createHearingRequest(any(), any(), any(), any(), any(), payloadCaptor.capture());
            verify(updateCcdCaseService, never()).updateCaseV2(eq(Long.parseLong(CASE_ID)), any(), any(), any(), any(), any());
        }

    }

    private static SscsCaseData createCaseData(final HearingOptions hearingOptions, final HearingSubtype hearingSubtype,
        String benefitCode, String issueCode) {
        return SscsCaseData
            .builder()
            .ccdCaseId(CASE_ID)
            .state(State.WITH_DWP)
            .region("CARDIFF")
            .processingVenue("Cardiff")
            .benefitCode(benefitCode)
            .issueCode(issueCode)
            .regionalProcessingCenter(RegionalProcessingCenter
                .builder()
                .name("CARDIFF")
                .postcode("CF24 0AB")
                .epimsId("372653")
                .hearingRoute(HearingRoute.LIST_ASSIST)
                .build())
            .appeal(Appeal
                .builder()
                .hearingType(Boolean.TRUE.equals(
                    hearingOptions.isWantsToAttendHearing()) ? HearingType.ORAL.getValue() : HearingType.PAPER.getValue())
                .hearingOptions(hearingOptions)
                .hearingSubtype(hearingSubtype)
                .appellant(Appellant
                    .builder()
                    .id("1")
                    .name(Name.builder().title("Mr").firstName("Joe").lastName("Bloggs").build())
                    .identity(Identity.builder().nino("AB123456C").dob("1980-01-01").build())
                    .address(Address.builder().line1("1 Test Street").town("Cardiff").postcode("CF10 1AA").build())
                    .build())
                .build())
            .build();
    }

    private static HearingOptions faceToFaceHearingOptions(final String languageInterpreter, final String language) {
        return HearingOptions
            .builder()
            .wantsToAttend(YesNo.YES.toString())
            .languageInterpreter(languageInterpreter)
            .languages(language)
            .build();
    }

    private static HearingOptions paperHearingOptions() {
        return HearingOptions.builder().wantsToAttend(YesNo.NO.toString()).build();
    }

    private static HearingSubtype faceToFaceSubtype() {
        return HearingSubtype.builder().wantsHearingTypeFaceToFace(YesNo.YES.toString()).build();
    }
}
