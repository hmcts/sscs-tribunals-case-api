import { test } from '../lib/steps.factory';
import { createUcCaseForPreValidConfidentiality } from '../api/client/sscs/factory/appeal.type.factory';
import { credentials } from '../config/config';


test.describe('CM confidentiality send to interloc pre-valid UC', () => {
    test(
      `Send to interloc - pre-valid shows confidentiality reasons for UC appeal as caseworker}`,
      { tag: ['@nightly-pipeline', '@confidentiality'] },
      async ({ sendToInterlocSteps }) => {
        test.slow();

        const caseId = await createUcCaseForPreValidConfidentiality();
        await sendToInterlocSteps.verifyPreValidConfidentialityReferralReasons(
          caseId,
          credentials.amCaseWorker
        );
      }
    );
});
