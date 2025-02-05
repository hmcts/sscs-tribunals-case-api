import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";

let caseId : string;


test.describe("Hearing upload test", async() => {

    test.beforeEach("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
        test.setTimeout(240000);
    });
    
    test("Grant - Hearing recording request", async ({ uploadHearingSteps }) => {
        await uploadHearingSteps.requestAndGrantAnHearingRecording(caseId);
    });
    
    test("Refuse - Hearing recording request", {tag: '@nightly-pipeline'}, async ({ uploadHearingSteps }) => {
        await uploadHearingSteps.requestAndRefuseAnHearingRecording(caseId);
    });
    
     test.afterAll("Case has to be set to Dormant",async () => {
        // await performAppealDormantOnCase(caseId);
     });
    
});

