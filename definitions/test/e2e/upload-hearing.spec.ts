import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";

let caseId : string;


test.describe("Hearing upload test", {tag: ['@nightly-pipeline']}, async() => {

    test.beforeEach("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
        test.setTimeout(240000);
    });
    
    test("Grant - Hearing recording request", async ({ uploadHearingSteps }) => {
        await uploadHearingSteps.requestAndGrantAnHearingRecording(caseId);
    });
    
    test("Refuse - Hearing recording request", async ({ uploadHearingSteps }) => {
        await uploadHearingSteps.requestAndRefuseAnHearingRecording(caseId);
    });
    
     test.afterAll("Case has to be set to Dormant",async () => {
        // await performAppealDormantOnCase(caseId);
     });
    
});

