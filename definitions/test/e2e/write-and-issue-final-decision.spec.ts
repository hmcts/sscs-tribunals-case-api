import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';
import { credentials } from '../config/config';
import dateUtilsComponent from '../utils/DateUtilsComponent';

let caseId: string;

test.describe(
  'Issue Final Decision - PIP Appeal Type',
  { tag: '@nightly-pipeline' },
  async () => {

    let date = dateUtilsComponent.formatDateToSpecifiedDateNumberFormat(
      new Date()
    );

    let documentType = 'Draft Decision Notice';
    let tab = 'Tribunal Internal Documents';
    let fileName = `Draft Decision Notice generated on ${date}.pdf`;

    test(
      "Issue Final Decision - Upload Response with Further Information as No - Simple Decision Notice - 'Yes' notice generated. - No Award Given",
      { tag: ['@regression'] },
      async ({ issueFinalDecisionSteps, manageDocumentsSteps }) => {
        test.slow();
        let pipCaseId = await createCaseBasedOnCaseType('PIP');


        //Write Draft decision and verify document is placed in internal tab
        await issueFinalDecisionSteps.performWriteFinalDecisionForAPIPAppealNoAwardAndNoticeGenerated(
          pipCaseId
        );
        await manageDocumentsSteps.verifyFileInTab(tab, documentType, fileName);
        await manageDocumentsSteps.signOut();

        //Login as DWP Response Writer and verify internal tab is hidden
        await manageDocumentsSteps.loginUserWithCaseId(
          credentials.dwpResponseWriter,
          false,
          pipCaseId
        );
        await manageDocumentsSteps.verifyInternalDocumentsTabHidden();
        await manageDocumentsSteps.signOut();

        //Login as Judge to issue final decision
        await manageDocumentsSteps.loginUserWithCaseId(
          credentials.judge,
          false,
          pipCaseId
        );
        await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
      }
    );

    test(
      "Issue Final Decision - Upload Response with Further Information as No - Simple Decision Notice - 'Yes' notice generated. - Yes Award is Given",
      { tag: ['@preview-pipeline', '@regression'] },
      async ({ issueFinalDecisionSteps, manageDocumentsSteps }) => {
        test.slow();
        let pipCaseId = await createCaseBasedOnCaseType('PIP');
        await issueFinalDecisionSteps.performWriteFinalDecisionForAPIPAppealAwardAndNoticeGenerated(
          pipCaseId
        );
        await manageDocumentsSteps.verifyFileInTab(tab, documentType, fileName);
        await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
      }
    );
  }
);

test.describe(
  'Issue Final Decision - Tax Credit Appeal Type',
  { tag: ['@preview-regression', '@nightly-pipeline'] },
  async () => {
    test(
      "Issue Final Decision - Upload Response with Further Information as No - Simple Decision Notice - 'No' notice generated",
      { tag: ['@regression'] },
      async ({ issueFinalDecisionSteps }) => {
        test.slow();
        let taxCreditCaseId = await createCaseBasedOnCaseType('TAX CREDIT');
        await issueFinalDecisionSteps.performWriteFinalDecisionForATaxCreditAppealAndNoNoticeGenerated(
          taxCreditCaseId
        );
        await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
      }
    );
  }
);

