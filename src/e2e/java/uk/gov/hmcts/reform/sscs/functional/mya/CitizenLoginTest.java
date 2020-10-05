package uk.gov.hmcts.reform.sscs.functional.mya;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;


public class CitizenLoginTest extends BaseFunctionTest {
    @Test
    public void checkUserDoesNotHaveCaseAssignCaseAndCheckUserHasCase() throws IOException {
        String userEmail = createRandomEmail();
        idamTestApiRequests.createUser(userEmail);
        CreatedCcdCase ccdCase = createCcdCase(userEmail);

        String appellantTya = ccdCase.getAppellantTya();

        JSONArray onlineHearingForTya = sscsMyaBackendRequests.getOnlineHearingForCitizen(appellantTya, userEmail);
        assertThat(onlineHearingForTya.length(), is(0));

        JSONObject jsonObject = sscsMyaBackendRequests.assignCaseToUser(appellantTya, userEmail, "TN32 6PL");
        Long expectedCaseId = Long.valueOf(ccdCase.getCaseId());
        assertThat(jsonObject.getLong("case_id"), is(expectedCaseId));

        onlineHearingForTya = sscsMyaBackendRequests.getOnlineHearingForCitizen("", userEmail);
        assertThat(onlineHearingForTya.length(), is(1));
        assertThat(onlineHearingForTya.getJSONObject(0).get("case_id"), is(expectedCaseId));
    }

    @Test
    public void checkJointDoesNotHaveCaseAssignCaseAndCheckUserHasCase() throws IOException {
        String userEmail = createRandomEmail();
        idamTestApiRequests.createUser(userEmail);
        CreatedCcdCase ccdCase = createCcdCase(userEmail);

        String jointPartyTya = ccdCase.getJointPartyTya();

        JSONArray onlineHearingForTya = sscsMyaBackendRequests.getOnlineHearingForCitizen(jointPartyTya, userEmail);
        assertThat(onlineHearingForTya.length(), is(0));

        JSONObject jsonObject = sscsMyaBackendRequests.assignCaseToUser(jointPartyTya, userEmail, "TN32 6PL");
        Long expectedCaseId = Long.valueOf(ccdCase.getCaseId());
        assertThat(jsonObject.getLong("case_id"), is(expectedCaseId));

        onlineHearingForTya = sscsMyaBackendRequests.getOnlineHearingForCitizen("", userEmail);
        assertThat(onlineHearingForTya.length(), is(1));
        assertThat(onlineHearingForTya.getJSONObject(0).get("case_id"), is(expectedCaseId));
    }
}
