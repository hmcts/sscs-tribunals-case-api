import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';

test(
    'Caseworker raise new request & FTA replies to the query & Caseworker reviews FTA reply',
    async ({ communicateWithFtaSteps }) => {
        let caseId = await createCaseBasedOnCaseType('PIP');
        // Caseworker raise new request
        await communicateWithFtaSteps.communicateWithFta(caseId);
        //Completes the communication with FTA request grabs details from history tab for verification
        await communicateWithFtaSteps.submitNewCommunicationRequesttoFta();

        // FTA replies to the query
        await communicateWithFtaSteps.replyToCaseWorkersQueryToFta(caseId);

        // Caseworker reviews FTA reply
        await communicateWithFtaSteps.reviewFtaReply(caseId);
    });