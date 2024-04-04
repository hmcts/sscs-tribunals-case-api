package uk.gov.hmcts.reform.sscs.functional.mya;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class CitizenLoginTest extends BaseFunctionTest {

    CreatedCcdCase ccdCase;
    String userEmail;

    @Before
    public void setup() throws IOException {
        userEmail = createRandomEmail();
        idamTestApiRequests.createUser(userEmail);
        ccdCase = createCcdCase(userEmail);
    }

    @Test
    public void checkUserDoesNotHaveCaseAssignCaseAndCheckUserHasCase() throws IOException, InterruptedException {
        String appellantTya = ccdCase.getAppellantTya();

        JSONArray onlineHearingForTya = sscsMyaBackendRequests.getOnlineHearingForCitizen(appellantTya, userEmail);
        assertThat(onlineHearingForTya.length(), is(0));

        // Give ES time to index
        Thread.sleep(5000L);

        JSONObject jsonObject = sscsMyaBackendRequests.assignCaseToUser(appellantTya, userEmail, "TN32 6PL");
        Long expectedCaseId = Long.valueOf(ccdCase.getCaseId());
        assertThat(jsonObject.getLong("case_id"), is(expectedCaseId));

        // Wait for above request to finish
        Thread.sleep(5000L);

        onlineHearingForTya = sscsMyaBackendRequests.getOnlineHearingForCitizen("", userEmail);
        assertThat(onlineHearingForTya.length(), is(1));
        assertThat(onlineHearingForTya.getJSONObject(0).get("case_id"), is(expectedCaseId));
    }

    @Test
    public void checkJointDoesNotHaveCaseAssignCaseAndCheckUserHasCase() throws IOException, InterruptedException {

        String jointPartyTya = ccdCase.getJointPartyTya();

        JSONArray onlineHearingForTya = sscsMyaBackendRequests.getOnlineHearingForCitizen(jointPartyTya, userEmail);
        assertThat(onlineHearingForTya.length(), is(0));

        // Give ES time to index
        Thread.sleep(5000L);

        JSONObject jsonObject = sscsMyaBackendRequests.assignCaseToUser(jointPartyTya, userEmail, "TN32 6PL");
        Long expectedCaseId = Long.valueOf(ccdCase.getCaseId());
        assertThat(jsonObject.getLong("case_id"), is(expectedCaseId));

        // Wait for above request to finish
        Thread.sleep(5000L);


        onlineHearingForTya = sscsMyaBackendRequests.getOnlineHearingForCitizen("", userEmail);
        assertThat(onlineHearingForTya.length(), is(1));
        assertThat(onlineHearingForTya.getJSONObject(0).get("case_id"), is(expectedCaseId));
    }

    @Test
    public void logUserWithCase_returnsNoContent() throws IOException, InterruptedException {

        // Give ES time to index
        Thread.sleep(6000L);

        sscsMyaBackendRequests.assignCaseToUser(ccdCase.getAppellantTya(), userEmail, "TN32 6PL");

        // Wait for above request to finish
        Thread.sleep(6000L);

        Long caseId = Long.valueOf(ccdCase.getCaseId());
        sscsMyaBackendRequests.logUserWithCase(caseId);
    }
}
