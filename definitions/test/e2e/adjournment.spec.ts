import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';

test.describe('Adjournment Feature', () => {

  test.beforeEach(async ({ uploadResponseSteps, adjournmentSteps, hearingSteps }, testInfo) => {
    const caseId = await createCaseBasedOnCaseType('DLASANDL');
    testInfo.attach('caseId', { body: caseId });
    await uploadResponseSteps.performUploadResponse(caseId, 'dla');
    await adjournmentSteps.verifyHearingHelper(caseId);
    await hearingSteps.cancelHearingForCleanUp();
    // Store caseId in testInfo for access in tests
    (testInfo as any).caseId = caseId;
  });

  test('Adjourn a hearing & move case back to ready to list state',
    {tag: '@nightly-pipeline'},
    async ({ adjournmentSteps }, testInfo) => {
      const caseId = (testInfo as any).caseId;
      await adjournmentSteps.writeAdjournmentAndMoveToListing(caseId, { setToRTLFlag: true });
      await adjournmentSteps.performIssueAdjournmentNoticeForAnAppeal({ endState: 'Ready to list' });
      await adjournmentSteps.verifyAdjournmentDecisionForAnAppeal();
      await adjournmentSteps.verifyListingRequirements();
  });

  test('Adjourn a hearing & move case back to Not listable state',
    {tag: '@nightly-pipeline'},
    async ({ adjournmentSteps }, testInfo) => {
      const caseId = (testInfo as any).caseId;
      await adjournmentSteps.writeAdjournmentAndMoveToListing(caseId, { setToRTLFlag: false });
      await adjournmentSteps.performIssueAdjournmentNoticeForAnAppeal({ endState: 'Not listable' });
      await adjournmentSteps.verifyAdjournmentDecisionForAnAppeal();
  });
});