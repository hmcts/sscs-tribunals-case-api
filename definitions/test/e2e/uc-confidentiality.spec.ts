import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';
import { credentials } from '../config/config';

let caseId: string;

test.describe(
  'Universal Credit confidentiality',
  { tag: '@nightly-pipeline' },
  async () => {
    test.beforeEach(async ({ ucConfidentialitySteps }) => {
      caseId = await createCaseBasedOnCaseType('UC');
      await ucConfidentialitySteps.addOtherPartyToUcCase(caseId);
      await ucConfidentialitySteps.issueHef(caseId);
    });

    test('UC - add other party - confirm confidentiality', async ({
      ucConfidentialitySteps
    }) => {
      await ucConfidentialitySteps.setConfidentialityForAppellant(true);
      await ucConfidentialitySteps.setConfidentialityForOtherParty(true);
      await ucConfidentialitySteps.confirmConfidentialityGrantedSetSuccessfully();
      await ucConfidentialitySteps.submitConfidentialityConfirmedEvent(caseId);
    });

    test('UC - appellant confidentiality refused via interloc', async ({
      ucConfidentialitySteps,
      sendToInterlocSteps
    }) => {
      await ucConfidentialitySteps.setConfidentialityForOtherParty(false);
      await sendToInterlocSteps.submitConfidentialityReferralAndVerifySummary(
        caseId,
        credentials.amCaseWorker
      );
      await ucConfidentialitySteps.refuseAppellantConfidentialityViaIssueDirectionNotice(
        caseId
      );
      await ucConfidentialitySteps.confirmConfidentialityRefusedSetSuccessfully();
      await ucConfidentialitySteps.submitConfidentialityConfirmedEvent(caseId);
    });
  }
);
