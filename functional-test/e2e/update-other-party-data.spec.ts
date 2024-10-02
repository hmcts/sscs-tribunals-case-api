import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";

let caseId: string;

test("Adding Other party data + subscription in a ChildSupport case", { tag: '@nightly-pipeline' }, async ({ updateOtherPartyDataSteps }) => {
    test.slow();
    await updateOtherPartyDataSteps.performUpdateOtherPartyData(caseId);
})

test("Adding Other party data + subscription in a TaxCredit case", { tag: '@nightly-pipeline' }, async ({ updateOtherPartyDataSteps }) => {
    test.slow();
    await updateOtherPartyDataSteps.performUpdateOtherPartyDataTaxCredit(caseId);
})
