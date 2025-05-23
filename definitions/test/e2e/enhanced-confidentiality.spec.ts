import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';
import { request } from '@playwright/test';

let caseId: string;

test.describe('Enhanced confidentiality test', async () => {
  test.beforeEach('Case has to be Created', async () => {
    caseId = await createCaseBasedOnCaseType('UC');
    test.setTimeout(360000);
  });

  test.afterEach(
    'Cancel the hearings after test run',
    async({ hearingSteps, enhancedConfidentialitySteps }, testInfo) => {
      if(testInfo.title.includes("#executeTearDown")){
        await enhancedConfidentialitySteps.loginAndNavigateToHearingsTab(caseId);
        await hearingSteps.cancelHearingForCleanUp();
      }
    }
  )

  test('Grant - Confidentiality request & verify bundle with redacted file', async ({
    uploadResponseSteps,
    enhancedConfidentialitySteps,
    createBundleSteps
  }) => {
    await uploadResponseSteps.performUploadResponseOnAUniversalCreditWithJP(
      caseId
    );
    await enhancedConfidentialitySteps.requestForConfidentiality();
    await enhancedConfidentialitySteps.grantConfidentialityForAppellant(caseId);
    await enhancedConfidentialitySteps.verifyConfidentialityFlag();
    await enhancedConfidentialitySteps.uploadSupplementaryCorrespondence(
      caseId
    );
    await enhancedConfidentialitySteps.requestForEvidenceConfidentiality();
    await enhancedConfidentialitySteps.verifyRedactedContent();
    await createBundleSteps.triggerBundleForConfidentialCase();
  });

  test(
    'Refuse - confidentiality request for a party on a case #executeTearDown',
    { tag: ['@preview-regression', '@nightly-pipeline'] },
    async ({ uploadResponseSteps, enhancedConfidentialitySteps, request }) => {
      test.slow();
      await uploadResponseSteps.checkHmcEnvironment(request);
      await uploadResponseSteps.performUploadResponseOnAUniversalCreditWithJP(
        caseId
      );
      await enhancedConfidentialitySteps.requestForConfidentiality();
      await enhancedConfidentialitySteps.requestConfidentialityForJP();
      await enhancedConfidentialitySteps.verifyTotalRequest();
      await enhancedConfidentialitySteps.confidentialityDecisionForParties(
        caseId
      );
      await enhancedConfidentialitySteps.verifyConfidentialityFlagForMultipleParties();
    }
  );
});
