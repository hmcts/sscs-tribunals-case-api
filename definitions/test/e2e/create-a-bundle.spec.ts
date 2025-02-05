import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';

let caseId: string;
test.beforeAll('Case has to be Created', async () => {
  caseId = await createCaseBasedOnCaseType('PIP');
});

test(
  'As a caseworker create a bundle',
  { tag: ['@preview-regression', '@master-pipeline', '@nightly-pipeline'] },
  async ({ createBundleSteps }) => {
    test.slow();
    await createBundleSteps.performUploadBundleResponse(caseId);
  }
);
