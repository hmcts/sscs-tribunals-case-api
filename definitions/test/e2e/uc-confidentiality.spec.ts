import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';

let caseId: string;

test.describe(
  'Universal Credit confidentiality',
  { tag: '@nightly-pipeline' },
  async () => {
    test('UC - add other party - confirm confidentiality', async ({
      ucConfidentialitySteps
    }) => {
      //login as DWP and go to case
      //confirm confidentiality tab is not visible
      // add other party
      //confirm other party is added successfully
      caseId = await createCaseBasedOnCaseType('UC');
      await ucConfidentialitySteps.addOtherPartyToUcCase(caseId);

      //login as caseworker and go to case
      //issue hef
      //confirm hef issued successfully
      await ucConfidentialitySteps.issueHef(caseId);

      //set confidentiality for appellant
      await ucConfidentialitySteps.setConfidentialityForAppellant(true);

      //set confidentiality for other party
      await ucConfidentialitySteps.setConfidentialityForOtherParty(true);

      //confirm confidentiality set successfully
      //verify confidentiality flag is set and visible
      await ucConfidentialitySteps.confirmConfidentialitySetSuccessfully();

      //run Confidentiality confirmed event and confirm end state is With FTA
      await ucConfidentialitySteps.submitConfidentialityConfirmedEvent();
    });
  }
);
