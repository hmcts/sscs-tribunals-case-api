import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";

test("Searching case by benefit and issue code ", { tag: '@nightly-pipeline' }, async ({ searchFilterSteps }) => {
    test.slow();
    await searchFilterSteps.performSearchSteps();
})