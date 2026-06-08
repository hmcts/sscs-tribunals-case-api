import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';

let caseId: string;

test.describe('Universal Credit confidentiality', { tag: '@nightly-pipeline' }, async () => {
  test.beforeAll('Case has to be Created', async () => {
    caseId = await createCaseBasedOnCaseType('UC');
  });

  test('UC - add other party - confirm confidentiality', async ({ }) => {
    test.setTimeout(240000);
  });
});
