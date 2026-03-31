import { test } from '../lib/steps.factory';
import { createChildSupportCaseForCmConfidentiality } from '../api/client/sscs/factory/appeal.type.factory';
import { credentials, featureFlags } from '../config/config';

const users = [
  {
    label: 'superuser',
    credentials: credentials.superUser
  },
  {
    label: 'fta user',
    credentials: credentials.dwpResponseWriter
  }
] as const;

test.describe('CM add other party data', () => {
  test.skip(
    !featureFlags.cmOtherPartyConfidentialityEnabled,
    'CM confidentiality flag is disabled'
  );

  for (const user of users) {
    test(
      `${user.label} adds other party data to a Child Support appeal in Await Other Party Data and moves it to Await Confidentiality Requirements`,
      { tag: '@nightly-pipeline' },
      async ({ updateOtherPartyDataSteps }) => {
        test.slow();
        test.setTimeout(240000);

        const caseId = await createChildSupportCaseForCmConfidentiality();

        await updateOtherPartyDataSteps.addOtherPartyDataForAwaitOtherPartyData(
          caseId,
          user.credentials
        );
      }
    );
  }
});
