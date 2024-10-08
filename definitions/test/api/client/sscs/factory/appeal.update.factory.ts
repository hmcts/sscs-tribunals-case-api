/* eslint-disable complexity */
import {request} from '@playwright/test';
import {urls} from '../../../../config/config';
import logger from '../../../../utils/loggerUtil';
import upload_response_payload_dwp_022_EC from '../../../data/payload/upload-response/upload-response-dwp-022-CC.json';
import upload_response_payload_hmrc_053_DQ
    from '../../../data/payload/upload-response/upload-response-hmrc-053-DQ.json';
import upload_response_dwp_av_002_PIP from '../../../data/payload/upload-response/upload-response-dwp-av-002-PIP.json';
import action_further_evidence_payload
    from '../../../data/payload/action-further-evidence/action-further-evidence-other.json';

let apiContext;

async function getStartEventTokenOnCase(idamToken: string,
                                        serviceToken: string,
                                        userId: string,
                                        jurisdiction: string,
                                        caseType: string,
                                        caseId: string, eventId: string) {
    //Formulate API Context For Request,wrapping the Request Endpoint
    apiContext = await request.newContext({
        // All requests we send go to this API Endpoint.
        baseURL: urls.ccdApiUrl,
    });
    logger.info(`The value the CCD URL : ${urls.ccdApiUrl}/caseworkers/${userId}/jurisdictions/${jurisdiction}/case-types/${caseType}/cases/${caseId}/event-triggers/${eventId}/token`);
    const responseForStartAnEvent = await apiContext
        .get(`${urls.ccdApiUrl}/caseworkers/${userId}/jurisdictions/${jurisdiction}/case-types/${caseType}/cases/${caseId}/event-triggers/${eventId}/token`, {
            headers: {
                Authorization: `Bearer ${idamToken}`,
                ServiceAuthorization: `${serviceToken}`,
                'content-type': 'application/json',
            }
        });
    logger.info('The value of the status for the start event :' + responseForStartAnEvent.status());
    logger.info('The value of the status text for the start event:' + responseForStartAnEvent.statusText());
    const body: string = await responseForStartAnEvent.body();
    await responseForStartAnEvent.dispose();
    return body;
}

async function performEventOnCaseWithEmptyBody(idamToken: string,
                                               serviceToken: string,
                                               userId: string,
                                               jurisdiction: string,
                                               caseType: string,
                                               caseId: string, eventId: string) {

    //logger.info('The logger value for the body : ' + body);
    let body: string = await getStartEventTokenOnCase(idamToken,
        serviceToken,
        userId,
        jurisdiction,
        caseType,
        caseId, eventId);
    let event_token: string = JSON.parse(body).token;
    logger.info('Retrieved the token : ' + event_token);

    let dataPayload = {
        event: {
            id: `${eventId}`,
            summary: `Summary for the ${eventId} of ${caseId}`,
            description: `Description for the ${eventId} of ${caseId}`,
        },
        data: {},
        event_token: `${event_token}`,
        ignore_warning: true,
    }

    logger.info('The payload : ' + JSON.stringify(dataPayload));

    let endURL: string = `${urls.ccdApiUrl}/caseworkers/${userId}/jurisdictions/${jurisdiction}/case-types/${caseType}/cases/${caseId}/events`;
    logger.info('The end URL : ' + endURL);

    const responseForSubmitAnEvent = await apiContext
        .post(`${endURL}`, {
            headers: {
                Authorization: `Bearer ${idamToken}`,
                ServiceAuthorization: `${serviceToken}`,
                'content-type': 'application/json',
            },
            data: dataPayload
        });
    logger.info('The value of the status for the submit event :' + responseForSubmitAnEvent.status());
    logger.info('The value of the status text for the submit event :' + responseForSubmitAnEvent.statusText());


    if (responseForSubmitAnEvent.status() != 201) {
        throw new Error("Error : Could not set the case to Dormant with status code - "
            + responseForSubmitAnEvent.status() + " and message " + responseForSubmitAnEvent.statusText());
    }
}

