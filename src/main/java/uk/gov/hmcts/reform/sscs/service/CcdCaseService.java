package uk.gov.hmcts.reform.sscs.service;

import feign.FeignException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
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
import uk.gov.hmcts.reform.sscs.model.HearingEvent;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;

@Slf4j
@Service
public class CcdCaseService {

    @Value("${core_case_data.caseTypeId}")
    private String caseType;
    public static final String CASE_ID_TERM = "reference.keyword";

    private final CcdService ccdService;
    private final IdamService idamService;
    private final SscsCcdConvertService sscsCcdConvertService;
    private final CoreCaseDataApi coreCaseDataApi;

    @Autowired
    public CcdCaseService(CcdService ccdService, IdamService idamService, CoreCaseDataApi coreCaseDataApi,
                          SscsCcdConvertService sscsCcdConvertService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.coreCaseDataApi = coreCaseDataApi;
        this.sscsCcdConvertService = sscsCcdConvertService;
    }

    public SscsCaseDetails getCaseDetails(String caseId) throws GetCaseException {
        return getCaseDetails(parseCaseId(caseId));
    }

    public SscsCaseDetails getCaseDetails(long caseId) throws GetCaseException {

        log.info("Retrieving case details using Case id : {}",
                caseId);

        IdamTokens idamTokens = idamService.getIdamTokens();

        SscsCaseDetails caseDetails = ccdService.getByCaseId(caseId, idamTokens);

        if (caseDetails == null) {
            String cause = String.format("The case data for Case id: %s could not be found", caseId);
            GetCaseException exc = new GetCaseException(cause);
            log.error(cause, exc);
            throw exc;
        }
        return caseDetails;
    }

    public SscsCaseDetails getStartEventResponse(long caseId, EventType eventType) {
        IdamTokens idamTokens = idamService.getIdamTokens();

        return ccdService.getCaseForModification(caseId, idamTokens, eventType.getCcdType());
    }

    public SscsCaseDetails updateCaseData(SscsCaseData caseData, HearingWrapper wrapper, HearingEvent event)
            throws UpdateCaseException {
        long caseId = parseCaseId(caseData.getCcdCaseId());
        IdamTokens idamTokens = idamService.getIdamTokens();

        try {
            String ccdType = event.getEventType().getCcdType();
            log.info("Updating case id {} with ccdType {}", caseId, ccdType);
            return ccdService.updateCase(caseData, caseId, wrapper.getEventId(), wrapper.getEventToken(),
                    ccdType, event.getSummary(), event.getDescription(), idamTokens);
        } catch (FeignException e) {
            UpdateCaseException exc = new UpdateCaseException(
                    String.format("The case with Case id: %s could not be updated with status %s, %s",
                            caseId, e.status(), e));
            log.error(exc.getMessage(), exc);
            throw exc;
        }
    }

    public SscsCaseDetails updateCaseData(SscsCaseData caseData, EventType event, String summary, String description)
            throws UpdateCaseException {

        long caseId = parseCaseId(caseData.getCcdCaseId());

        log.info("Updating case data using Case id : {}", caseId);

        IdamTokens idamTokens = idamService.getIdamTokens();

        try {
            return ccdService.updateCase(caseData, caseId, event.getCcdType(), summary, description, idamTokens);
        } catch (FeignException e) {
            UpdateCaseException exc = new UpdateCaseException(
                    String.format("The case with Case id: %s could not be updated with status %s, %s",
                            caseId, e.status(), e));
            log.error(exc.getMessage(), exc);
            throw exc;
        }
    }

    private long parseCaseId(String caseId) {
        try {
            return Long.parseLong(caseId);
        } catch (NumberFormatException e) {
            log.error("Invalid case id {} should be in long format", caseId);
            throw e;
        }
    }

    public List<SscsCaseDetails> getCasesViaElastic(List<String> caseReferences) {
        SearchSourceBuilder bulkCaseSearch = SearchSourceBuilder.searchSource()
                .query(QueryBuilders.termsQuery(CASE_ID_TERM, caseReferences));

        SearchResult result = searchCases(idamService.getIdamTokens(), bulkCaseSearch);

        return Optional.ofNullable(result)
                .map(SearchResult::getCases)
                .orElse(new ArrayList<>())
                .stream()
                .map(sscsCcdConvertService::getCaseDetails)
                .collect(Collectors.toList());
    }

    private SearchResult searchCases(IdamTokens idamTokens, SearchSourceBuilder searchBuilder) {
        return coreCaseDataApi.searchCases(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                caseType,
                searchBuilder.toString());
    }
}
