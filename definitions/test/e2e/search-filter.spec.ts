import { test } from "../lib/steps.factory";

test("Searching case by benefit and issue code ", async ({ searchFilterSteps }) => {
    test.slow();
    await searchFilterSteps.performSearchSteps();
})