async function performEventOnCaseWithUploadResponse(idamToken: string,
                                                    serviceToken: string,
                                                    userId: string,
                                                    jurisdiction: string,
                                                    caseType: string,
                                                    caseId: string,
                                                    eventId: string,
                                                    ftaAuthority: string) {
    let body: string = await getStartEventTokenOnCase(idamToken,
        serviceToken,
        userId,
        jurisdiction,
        caseType,
        caseId, eventId);
    let event_token: string = JSON.parse(body).token;
    let upload_response_payload = {};
    if (ftaAuthority === 'dwp') {
        upload_response_payload = upload_response_payload_dwp_022_EC;
    } else if (ftaAuthority === 'av') {
        upload_response_payload = upload_response_dwp_av_002_PIP;
    } else if (ftaAuthority === 'hmrc') {
        upload_response_payload = upload_response_payload_hmrc_053_DQ;
    }

    //logger.info("The value of the case Details " + JSON.stringify(case_details));
    let dataPayload = {
        event: {
            id: `${eventId}`,
            summary: `Summary for the ${eventId} of ${caseId}`,
            description: `Description for the ${eventId} of ${caseId}`,
        },
        data: upload_response_payload,
        event_token: `${event_token}`,
        ignore_warning: true,
    }
    logger.debug("The value of the Upload Response Payload : " + JSON.stringify(dataPayload));
    await performEventSubmission(userId, jurisdiction, caseType, caseId, idamToken, serviceToken, dataPayload);
}

async function performEventOnCaseForActionFurtherEvidence(idamToken: string,
                                  serviceToken: string,
                                  userId: string,
                                  jurisdiction: string,
                                  caseType: string,
                                  caseId: string,
                                  eventId: string) {
    let body: string = await getStartEventTokenOnCase(idamToken,
        serviceToken,
        userId,
        jurisdiction,
        caseType,
        caseId, eventId);
    let event_token: string = JSON.parse(body).token;


    //logger.info("The value of the case Details " + JSON.stringify(case_details));
    let dataPayload = {
        event: {
            id: `${eventId}`,
            summary: `Summary for the ${eventId} of ${caseId}`,
            description: `Description for the ${eventId} of ${caseId}`,
        },
        data: action_further_evidence_payload,
        event_token: `${event_token}`,
        ignore_warning: true,
    }
    logger.debug("The value of the Upload Response Payload : " + JSON.stringify(dataPayload));
    await performEventSubmission(userId, jurisdiction, caseType, caseId, idamToken, serviceToken, dataPayload);
}

async function performEventSubmission(userId, jurisdiction, caseType, caseId, idamToken, serviceToken, dataPayload) {
    let endURL: string = `${urls.ccdApiUrl}/caseworkers/${userId}/jurisdictions/${jurisdiction}/case-types/${caseType}/cases/${caseId}/events`;
    logger.debug('The end URL : ' + endURL);


    const responseForSubmitAnEvent = await apiContext
        .post(`${endURL}`, {
            headers: {
                Authorization: `Bearer ${idamToken}`,
                ServiceAuthorization: `${serviceToken}`,
                'content-type': 'application/json',
            },
            data: dataPayload
        });
    logger.info('The value of the status for the submit event :' + responseForSubmitAnEvent.status());
    logger.info('The value of the status text for the submit event :' + responseForSubmitAnEvent.statusText());


    if (responseForSubmitAnEvent.status() != 201) {
        throw new Error("Error : Could not perform the Upload Response with status code - "
            + responseForSubmitAnEvent.status() + " and message " + responseForSubmitAnEvent.statusText());
    }
}

export {performEventOnCaseWithEmptyBody, performEventOnCaseWithUploadResponse, performEventOnCaseForActionFurtherEvidence}
