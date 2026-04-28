import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType, {
  createChildSupportCaseForCmConfidentiality
} from '../api/client/sscs/factory/appeal.type.factory';
import { credentials, featureFlags } from '../config/config';

const appealTypes = ['UC', 'CHILDSUPPORT'] as const;
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

test.describe('CM confidentiality send to interloc summary', () => {
  test.skip(
    !featureFlags.cmOtherPartyConfidentialityEnabled,
    'CM confidentiality flag is disabled'
  );

  for (const appealType of appealTypes) {
    for (const user of users) {
      test(
        `Selected confidentiality reason is visible on summary for confidential ${appealType} appeal as ${user.label}`,
        { tag: ['@nightly-pipeline', '@confidentiality'] },
        async ({
          enhancedConfidentialitySteps,
          sendToInterlocSteps,
          updateOtherPartyDataSteps,
          readyToListSteps,
          uploadResponseSteps
        }) => {
          const caseId =
            appealType === 'CHILDSUPPORT'
              ? await createChildSupportCaseForCmConfidentiality()
              : await createCaseBasedOnCaseType(appealType);

          if (appealType === 'CHILDSUPPORT') {
            await updateOtherPartyDataSteps.makeChildSupportCaseConfidential(
              caseId
            );
            if (user.label === 'superuser') {
              await updateOtherPartyDataSteps.signOut();
            }
          } else {
            await uploadResponseSteps.performUploadResponseOnAUniversalCreditWithJP(
              caseId,
              false
            );
            await enhancedConfidentialitySteps.prepareConfidentialCase(caseId);
          }

          if (appealType === 'CHILDSUPPORT' && user.label === 'caseworker') {
            await readyToListSteps.performReadyToListEvent(
              caseId,
              false,
              user.credentials
            );
          } else {
            await readyToListSteps.loginUserWithCaseId(
              user.credentials,
              true,
              caseId
            );
            await readyToListSteps.performReadyToListEvent(
              caseId,
              false,
              user.credentials
            );
          }
          await readyToListSteps.signOut();
          await sendToInterlocSteps.submitConfidentialityReferralAndVerifySummary(
            caseId,
            user.credentials
          );
        }
      );
    }
  }
});
