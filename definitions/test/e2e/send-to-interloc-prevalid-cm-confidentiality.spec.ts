import { test } from '../lib/steps.factory';
import { createChildSupportCaseForPreValidConfidentiality } from '../api/client/sscs/factory/appeal.type.factory';
import { credentials, featureFlags } from '../config/config';

const users = [
  {
    label: 'caseworker',
    credentials: credentials.amCaseWorker
  },
  {
    label: 'superuser',
    credentials: credentials.superUser
  }
] as const;

test.describe('CM confidentiality send to interloc pre-valid', () => {
  test.skip(
    !featureFlags.cmOtherPartyConfidentialityEnabled,
    'CM confidentiality flag is disabled'
  );

  for (const user of users) {
    test(
      `Send to interloc - pre-valid shows confidentiality reasons for CHILDSUPPORT appeal as ${user.label}`,
      { tag: '@nightly-pipeline' },
      async ({ sendToInterlocSteps }) => {
        test.slow();
        test.setTimeout(240000);

        const caseId = await createChildSupportCaseForPreValidConfidentiality();

        await sendToInterlocSteps.verifyPreValidConfidentialityReferralReasons(
          caseId,
          user.credentials
        );
      }
    );
  }
});
