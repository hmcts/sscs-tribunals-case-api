import {test} from "../lib/steps.factory";
import {credentials} from "../config/config"
import {accessToken, getSSCSServiceToken, accessId, getIDAMUserID} from "../api/client/idam/idam.service";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import {
    performEventOnCaseWithEmptyBody,
    performEventOnCaseWithUploadResponse
} from "../api/client/sscs/factory/appeal.update.factory";
import logger from "../utils/loggerUtil";
//var event_token: string = JSON.parse(response_document).push({hello: 'value'});
import fs from 'fs';

test("Test to Test API Working....", async () => {

    let pipCaseId = await createCaseBasedOnCaseType('PIP');
    await new Promise(f => setTimeout(f, 10000)); //Delay required for the Case to be ready
    logger.info('The value of the response writer : ' + credentials.dwpResponseWriter.email)
    let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
    let serviceToken: string = await getSSCSServiceToken();
    let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
    await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
        serviceToken.trim(), responseWriterId.trim(),
        'SSCS', 'Benefit',
        pipCaseId.trim(), 'dwpUploadResponse', 'dwp');

    /*logger.info('The value of the response writer : '+credentials.amCaseWorker.email)
       let caseWorkerToken: string = await accessToken(credentials.amCaseWorker);
       let serviceTokenForCaseWorker: string = await getSSCSServiceToken();
       let caseWorkerId: string = await accessId(credentials.amCaseWorker);
       await new Promise(f => setTimeout(f, 20000)); //Delay required for the Case to be ready
       await performEventOnCaseForActionFurtherEvidence(caseWorkerToken.trim(),
           serviceTokenForCaseWorker.trim(),caseWorkerId.trim(),'SSCS','Benefit',
           childSupportCaseId.trim(), 'uploadDocumentFurtherEvidence');*/


   /* let token: string = await accessToken(credentials.amSuperUser);
    let serviceToken: string = await getSSCSServiceToken();
    let userId: string = await accessId(credentials.amSuperUser);
    await new Promise(f => setTimeout(f, 3000)); //Delay required for the Case to be ready
     await performEventOnCaseWithEmptyBody(token.trim(),
         serviceToken.trim(), userId.trim(),
         'SSCS','Benefit',
         childSupportCaseId.trim(),'appealDormant');
    logger.info("The value of the IDAM Token : "+token);*/


});

/*test.only("Temporary testing of the JSON Node on Json Node", async ({addNoteSteps}) => {

    fs.readFile('./functional-test/api/data/payload/upload-response/upload-response-dwp-022-CC.json', function read(err, data) {
        if (err) {
            throw err;
        }
        const content = data;

        // Invoke the next step here however you like
        console.log(content.toString());   // Put all of the code here (not the best solution)
    });

    /!*var response_document = {
        documentLink: {
            document_url: "http://dm-store-aat.service.core-compute-aat.internal/documents/b4b8b038-1e11-49b3-b83e-13546cfc152d",
            document_binary_url: "http://dm-store-aat.service.core-compute-aat.internal/documents/b4b8b038-1e11-49b3-b83e-13546cfc152d/binary",
            document_filename: "Bloggs_IEF.pdf"
        },
        documentFilename: "Bloggs_IEF.pdf"
    };
    //var event_token: string = JSON.parse(response_document).push({hello: 'value'});
    console.log("The value of the event Token : "+response_document)*!/

});*/
