import {test} from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";
let caseId : string;
test.beforeAll("Case has to be Created",async () => {
    caseId = await createCaseBasedOnCaseType('PIP');
});

test("Test Listing Error Event sets case state to Listing Error", {tag: '@nightly-pipeline'}, async ({listingErrorSteps}) => {
    await listingErrorSteps.performListingErrorEvent(caseId);

});

test.afterAll("Case has to be set to Dormant",async () => {
   await performAppealDormantOnCase(caseId);
});
