import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";

let caseId: string;

test("Performing Update subscription event for all parties PIP case", { tag: '@nightly-pipeline' }, async ({ updateSubscriptionSteps }) => {
    test.slow();
    await updateSubscriptionSteps.performUpdateSubscription(caseId);
})