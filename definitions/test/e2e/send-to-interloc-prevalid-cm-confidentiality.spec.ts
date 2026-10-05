import { test } from '../lib/steps.factory';
import { createChildSupportCaseForPreValidConfidentiality } from '../api/client/sscs/factory/appeal.type.factory';
import { credentials } from '../config/config';


test.describe('CM confidentiality send to interloc pre-valid', () => {
    test(
      `Send to interloc - pre-valid shows confidentiality reasons for CHILDSUPPORT appeal as caseworker`,
      { tag: ['@nightly-pipeline', '@confidentiality'] },
      async ({ sendToInterlocSteps }) => {
        test.slow();

        const caseId = await createChildSupportCaseForPreValidConfidentiality();
        await sendToInterlocSteps.verifyPreValidConfidentialityReferralReasons(
          caseId,
          credentials.amCaseWorker
        );
      }
    );
});
