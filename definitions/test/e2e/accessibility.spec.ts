import {test} from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";

let firstCaseId: string;
let secondCaseId: string;

test.describe("Accessibility Tests", {tag: '@accessibility'}, async () => {

    test.beforeAll("Case has to be Created", async () => {
        firstCaseId = await createCaseBasedOnCaseType('PIP');
        secondCaseId = await createCaseBasedOnCaseType("PIP");

    });

    test("Test", async ({accessibilitySteps}) => {
        test.setTimeout(180000)
        await accessibilitySteps.performAccessibilityTest(firstCaseId, secondCaseId)
    });

// Create a case
// Login as dwp user -> (scan login page)
// Upload FE -> (scan pages)
// Action further evidence -> (scan pages)
// Upload response -> (scan pages)
// Create bundle -> (scan page)
// Link a case -> (scan page)
// Scan all the tab
})