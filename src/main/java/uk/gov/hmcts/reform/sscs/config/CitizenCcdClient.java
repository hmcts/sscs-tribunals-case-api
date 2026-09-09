package uk.gov.hmcts.reform.sscs.config;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CaseAccessApi;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.ccd.client.model.UserId;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class CitizenCcdClient {

    private final CcdRequestDetails ccdRequestDetails;
    private final CoreCaseDataApi coreCaseDataApi;
    private final CaseAccessApi caseAccessApi;
    private final boolean elasticSearchEnabled;
    private static final List<String> EXCLUDED_STATES_FOR_ACTIVE_CASES =
            List.of(State.DORMANT_APPEAL_STATE.getId(), State.VOID_STATE.getId());
    private static final int MYA_MAX_CASES_PER_PAGE = 200;


    @Autowired
    CitizenCcdClient(CcdRequestDetails ccdRequestDetails,
                     CoreCaseDataApi coreCaseDataApi,
                     CaseAccessApi caseAccessApi,
                     @Value("${feature.elasticsearch.enabled}") boolean elasticSearchEnabled) {
        this.ccdRequestDetails = ccdRequestDetails;
        this.coreCaseDataApi = coreCaseDataApi;
        this.caseAccessApi = caseAccessApi;
        this.elasticSearchEnabled = elasticSearchEnabled;
    }

    StartEventResponse startCaseForCitizen(IdamTokens idamTokens, String eventId) {
        return coreCaseDataApi.startForCitizen(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            ccdRequestDetails.getJurisdictionId(),
            ccdRequestDetails.getCaseTypeId(),
            eventId);
    }

    CaseDetails submitForCitizen(IdamTokens idamTokens, CaseDataContent caseDataContent) {
        return coreCaseDataApi.submitForCitizen(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            ccdRequestDetails.getJurisdictionId(),
            ccdRequestDetails.getCaseTypeId(),
            true,
            caseDataContent
        );
    }

    @Retryable
    public List<CaseDetails> searchForCitizen(IdamTokens idamTokens) {
        log.info("Searching cases for citizen");
        if (elasticSearchEnabled) {
            String searchCriteria = buildQuery("state", State.DRAFT.getId());
            SearchResult searchResult = coreCaseDataApi.searchCases(
                    idamTokens.getIdamOauth2Token(),
                    idamTokens.getServiceAuthorization(),
                    ccdRequestDetails.getCaseTypeId(),
                    searchCriteria);
            return ofNullable(searchResult).isEmpty() ? new ArrayList<>() : searchResult.getCases();
        } else {
            Map<String, String> searchCriteria = new HashMap<>();
            searchCriteria.put("state", State.DRAFT.getId());
            searchCriteria.put("sortDirection", "desc");
            return coreCaseDataApi.searchForCitizen(
                    idamTokens.getIdamOauth2Token(),
                    idamTokens.getServiceAuthorization(),
                    idamTokens.getUserId(),
                    ccdRequestDetails.getJurisdictionId(),
                    ccdRequestDetails.getCaseTypeId(),
                    searchCriteria
            );
        }


    }

    public List<CaseDetails> searchForCitizenAllCases(IdamTokens idamTokens) {
        if (elasticSearchEnabled) {
            String searchCriteria = "{\"query\":{\"match_all\":{}}}";
            SearchResult searchResult = coreCaseDataApi.searchCases(
                    idamTokens.getIdamOauth2Token(),
                    idamTokens.getServiceAuthorization(),
                    ccdRequestDetails.getCaseTypeId(),
                    searchCriteria);
            return ofNullable(searchResult).isEmpty() ? new ArrayList<>() : searchResult.getCases();
        } else {
            Map<String, String> searchCriteria = new HashMap<>();
            searchCriteria.put("sortDirection", "desc");
            return coreCaseDataApi.searchForCitizen(
                    idamTokens.getIdamOauth2Token(),
                    idamTokens.getServiceAuthorization(),
                    idamTokens.getUserId(),
                    ccdRequestDetails.getJurisdictionId(),
                    ccdRequestDetails.getCaseTypeId(),
                    searchCriteria
            );
        }
    }

    public List<CaseDetails> searchForCitizenAllCasesNonDormant(IdamTokens idamTokens) {
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        searchBuilder.query(QueryBuilders
                        .boolQuery()
                        .mustNot(QueryBuilders.termsQuery("state.keyword", EXCLUDED_STATES_FOR_ACTIVE_CASES)))
                .sort("last_modified", SortOrder.DESC)
                .size(MYA_MAX_CASES_PER_PAGE);
        String searchCriteria = searchBuilder.toString();
        SearchResult searchResult = coreCaseDataApi.searchCases(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                ccdRequestDetails.getCaseTypeId(),
                searchCriteria);
        return ofNullable(searchResult).map(SearchResult::getCases).orElse(emptyList());
    }

    public List<CaseDetails> searchForCitizenBasedOnEmail(IdamTokens idamToken, String email) {
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        searchBuilder.query(QueryBuilders
                        .boolQuery()
                        .should(matchQuery("data.subscriptions.appellantSubscription.email", email))
                        .should(matchQuery("data.subscriptions.appointeeSubscription.email", email))
                        .should(matchQuery("data.subscriptions.representativeSubscription.email", email))
                        .should(matchQuery("data.subscriptions.jointPartySubscription.email", email))
                        .should(matchQuery("data.otherParties.value.otherPartySubscription.email", email))
                        .should(matchQuery("data.otherParties.value.otherPartyAppointeeSubscription.email", email))
                        .should(matchQuery("data.otherParties.value.otherPartyRepresentativeSubscription.email", email))
                )
                .size(MYA_MAX_CASES_PER_PAGE);
        String searchCriteria = searchBuilder.toString();
        SearchResult searchResult = coreCaseDataApi.searchCases(
                idamToken.getIdamOauth2Token(),
                idamToken.getServiceAuthorization(),
                ccdRequestDetails.getCaseTypeId(),
                searchCriteria);
        return ofNullable(searchResult).map(SearchResult::getCases).orElse(emptyList());
    }

    CaseDetails submitEventForCitizen(IdamTokens idamTokens, String caseId, CaseDataContent caseDataContent) {
        return coreCaseDataApi.submitEventForCitizen(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            ccdRequestDetails.getJurisdictionId(),
            ccdRequestDetails.getCaseTypeId(),
            caseId,
            true,
            caseDataContent
        );
    }

    StartEventResponse startEventForCitizen(IdamTokens idamTokens, String caseId, String eventType) {
        return coreCaseDataApi.startEventForCitizen(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            ccdRequestDetails.getJurisdictionId(),
            ccdRequestDetails.getCaseTypeId(),
            caseId,
            eventType
        );
    }

    public void addUserToCase(IdamTokens idamTokens, String userIdToAdd, Long caseId) {
        caseAccessApi.grantAccessToCase(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                idamTokens.getUserId(),
                ccdRequestDetails.getJurisdictionId(),
                ccdRequestDetails.getCaseTypeId(),
                caseId.toString(),
                new UserId(userIdToAdd)
        );
    }

    public String buildQuery(String searchValue, String searchField) {
        return "{\"query\":{\"term\":{ \""
                + searchValue
                + "\":\"" + searchField + "\"}}}";
    }
}

