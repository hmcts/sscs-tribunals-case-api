import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';
import { EventDetails } from '../fixtures/steps/communicate-with-fta';


let caseId: string;
test.beforeAll('Case has to be Created', async () => {

});
test(
    'Caseworker raise new request & FTA replies to the query & Caseworker reviews FTA reply',
    async ({ communicateWithFtaSteps }) => {
        caseId = await createCaseBasedOnCaseType('PIP');
        // Caseworker raise new request
        await communicateWithFtaSteps.communicateWithFta(caseId);
        //Completes the communication with FTA request grabs details from history tab for verification
        let eventDetails: EventDetails = await communicateWithFtaSteps.submitNewCommunicationRequesttoFta();

        let dateOfEvent = eventDetails.dateOfEvent;
        let authorOfEvent = eventDetails.authorOfEvent;

        // FTA replies to the query
        await communicateWithFtaSteps.replyToCaseWorkersQueryToFta(caseId);

        // Caseworker reviews FTA reply
        await communicateWithFtaSteps.reviewFtaReply(caseId);
    });