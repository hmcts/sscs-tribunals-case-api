import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";
let caseId : string;
test.beforeAll("Case has to be Created",async () => {
    caseId = await createCaseBasedOnCaseType('PIP');
});

test("As a caseworker Strike out case",
    async ({ strikeOutCaseSteps }) => {
    await strikeOutCaseSteps.performStrikeOutCase(caseId);
});
