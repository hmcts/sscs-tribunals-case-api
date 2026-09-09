import { test } from '../lib/steps.factory';
import { createChildSupportCaseForCmConfidentiality } from '../api/client/sscs/factory/appeal.type.factory';

test.describe('CM child support confidentiality confirmation', () => {
  test(
    'FTA user completes the Child Support confidentiality workflow and moves the appeal to With FTA',
    { tag: ['@nightly-pipeline', '@confidentiality'] },
    async ({ updateOtherPartyDataSteps }) => {
      test.slow();
      test.setTimeout(360000);

      const caseId = await createChildSupportCaseForCmConfidentiality();
      await updateOtherPartyDataSteps.completeChildSupportConfidentialityDeterminationFlow(
        caseId
      );
    }
  );
});
