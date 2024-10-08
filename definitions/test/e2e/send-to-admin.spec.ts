import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";
let caseId : string;
test.beforeAll("Case has to be Created",async () => {
    caseId = await createCaseBasedOnCaseType('TAX CREDIT');
});

test("Send to admin", async ({ sendToAdminSteps }) => {
    await sendToAdminSteps.performSendToAdmin(caseId)
});

// test.afterAll("Case has to be set to Dormant",async () => {
//     await performAppealDormantOnCase(caseId);
// });

