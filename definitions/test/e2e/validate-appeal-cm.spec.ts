import { test } from '../lib/steps.factory';
import { createChildSupportCaseForPreValidConfidentiality } from '../api/client/sscs/factory/appeal.type.factory';
import { featureFlags } from '../config/config';

// const roles = ['caseworker', 'superuser'] as const;
const roles = ['superuser'] as const;

test.describe('CM validate appeal', () => {
  test.skip(
    !featureFlags.cmOtherPartyConfidentialityEnabled,
    'CM confidentiality flag is disabled'
  );

  for (const role of roles) {
    test(
      `${role} validates an incomplete Child Support appeal and moves it to Await Other Party Data`,
      { tag: ['@nightly-pipeline', '@confidentiality'] },
      async ({ validateAppealSteps }) => {
        const caseId = await createChildSupportCaseForPreValidConfidentiality();
        await validateAppealSteps.validateChildSupportIncompleteAppeal(
          caseId,
          role
        );
      }
    );
  }
});
