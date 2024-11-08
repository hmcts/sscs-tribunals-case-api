import {test} from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";

test.describe('Provide Appointee Details to a Non Dormant Case', {tag: '@nightly-pipeline'}, async() => {
    let caseId: string;
    test.beforeAll("A non Dormant case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("As a Caseworker verify Provide Appointee Details is not visible", async ({ provideAppointeeDetailsSteps }) => {
        await provideAppointeeDetailsSteps.performAttemptAppointeeDetailsNonDormantCase(caseId);
    });
});

test.describe('Provide No Appointee Details on a Dormant Case',  {tag: '@nightly-pipeline'}, async() => {
    let caseId: string;
    test.beforeAll("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
        await performAppealDormantOnCase(caseId);
    });

    test("As a Caseworker Provide No Appointee Details on a Dormant Case", async ({ provideAppointeeDetailsSteps }) => {
        await provideAppointeeDetailsSteps.performNoAppointeeDetails(caseId);
    });
});

test.describe('Provide Appointee Details on a Dormant Case', {tag: ['@preview-regression', '@nightly-pipeline']}, async() => {
    let caseId: string;
    test.beforeAll("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
        await performAppealDormantOnCase(caseId);
    });

    test("As a Caseworker Provide Appointee Details on a Dormant Case", async ({ provideAppointeeDetailsSteps }) => {
        await provideAppointeeDetailsSteps.performProvideAppointeeDetails(caseId);
    });
});
