import { test } from "../lib/steps.factory";

test.describe.skip('Associate case tests', () => {

    /* TODO: Below test needs to be revisited when the defect SSCSSI-860 is fixed.
       Related cases tab is not shown after completing Associate Case Event with 
       another related case that can be associated.
    */
    test("As a caseworker associate a case to another case", async ({ associateCaseSteps }) => {
        await associateCaseSteps.associateCaseSuccessfully();
    });

    test("As a caseworker I should not be able associate a case to non-existent case", async ({ associateCaseSteps }) => {
        await associateCaseSteps.associateNonExistentCase();
    });

    /* TODO: Below test needs to be revisited when the relevant bug SSCSCI-877 is fixed.
       No validation is in place when the case worker self associates the case.
    */
    test.describe.fixme(() => {
        test("As a caseworker I should not be able self associate a case", async ({ associateCaseSteps }) => {
            await associateCaseSteps.selfAssociateACase();
        });
    });
});




