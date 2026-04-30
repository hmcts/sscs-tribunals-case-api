import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType, {
  createChildSupportCaseForCmConfidentiality
} from '../api/client/sscs/factory/appeal.type.factory';
import { credentials, featureFlags } from '../config/config';

const appealTypes = ['UC', 'CHILDSUPPORT'] as const;

test.describe('CM confidentiality send to interloc summary', () => {
  test.skip(
    !featureFlags.cmOtherPartyConfidentialityEnabled,
    'CM confidentiality flag is disabled'
  );

  for (const appealType of appealTypes) {
      test(
        `Selected confidentiality reason is visible on summary for confidential ${appealType} appeal as Caseworker`,
        { tag: ['@nightly-pipeline-cm', '@confidentiality'] },
        async ({
          enhancedConfidentialitySteps,
          sendToInterlocSteps,
          updateOtherPartyDataSteps,
          readyToListSteps,
          uploadResponseSteps
        }) => {
          test.slow();

          const caseId =
            appealType === 'CHILDSUPPORT'
              ? await createChildSupportCaseForCmConfidentiality()
              : await createCaseBasedOnCaseType(appealType);

          if (appealType === 'CHILDSUPPORT') {
            await updateOtherPartyDataSteps.makeChildSupportCaseConfidential(
              caseId
            );
          } else {
            await uploadResponseSteps.performUploadResponseOnAUniversalCreditWithJP(
              caseId,
              false
            );
            await enhancedConfidentialitySteps.prepareConfidentialCase(caseId);
          }

          if (appealType === 'CHILDSUPPORT') {
            await readyToListSteps.performReadyToListEvent(
              caseId,
              false,
              credentials.amCaseWorker
            );
          } else {
            await readyToListSteps.loginUserWithCaseId(
              credentials.amCaseWorker,
              true,
              caseId
            );
            await readyToListSteps.performReadyToListEvent(
              caseId,
              false,
              credentials.amCaseWorker
            );
          }
          await readyToListSteps.signOut();
          await sendToInterlocSteps.submitConfidentialityReferralAndVerifySummary(
            caseId,
            credentials.amCaseWorker
          );
        }
      );
    }
});
