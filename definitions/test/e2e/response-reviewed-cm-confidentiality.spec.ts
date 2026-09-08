import { test } from '../lib/steps.factory';
import { createChildSupportCaseForCmConfidentiality } from '../api/client/sscs/factory/appeal.type.factory';
import { credentials } from '../config/config';
import sendToInterlocData from '../pages/content/send.to.interloc_en.json';


test.describe('CM confidentiality response reviewed', () => {
      test(
        `Response reviewed shows confidentiality reasons for confidential CHILDSUPPORT appeal as Caseworker for Judge`,
        { tag: ['@nightly-pipeline', '@confidentiality'] },
        async ({ uploadResponseSteps }) => {
          test.slow();
          test.setTimeout(360000);

          const caseId = await createChildSupportCaseForCmConfidentiality();
          await uploadResponseSteps.prepareChildSupportCaseForResponseReviewed(
            caseId
          );
          await uploadResponseSteps.verifyChildSupportResponseReviewedConfidentialityReferralReasons(
            caseId,
            credentials.amCaseWorker,
            sendToInterlocData.sendToInterlocCaseReviewSelectValueJudge
          );
        }
      );
});
