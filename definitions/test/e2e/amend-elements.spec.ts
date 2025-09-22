import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';

test.describe('Amend elements Feature', () => {

  test.beforeEach(async ({ uploadResponseSteps }, testInfo) => {
    const ucCaseId = await createCaseBasedOnCaseType('UC');
    testInfo.attach('caseId', { body: ucCaseId });
    await uploadResponseSteps.performUploadResponseOnAUniversalCredit(ucCaseId);
    (testInfo as any).caseId = ucCaseId;
  });

  test('Update elements and issues codes',
    {tag: '@nightly-pipeline'},
    async ({ amendElementSteps}, testInfo) => {
      const ucCaseId = (testInfo as any).caseId;
      await amendElementSteps.performAmendElements(ucCaseId);
  });
});