import { test } from '../lib/steps.factory';
import { createChildSupportCaseForInterlocReviewValidation } from '../api/client/sscs/factory/appeal.type.factory';
import { credentials, featureFlags } from '../config/config';

const users = [
  {
    label: 'superuser',
    credentials: credentials.superUser
  },
  {
    label: 'judge',
    credentials: credentials.judge
  },
  {
    label: 'legal officer',
    credentials: credentials.legalOfficer
  }
] as const;

test.describe('CM interlocutory review appeal validated', () => {
  test.skip(
    !featureFlags.cmOtherPartyConfidentialityEnabled,
    'CM confidentiality flag is disabled'
  );

  for (const user of users) {
    test(
      `${user.label} validates an interlocutory review pre-valid Child Support appeal and moves it to Await Other Party Data`,
      { tag: '@nightly-pipeline' },
      async ({ issueDirectionsNoticeSteps }) => {
        test.slow();
        test.setTimeout(300000);

        const caseId = await createChildSupportCaseForInterlocReviewValidation();

        await issueDirectionsNoticeSteps.validateChildSupportInterlocReviewPreValidAppeal(
          caseId,
          user.credentials
        );
      }
    );
  }
});
