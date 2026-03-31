import { test } from '../lib/steps.factory';
import { createChildSupportCaseForInterlocReviewValidation } from '../api/client/sscs/factory/appeal.type.factory';
import { credentials, featureFlags } from '../config/config';

const users = [
  {
    label: 'superuser',
    credentials: credentials.superUser,
    locked: true
  },
  {
    label: 'judge',
    credentials: credentials.judge,
    locked: false
  },
  {
    label: 'legal officer',
    credentials: credentials.legalOfficer,
    locked: false
  }
] as const;

test.describe('CM interlocutory review appeal validated', () => {
  test.skip(
    !featureFlags.cmOtherPartyConfidentialityEnabled,
    'CM confidentiality flag is disabled'
  );

  for (const user of users) {
    if (user.locked) {
      test.skip(
        `${user.label} validates an interlocutory review pre-valid Child Support appeal and moves it to Await Other Party Data`,
        async () => {},
        'Superuser account is locked'
      );
      continue;
    }

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
