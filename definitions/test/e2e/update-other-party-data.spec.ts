import { test } from '../lib/steps.factory';
import { createChildSupportCaseForCmConfidentiality, createUCCaseForConfidentiality } from '../api/client/sscs/factory/appeal.type.factory';

let caseId: string;

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

for (const benefit of benefitTypes) {
  test(
  `Adding Other party data + subscription in a ${benefit.label} case`,
  { tag: '@nightly-pipeline' },
  async ({ updateOtherPartyDataSteps }) => {
    test.slow();
    const caseId = await benefit.createCase();
    await updateOtherPartyDataSteps.performUpdateOtherPartyData(caseId, benefit.label);
  }
);
}


test(
  'Adding Other party data + subscription in a TaxCredit case',
  { tag: '@nightly-pipeline' },
  async ({ updateOtherPartyDataSteps }) => {
    test.slow();
    await updateOtherPartyDataSteps.performUpdateOtherPartyDataTaxCredit(
      caseId
    );
  }
);
