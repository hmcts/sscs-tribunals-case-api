import { test } from '../lib/steps.factory';
import { createChildSupportCaseForCmConfidentiality } from '../api/client/sscs/factory/appeal.type.factory';
import { featureFlags } from '../config/config';

test.describe('CM child support confidentiality confirmation', () => {
  test.skip(
    !featureFlags.cmOtherPartyConfidentialityEnabled,
    'CM confidentiality flag is disabled'
  );

  test(
    'FTA user completes the Child Support confidentiality workflow and moves the appeal to With FTA',
    { tag: '@nightly-pipeline' },
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
