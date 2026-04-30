import { test } from '../lib/steps.factory';
import { createChildSupportCaseForCmConfidentiality } from '../api/client/sscs/factory/appeal.type.factory';
import { credentials, featureFlags } from '../config/config';
import sendToInterlocData from '../pages/content/send.to.interloc_en.json';

// const reviewers = [
//   sendToInterlocData.sendToInterlocCaseReviewSelectValueJudge,
//   sendToInterlocData.sendToInterlocCaseReviewSelectValue
// ] as const;

// const users = [
//   {
//     label: 'caseworker',
//     credentials: credentials.amCaseWorker
//   }
// ] as const;

test.describe('CM confidentiality response reviewed', () => {
  test.skip(
    !featureFlags.cmOtherPartyConfidentialityEnabled,
    'CM confidentiality flag is disabled'
  );

  // for (const user of users) {
  //   for (const reviewer of reviewers) {
      test(
        `Response reviewed shows confidentiality reasons for confidential CHILDSUPPORT appeal as Caseworker for Judge`,
        { tag: ['@nightly-pipeline-cm', '@confidentiality'] },
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
  //   }
  // }
});
