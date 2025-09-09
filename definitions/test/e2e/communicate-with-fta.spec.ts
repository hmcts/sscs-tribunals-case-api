import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';

let caseId: string;
test.beforeAll('Case has to be Created', async () => {
    caseId = await createCaseBasedOnCaseType('PIP');
});
test(
    'Caseworker raise new request & FTA replies to the query & Caseworker reviews FTA reply',
    async ({ communicateWithFtaSteps,  }) => {
        await communicateWithFtaSteps.communicateWithFta(caseId);
        let dateOfEvent = await communicateWithFtaSteps.submitCommunicationRequesttoFta('New Request', 'Appeal Type');
        
    });