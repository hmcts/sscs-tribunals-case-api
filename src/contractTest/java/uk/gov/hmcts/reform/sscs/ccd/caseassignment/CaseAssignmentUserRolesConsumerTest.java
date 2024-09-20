package uk.gov.hmcts.reform.sscs.ccd.caseassignment;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi.SERVICE_AUTHORIZATION;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRole;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;

@ExtendWith(PactConsumerTestExt.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@PactTestFor(providerName = "ccdDataStoreAPI_Cases", port = "8891")
@PactDirectory("pacts")
@SpringBootTest
@TestPropertySource(locations = {"/config/application_contract.properties"})
public class CaseAssignmentUserRolesConsumerTest {

    @Autowired
    private CaseAssignmentApi caseAssignmentApi;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SOME_AUTHORIZATION_TOKEN = "Bearer UserAuthToken";

    private static final String SERVICE_AUTHORIZATION_TOKEN = "Auth Bearer ServiceAuthToken";

    private static final String caseId = "1";

    private static final String userId = "1234";

    private static final String caseRole = "role";

    @Pact(provider = "ccdDataStoreAPI_Cases", consumer = "sscs_tribunalsCaseApi")
    public V4Pact generatePactForGetUserRoles(PactBuilder builder) throws JSONException, JsonProcessingException {
        return builder
                .usingLegacyDsl()
                .given("user roles are requested")
                .uponReceiving("A request for a User roles from SSCS Tribunals API")
                .path("/case-users")
                .method("GET")
                .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN, SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .matchQuery("case_ids", caseId)
                .willRespondWith()
                .status(200)
                .body(generateResponse())
                .toPact(V4Pact.class);
    }

    private String generateResponse() throws JsonProcessingException {
        var caseAssignmentUserRolesResource = CaseAssignmentUserRolesResource.builder()
                .caseAssignmentUserRoles(List.of(
                        CaseAssignmentUserRole.builder()
                                .caseDataId(caseId)
                                .userId(userId)
                                .caseRole(caseRole)
                                .build()
                ))
                .build();
        return objectMapper.writeValueAsString(caseAssignmentUserRolesResource);
    }

    @Test
    @PactTestFor(pactMethod = "generatePactForGetUserRoles")
    public void verifyUserRolesPact() {
        CaseAssignmentUserRolesResource caseAssignmentUserRolesResource = caseAssignmentApi.getUserRoles(
                SOME_AUTHORIZATION_TOKEN,
                SERVICE_AUTHORIZATION_TOKEN,
                List.of(caseId));

        List<CaseAssignmentUserRole> caseAssignmentUserRoles = caseAssignmentUserRolesResource.getCaseAssignmentUserRoles();
        assertNotNull(caseAssignmentUserRoles);
        assertEquals(1, caseAssignmentUserRoles.size());
        CaseAssignmentUserRole userRole = caseAssignmentUserRoles.get(0);
        assertEquals(caseId, userRole.getCaseDataId());
        assertEquals(userId, userRole.getUserId());
        assertEquals(caseRole, userRole.getCaseRole());
    }

}