test.describe(
  'Issue Final Decision - Universal Credit Appeal Type',
  { tag: ['@preview-regression', '@nightly-pipeline'] },
  async () => {
    test("Issue Final Decision - Simple Decision Notice - 'Yes' notice generated", async ({
      issueFinalDecisionSteps
    }) => {
      test.slow();
      let universalCreditCaseId = await createCaseBasedOnCaseType('UC');
      await issueFinalDecisionSteps.performWriteFinalDecisionForAUniversalCreditAppealAndNoticeGenerated(
        universalCreditCaseId
      );
      await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
    });
    test("Issue Final Decision - SV issue code - 'Yes' notice generated - Appeal 'Allowed' - SCC appeal 'Yes' - SCC applies 'Yes'", async ({
      uploadResponseSteps, issueFinalDecisionSteps
    }) => {
      let universalCreditCaseId = await createCaseBasedOnCaseType('UC');

      await uploadResponseSteps.uploadResponseUcAppealWcaAndSvIssueCode(universalCreditCaseId)
      await issueFinalDecisionSteps.performWriteFinalDecisionForUniversalCreditAppealWithScc({ universalCreditCaseId });
      await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
      await issueFinalDecisionSteps.verifyAppealDormantAfterIssueFinalDecisionEvent();

    });
    test("Issue Final Decision - SV issue code - 'Yes' notice generated - Appeal 'Refused' - SCC appeal 'Yes' - SCC applies 'No'", async ({
      uploadResponseSteps, issueFinalDecisionSteps
    }) => {
      let universalCreditCaseId = await createCaseBasedOnCaseType('UC');

      await uploadResponseSteps.uploadResponseUcAppealWcaAndSvIssueCode(universalCreditCaseId)
      await issueFinalDecisionSteps.performWriteFinalDecisionForUniversalCreditAppealWithScc({
        universalCreditCaseId,
        isAppealAllowed: false,
        sccApplies: false
      });
      await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
      await issueFinalDecisionSteps.verifyAppealDormantAfterIssueFinalDecisionEvent();
    });

  }
);

test.describe(
  'Issue Final Decision - ESA Appeal Type',
  { tag: '@nightly-pipeline' },
  async () => {
    test(
      "Issue Final Decision - 'Yes' notice generated - 'No' Award Given",
      { tag: '@preview-regression' },
      async ({ issueFinalDecisionSteps }) => {
        test.slow();
        let esaCaseId = await createCaseBasedOnCaseType('ESA');
        await issueFinalDecisionSteps.performWriteFinalDecisionForAESAAppealNoAwardGivenAndNoticeGenerated(
          esaCaseId
        );
        await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
        await issueFinalDecisionSteps.verifyFinalDecisionForAnAppeal();
      }
    );

    test("Issue Final Decision - 'Yes' notice generated - 'Yes' Award Given", async ({
      issueFinalDecisionSteps
    }) => {
      test.slow();
      let esaCaseId = await createCaseBasedOnCaseType('ESA');
      await issueFinalDecisionSteps.performWriteFinalDecisionForAESAAppealYesAwardGivenAndNoticeGenerated(
        esaCaseId
      );
      await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
      await issueFinalDecisionSteps.verifyFinalDecisionForAnAppeal();
    });

    test("Issue Final Decision - 'Yes' notice generated - Appeal Allowed - SCC appeal 'Yes' - SCC apply 'Yes'", async ({
      uploadResponseSteps, issueFinalDecisionSteps
    }) => {
      let esaCaseId = await createCaseBasedOnCaseType('ESA');
      await uploadResponseSteps.uploadResponseEsaAppealWcaAndSvIssueCode(esaCaseId)
      await issueFinalDecisionSteps.performWriteFinalDecisionForEsaAppealWithScc({ 
        esaCaseId,
        isAppealAllowed: true,
        sccAppeal: true,
        sccApplies: true
       });
      await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
      await issueFinalDecisionSteps.verifyAppealDormantAfterIssueFinalDecisionEvent();
    });

    test("Issue Final Decision - 'Yes' notice generated - Appeal Refused - SCC appeal 'Yes' - SCC apply 'No'", async ({
      uploadResponseSteps, issueFinalDecisionSteps
    }) => {
      let esaCaseId = await createCaseBasedOnCaseType('ESA');
      await uploadResponseSteps.uploadResponseEsaAppealWcaAndSvIssueCode(esaCaseId)
      await issueFinalDecisionSteps.performWriteFinalDecisionForEsaAppealWithScc({
        esaCaseId,
        isAppealAllowed: false,
        sccAppeal: true,
        sccApplies: false
      });
      await issueFinalDecisionSteps.performIssueFinalDecisionForAnAppeal();
      await issueFinalDecisionSteps.verifyAppealDormantAfterIssueFinalDecisionEvent();
    });
  }
);
