package uk.gov.hmcts.reform.sscs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLinkDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.GetCaseException;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.helper.mapping.HearingsCaseMapping;
import uk.gov.hmcts.reform.sscs.helper.mapping.ServiceHearingValuesMapping;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.service.ServiceHearingRequest;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.ServiceHearingValues;
import uk.gov.hmcts.reform.sscs.model.service.linkedcases.ServiceLinkedCases;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceHearingsService {

    public static final int NUM_CASES_EXPECTED = 1;

    private final CcdCaseService ccdCaseService;

    private final ReferenceDataServiceHolder refData;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final UpdateCcdCaseService updateCcdCaseService;

    private final IdamService idamService;

    @Value("${feature.update-case-only-hearing-v2.enabled}")
    private boolean updatCaseOnlyHearingV2Enabled;

    public ServiceHearingValues getServiceHearingValues(ServiceHearingRequest request)
            throws GetCaseException, UpdateCaseException, ListingException, JsonProcessingException {
        SscsCaseDetails caseDetails = ccdCaseService.getCaseDetails(request.getCaseId());

        SscsCaseData caseData = caseDetails.getData();
        String originalCaseData = objectMapper.writeValueAsString(caseData);

        ServiceHearingValues model = ServiceHearingValuesMapping.mapServiceHearingValues(caseData, refData);

        String updatedCaseData = objectMapper.writeValueAsString(caseData);

        if (!originalCaseData.equals(updatedCaseData)) {
            if (updatCaseOnlyHearingV2Enabled) {
                log.debug("Updating case V2 data with Service Hearing Values for Case ID {}", caseData.getCcdCaseId());
                Long currentCaseId = Long.parseLong(caseData.getCcdCaseId());
                updateCcdCaseService.updateCaseV2(currentCaseId,
                        EventType.UPDATE_CASE_ONLY.getType(),
                        "Updating caseDetails IDs",
                        "IDs updated for caseDetails due to ServiceHearingValues request",
                        idamService.getIdamTokens(),
                        sscsCaseDetails -> caseDetails.getData());

            } else {
                log.debug("Updating case data with Service Hearing Values for Case ID {}", caseData.getCcdCaseId());
                ccdCaseService.updateCaseData(
                        caseData,
                        EventType.UPDATE_CASE_ONLY,
                        "Updating caseDetails IDs",
                        "IDs updated for caseDetails due to ServiceHearingValues request");
            }
        }

        return model;
    }

    public List<ServiceLinkedCases> getServiceLinkedCases(ServiceHearingRequest request)
            throws GetCaseException {

        String caseId = request.getCaseId();
        List<SscsCaseDetails> mainCaseData = ccdCaseService.getCasesViaElastic(List.of(request.getCaseId()));

        if (mainCaseData == null || mainCaseData.size() != NUM_CASES_EXPECTED) {
            throw new GetCaseException(
                    "Invalid search data returned: one case is required. Attempted to fetch data for " + caseId);
        }

        SscsCaseData caseData = mainCaseData.get(0).getData();

        List<String> linkedReferences = Optional.ofNullable(caseData.getLinkedCase())
                .orElseGet(Collections::emptyList).stream()
                .filter(Objects::nonNull)
                .map(CaseLink::getValue)
                .filter(Objects::nonNull)
                .map(CaseLinkDetails::getCaseReference)
                .collect(Collectors.toList());

        log.info("{} linked case references found for case: {}", linkedReferences.size(), caseId);

        List<SscsCaseDetails> linkedCases = ccdCaseService.getCasesViaElastic(linkedReferences);

        return linkedCases.stream().map(linkedCase ->
                        ServiceLinkedCases.builder()
                                .caseReference(linkedCase.getId().toString())
                                .caseName(linkedCase.getData().getCaseAccessManagementFields().getCaseNamePublic())
                                .reasonsForLink(HearingsCaseMapping.getReasonsForLink(caseData))
                                .build())
                .collect(Collectors.toList());
    }
}
