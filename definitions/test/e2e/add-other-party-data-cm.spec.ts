import { test } from '../lib/steps.factory';
import { createChildSupportCaseForCmConfidentiality, createUCCaseForConfidentiality} from '../api/client/sscs/factory/appeal.type.factory';
import { credentials, featureFlags } from '../config/config';

const benefitTypes = [
  {
    label: 'Child Support',
    createCase: createChildSupportCaseForCmConfidentiality
  },
  {
    label: 'Universal Credit',
    createCase: createUCCaseForConfidentiality
  }
]

test.describe('CM add other party data', () => {
  test.skip(
    !featureFlags.cmOtherPartyConfidentialityEnabled,
    'CM confidentiality flag is disabled'
  );

  for (const benefit of benefitTypes) {
    test(
      `Add other party data to a ${benefit.label} appeal in Await Other Party Data and moves it to Await Confidentiality Requirements`,
      { tag: ['@confidentiality'] },
      async ({ updateOtherPartyDataSteps }) => {
        test.slow();
        test.setTimeout(240000);

        const caseId = await benefit.createCase();
        await updateOtherPartyDataSteps.addOtherPartyDataForAwaitOtherPartyData(
          caseId,
          credentials.dwpResponseWriter,
          benefit.label
        );
      }
    );
  }
});
