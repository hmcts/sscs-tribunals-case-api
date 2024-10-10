import { test } from "../lib/steps.factory";

test.describe("Postponement Request test", {tag: ['@preview-regression', '@nightly-pipeline']}, async() => {

    test("Hearing Route as LA with a Grant Option", async ({ postponementSteps }) => {
        test.slow();
        await postponementSteps.postponeAListAssistCaseWithAPostponement('Grant Postponement');
    });

    test("Hearing Route as LA with a Refuse Option", async ({ postponementSteps }) => {
        test.slow();
        await postponementSteps.postponeAListAssistCaseWithAPostponement('Refuse Postponement');
    });

    test("Hearing Route as LA with a Send to Judge Option", async ({ postponementSteps }) => {
        test.slow();
        await postponementSteps.postponeAListAssistCaseWithAPostponement('Send to Judge');
    });
});

