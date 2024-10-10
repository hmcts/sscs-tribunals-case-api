import {test} from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";

let caseId: string;

test.describe('Issue Final Decision - PIP Appeal Type', {tag: '@nightly-pipeline'}, async () => {

    test("Issue Final Decision - Upload Response with Further Information as No - Simple Decision Notice - 'Yes' notice generated. - No Award Given", {tag: ['@regression']},
        async ({issueFinalDecisionSteps}) => {
            test.slow();
            let pipCaseId = await createCaseBasedOnCaseType('PIP');
            await issueFinalDecisionSteps.performWriteFinalDecisionForAPIPAppealNoAwardAndNoticeGenerated(pipCaseId);
            await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
            // await performAppealDormantOnCase(pipCaseId);
        });

    test("Issue Final Decision - Upload Response with Further Information as No - Simple Decision Notice - 'Yes' notice generated. - Yes Award is Given", {tag: ['@preview-regression', '@regression']},
        async ({issueFinalDecisionSteps}) => {
            test.slow();
            let pipCaseId = await createCaseBasedOnCaseType('PIP');
            await issueFinalDecisionSteps.performWriteFinalDecisionForAPIPAppealAwardAndNoticeGenerated(pipCaseId);
            await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
            // await performAppealDormantOnCase(pipCaseId);
        });

})

test.describe('Issue Final Decision - Tax Credit Appeal Type', {tag: ['@preview-regression', '@nightly-pipeline']},  async () => {

    test("Issue Final Decision - Upload Response with Further Information as No - Simple Decision Notice - 'No' notice generated", {tag: ['@regression']},
        async ({issueFinalDecisionSteps}) => {
            test.slow();
            let taxCreditCaseId = await createCaseBasedOnCaseType('TAX CREDIT');
            await issueFinalDecisionSteps.performWriteFinalDecisionForATaxCreditAppealAndNoNoticeGenerated(taxCreditCaseId);
            await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
            // await performAppealDormantOnCase(taxCreditCaseId);
        });
})

test.describe('Issue Final Decision - Universal Credit Appeal Type', {tag: ['@preview-regression', '@nightly-pipeline']},  async () => {

    test("Issue Final Decision - Simple Decision Notice - 'Yes' notice generated",
        async ({issueFinalDecisionSteps}) => {
            test.slow();
            let universalCreditCaseId = await createCaseBasedOnCaseType('UC');
            await issueFinalDecisionSteps.performWriteFinalDecisionForAUniversalCreditAppealAndNoticeGenerated(universalCreditCaseId);
            await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
            // await performAppealDormantOnCase(universalCreditCaseId);
        });
})

test.describe('Issue Final Decision - ESA Appeal Type', {tag: '@nightly-pipeline'}, async () => {

    test("Issue Final Decision - 'Yes' notice generated - 'No' Award Given",
        {tag: '@preview-regression'},
        async ({issueFinalDecisionSteps}) => {
            test.slow();
            let esaCaseId = await createCaseBasedOnCaseType('ESA');
            await issueFinalDecisionSteps.performWriteFinalDecisionForAESAAppealNoAwardGivenAndNoticeGenerated(esaCaseId);
            await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
            await issueFinalDecisionSteps.verifyFinalDecisionForAnAppeal();
    });

    test("Issue Final Decision - 'Yes' notice generated - 'Yes' Award Given", 
        async ({issueFinalDecisionSteps}) => {
            test.slow();
            let esaCaseId = await createCaseBasedOnCaseType('ESA');
            await issueFinalDecisionSteps.performWriteFinalDecisionForAESAAppealYesAwardGivenAndNoticeGenerated(esaCaseId);
            await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
            await issueFinalDecisionSteps.verifyFinalDecisionForAnAppeal();
    });


})
