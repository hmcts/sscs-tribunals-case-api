import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';


let caseId: string;

test('Caseworker raise new request & FTA(DWP) replies to the query & Caseworker reviews FTA(DWP) reply',
    { tag: '@nightly-pipeline' },
    async ({ communicateWithFtaSteps }) => {
        caseId = await createCaseBasedOnCaseType('PIP');

        // Caseworker raise new request
        await communicateWithFtaSteps.communicateWithUser(caseId, 'Communication with FTA', 'amTribunalCaseWorker');

        //Completes the communication with FTA request grabs details from history tab for verification
        await communicateWithFtaSteps.submitNewCommunicationRequest({ isCommsToFta: true });

        // FTA replies to the query
        await communicateWithFtaSteps.replyToQuery(caseId, 'dwpResponseWriter', {
            event: 'Communication with Tribunal',
            replyMethod: 'replyToTribunalQuery'
        });

        // Caseworker reviews FTA reply
        await communicateWithFtaSteps.reviewUserReply(caseId, {
            tribsVerifyReply: true,
            userType: 'amTribunalCaseWorker',
            event: 'Communication with FTA',
            reviewMethod: 'fillOutReviewFtaReply'
        });
    });


test('FTA(DWP) raises a new request & Caseworker replies to the query & FTA(DWP) reviews Caseworker reply',
    { tag: '@nightly-pipeline' },
    async ({ communicateWithFtaSteps }) => {
        caseId = await createCaseBasedOnCaseType('PIP');

        // FTA raises a new request
        await communicateWithFtaSteps.communicateWithUser(caseId, 'Communication with Tribunal', 'dwpResponseWriter');

        //Completes the communication with Tribunal request grabs details from history tab for verification
        await communicateWithFtaSteps.submitNewCommunicationRequest({ isCommsToFta: false });

        // Caseworker replies to the query
        await communicateWithFtaSteps.replyToQuery(caseId, 'amTribunalCaseWorker', {
            event: 'Communication with FTA',
            replyMethod: 'replyToFTAQuery'
        });

        // FTA reviews Caseworker reply
        await communicateWithFtaSteps.reviewUserReply(caseId, {
            tribsVerifyReply: false,
            userType: 'dwpResponseWriter',
            event: 'Communication with Tribunal',
            reviewMethod: 'fillOutReviewTribunalReply'
        });

    }
)

test('Delete a request sent to FTA(DWP)',
    { tag: '@nightly-pipeline' },
    async ({ communicateWithFtaSteps }) => {
        caseId = await createCaseBasedOnCaseType('PIP');

        // Caseworker raise new request
        await communicateWithFtaSteps.communicateWithUser(caseId, 'Communication with FTA', 'amTribunalCaseWorker');

        //Completes the communication with FTA request
        await communicateWithFtaSteps.submitNewCommunicationRequest({ isCommsToFta: true });

        // Caseworker deletes the request
        await communicateWithFtaSteps.deleteRequestOrReply();
    }
)