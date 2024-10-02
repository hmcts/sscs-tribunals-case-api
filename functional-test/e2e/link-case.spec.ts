import {test} from "../lib/steps.factory";

test.describe('Link a case tests', () => {


    // Happy Path test:
    test("As a caseworker link a case to another case", {tag: '@nightly-pipeline'},async ({linkACaseSteps}) => {
        await linkACaseSteps.linkCaseSuccessfully();
    })

    // Test for error message that comes with linking invalid case:
    test("As a caseworker I should not be able link a case to a non-existent case",{tag: '@nightly-pipeline'}, async ({linkACaseSteps}) => {
        await linkACaseSteps.linkNonExistingCase();
    })

    // Test for error message that comes with linking a case to itself
    test("As a caseworker I should not be able to link a case to itself",{tag: '@nightly-pipeline'}, async ({linkACaseSteps}) => {
        await linkACaseSteps.linkCaseToItself();
    })

    // Test for removing link between cases after link a case has linked them together.
    test("As a caseworker I should be able to unlink cases",{tag: '@todo - as remove button is not working'}, async ({linkACaseSteps}) => {
        await linkACaseSteps.removeLinkedCase();
    })
})
