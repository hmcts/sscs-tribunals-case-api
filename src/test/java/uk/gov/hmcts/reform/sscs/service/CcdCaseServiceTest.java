package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.exception.GetCaseException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@ExtendWith(MockitoExtension.class)
class CcdCaseServiceTest {

    private static final long CASE_ID = 1625080769409918L;
    private static final long MISSING_CASE_ID = 99250807409918L;
    private static final String INVALID_CASE_ID = "pq7409918";
    private static final String SUMMARY = "Update Summary";
    private static final String DESCRIPTION = "Update Description";

    @InjectMocks
    private CcdCaseService ccdCaseService;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @Mock
    private SscsCcdConvertService sscsCcdConvertService;

    @Mock
    private CoreCaseDataApi coreCaseDataApi;

    @Test
    void getByCaseId_shouldReturnCaseDetails() throws GetCaseException {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        SscsCaseDetails expectedCaseDetails =
                SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build();
        given(ccdService.getByCaseId(eq(CASE_ID), any(IdamTokens.class))).willReturn(expectedCaseDetails);

        SscsCaseDetails caseDetails = ccdCaseService.getCaseDetails(CASE_ID);

        assertThat(expectedCaseDetails).isEqualTo(caseDetails);
    }

    @Test
    void getByCaseId_shouldReturnCaseDetailsWithString() throws GetCaseException {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        SscsCaseDetails expectedCaseDetails =
                SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build();
        given(ccdService.getByCaseId(eq(CASE_ID), any(IdamTokens.class))).willReturn(expectedCaseDetails);

        SscsCaseDetails caseDetails = ccdCaseService.getCaseDetails(String.valueOf(CASE_ID));

        assertThat(expectedCaseDetails).isEqualTo(caseDetails);
    }

    @Test
    void getByCaseId_shouldThrowGetCaseExceptionWhenNoCaseFound() {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());
        given(ccdService.getByCaseId(eq(MISSING_CASE_ID), any(IdamTokens.class))).willReturn(null);

        assertThatExceptionOfType(GetCaseException.class).isThrownBy(
                () -> ccdCaseService.getCaseDetails(MISSING_CASE_ID));
    }

    @Test
    void getByCaseId_shouldThrowGetCaseExceptionWhenStringNotLong() {
        assertThatExceptionOfType(NumberFormatException.class).isThrownBy(
                () -> ccdCaseService.getCaseDetails(INVALID_CASE_ID));
    }

    @Test
    void updateCase_shouldUpdateCaseDetails() throws UpdateCaseException {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());
        SscsCaseDetails expectedCaseDetails =
                SscsCaseDetails.builder()
                        .data(SscsCaseData.builder()
                                .ccdCaseId(String.valueOf(CASE_ID)).build()).build();
        given(ccdService.updateCase(
                any(SscsCaseData.class), eq(CASE_ID), anyString(), anyString(), anyString(), any(IdamTokens.class)))
                .willReturn(expectedCaseDetails);

        SscsCaseDetails caseDetails = ccdCaseService.updateCaseData(
                expectedCaseDetails.getData(), EventType.READY_TO_LIST, SUMMARY, DESCRIPTION);

        assertThat(expectedCaseDetails).isEqualTo(caseDetails);
    }

    @Test
    void updateCase_shouldThrowUpdateCaseExceptionWhenCaseUpdateFails() {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());
        Request request = Request.create(Request.HttpMethod.GET, "url",
                new HashMap<>(), null, new RequestTemplate());

        given(ccdService.updateCase(
                any(SscsCaseData.class), eq(CASE_ID), anyString(), anyString(), anyString(), any(IdamTokens.class)))
                .willThrow(
                        new FeignException.InternalServerError("Test Error", request, null, null));

        SscsCaseDetails testCaseDetails =
                SscsCaseDetails.builder()
                        .data(SscsCaseData.builder()
                                .ccdCaseId(String.valueOf(CASE_ID)).build()).build();

        assertThatExceptionOfType(UpdateCaseException.class).isThrownBy(
                () -> ccdCaseService.updateCaseData(
                        testCaseDetails.getData(), EventType.READY_TO_LIST, SUMMARY, DESCRIPTION));
    }

    @ParameterizedTest
    @MethodSource("emptyCaseArguments")
    void getCasesViaElastic_noCases(List<CaseDetails> cases) throws UpdateCaseException {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        SearchResult searchResult = SearchResult.builder().cases(cases).total(0).build();

        given(coreCaseDataApi.searchCases(any(), any(), any(), any()))
            .willReturn(searchResult);

        List<SscsCaseDetails> result = ccdCaseService.getCasesViaElastic(List.of("1234"));

        assertThat(result).isEmpty();
    }

    @Test
    void getCasesViaElastic_noResult() throws UpdateCaseException {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        List<SscsCaseDetails> result = ccdCaseService.getCasesViaElastic(List.of("1234"));

        assertThat(result).isEmpty();
    }

    private static Stream<Arguments> emptyCaseArguments() {
        return Stream.of(
            null,
            Arguments.of(new ArrayList<>())
        );
    }

    @Test
    void getCasesViaElastic() throws UpdateCaseException {
        Long id = 1L;
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        CaseDetails details = CaseDetails.builder().id(id).build();

        SearchResult searchResult = SearchResult.builder()
            .cases(List.of(details))
            .total(1)
            .build();

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().id(id).build();

        given(coreCaseDataApi.searchCases(any(), any(), any(), any()))
            .willReturn(searchResult);

        given(sscsCcdConvertService.getCaseDetails(details)).willReturn(sscsCaseDetails);

        List<SscsCaseDetails> result = ccdCaseService.getCasesViaElastic(List.of(id.toString()));

        assertThat(result).hasSize(1);

        assertThat(result.get(0)).isEqualTo(sscsCaseDetails);
    }
}
