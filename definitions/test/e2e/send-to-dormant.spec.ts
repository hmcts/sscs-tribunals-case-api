import {test} from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";

let caseId: string;
test.beforeAll("Case has to be Created", async () => {
    caseId = await createCaseBasedOnCaseType('PIP');
});
test("As a caseworker set a Case to Dormant",
    async ({sendToDormantSteps}) => {
    await sendToDormantSteps.performSendToDormant(caseId);
});
