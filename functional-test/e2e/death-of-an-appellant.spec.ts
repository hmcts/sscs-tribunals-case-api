import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";

let caseId : string;

test.describe("Death of appellant test", async() => {

    test("Death of an Appellant without an Appointee", {tag: '@nightly-pipeline'}, async ({ deathOfAppellant }) => {
        await deathOfAppellant.performDeathOfAnAppellantWithoutAnApointee();
    });
    
    test("Death of an Appellant with an Appointee",{tag: '@nightly-pipeline'}, async ({ deathOfAppellant }) => {
        await deathOfAppellant.performDeathOfAnAppellantWithAnAppointee();
    });
    
    test("Validation Test - Death of the Appellant invalid Date", {tag: '@nightly-pipeline'}, async ({ deathOfAppellant }) => {
        await deathOfAppellant.performDeathOfAnAppellantNotValidErrorScenarios();
    });
    
    test("Validation Test - Death of the Appellant in the Future", {tag: '@nightly-pipeline'}, async ({ deathOfAppellant }) => {
        await deathOfAppellant.performDeathOfAnAppellantFutureDateErrorScenarios();
    });
    
})

