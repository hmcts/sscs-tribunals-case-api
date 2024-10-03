import {Page} from '@playwright/test';
import {BaseStep} from './base';
import {credentials} from "../../config/config";
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import {accessId, accessToken, getSSCSServiceToken} from "../../api/client/idam/idam.service";
import {
    performEventOnCaseForActionFurtherEvidence,
    performEventOnCaseWithUploadResponse
} from "../../api/client/sscs/factory/appeal.update.factory";
import issueDirectionTestdata from "../../pages/content/issue.direction_en.json";
import eventTestData from "../../pages/content/event.name.event.description_en.json";
import actionFurtherEvidenceTestdata from '../../pages/content/action.further.evidence_en.json';
import sendToInterLocPreValidData from '../../pages/content/send.to.interloc_en.json';
import logger from "../../utils/loggerUtil";
import performAppealDormantOnCase from "../../api/client/sscs/appeal.event";

export class IssueDirectionsNotice extends BaseStep {

    readonly page: Page;


    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performIssueDirectionNoticeIncompleteApplicationPreHearingAppealToProceed() {

        let pipCaseId = await createCaseBasedOnCaseType('PIPINCOMPLETE');
        await new Promise(f => setTimeout(f, 10000)); //Delay required for the Case to be ready
        logger.info('The value of the response writer : ' + credentials.dwpResponseWriter.email)
        /*let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            pipCaseId.trim(), 'dwpUploadResponse', 'dwp');*/

        /*logger.info('The value of the response writer : '+credentials.amCaseWorker.email)
        let caseWorkerToken: string = await accessToken(credentials.amCaseWorker);
        let serviceTokenForCaseWorker: string = await getSSCSServiceToken();
        let caseWorkerId: string = await accessId(credentials.amCaseWorker);
        await new Promise(f => setTimeout(f, 20000)); //Delay required for the Case to be ready
        await performEventOnCaseForActionFurtherEvidence(caseWorkerToken.trim(),
            serviceTokenForCaseWorker.trim(),caseWorkerId.trim(),'SSCS','Benefit',
            taxCreditCaseId.trim(), 'uploadDocumentFurtherEvidence');*/

        //This block would also act as a Test for the Send to interloc - pre-valid Event. SSCSSI-228
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, pipCaseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent("Send to interloc - pre-valid");
        await this.sendToInterlocPrevalidPage.verifyPageContentForTheInterlocReferralPage();
        await this.sendToInterlocPrevalidPage.inputReasonForReferral();
        await this.sendToInterlocPrevalidPage.submitContinueBtn();
        await this.textAreaPage.verifyPageContent(sendToInterLocPreValidData.sendToInterLocPreValidCaption, sendToInterLocPreValidData.appealNotepad, sendToInterLocPreValidData.enterNoteOptionalLabel);
        await this.textAreaPage.inputData(sendToInterLocPreValidData.appealNotepadInput);
        await this.sendToInterlocPrevalidPage.confirmSubmission();
        await this.eventNameAndDescriptionPage.verifyPageContent(sendToInterLocPreValidData.sendToInterLocPreValidCaption,
            false);
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.verifyHistoryTabDetails("Send to interloc - pre-valid");
        await this.homePage.signOut();
        await new Promise(f => setTimeout(f, 2000)); //Delay required for the Case to be ready


        await this.loginUserWithCaseId(credentials.judge, false, pipCaseId);
        await new Promise(f => setTimeout(f, 12000)); //Delay required for the Case to be ready
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent("Issue directions notice");

        await this.issueDirectionPage.verifyPageContent();
        await this.issueDirectionPage.populatePreHearingAppealToProceed(
            issueDirectionTestdata.preHearingType,
            'Appeal to Proceed',
            issueDirectionTestdata.docTitle);

        await this.eventNameAndDescriptionPage.verifyPageContent("Issue directions notice",
            true, "Direction type");
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.verifyHistoryTabDetails("Issue directions notice");
        // await performAppealDormantOnCase(pipCaseId);
    }

    async performIssueDirectionNoticePreHearingAppealToProceed() {

        let taxCreditCaseId = await createCaseBasedOnCaseType('TAX CREDIT');
        await new Promise(f => setTimeout(f, 30000)); //Delay required for the Case to be ready
        logger.info('The value of the response writer : ' + credentials.hmrcUser.email)
        /* let responseWriterToken: string = await accessToken(credentials.hmrcUser);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.hmrcUser);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            taxCreditCaseId.trim(), 'dwpUploadResponse', 'hmrc'); */

        /*logger.info('The value of the response writer : '+credentials.amCaseWorker.email)
        let caseWorkerToken: string = await accessToken(credentials.amCaseWorker);
        let serviceTokenForCaseWorker: string = await getSSCSServiceToken();
        let caseWorkerId: string = await accessId(credentials.amCaseWorker);
        await new Promise(f => setTimeout(f, 20000)); //Delay required for the Case to be ready
        await performEventOnCaseForActionFurtherEvidence(caseWorkerToken.trim(),
            serviceTokenForCaseWorker.trim(),caseWorkerId.trim(),'SSCS','Benefit',
            taxCreditCaseId.trim(), 'uploadDocumentFurtherEvidence');*/

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, taxCreditCaseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent(actionFurtherEvidenceTestdata.eventNameCaptor);
        await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
            actionFurtherEvidenceTestdata.sender,
            actionFurtherEvidenceTestdata.other,
            actionFurtherEvidenceTestdata.testfileone
        );
        await new Promise(f => setTimeout(f, 2000)); //Delay required for the Case to be ready
        await this.homePage.signOut();
        await new Promise(f => setTimeout(f, 2000)); //Delay required for the Case to be ready


        await this.loginUserWithCaseId(credentials.judge, false, taxCreditCaseId);
        await new Promise(f => setTimeout(f, 12000)); //Delay required for the Case to be ready
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent("Issue directions notice");

        await this.issueDirectionPage.verifyPageContent();
        await this.issueDirectionPage.populatePreHearingAppealToProceed(
            issueDirectionTestdata.preHearingType,
            'Appeal to Proceed',
            issueDirectionTestdata.docTitle);

        await this.eventNameAndDescriptionPage.verifyPageContent("Issue directions notice",
            true, "Direction type");
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.verifyHistoryTabDetails("Issue directions notice");
        // await performAppealDormantOnCase(taxCreditCaseId);
    }


    async performIssueDirectionNoticePostHearingESAAppealToProceed() {

        //let esaCaseId = await createCaseBasedOnCaseType('ESA');
        let esaCaseId = await createCaseBasedOnCaseType('PIP');
        await new Promise(f => setTimeout(f, 30000)); //Delay required for the Case to be ready
        logger.info('The value of the response writer : ' + credentials.dwpResponseWriter.email)
        /*let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            esaCaseId.trim(), 'dwpUploadResponse', 'dwp');*/

        /*logger.info('The value of the response writer : '+credentials.amCaseWorker.email)
        let caseWorkerToken: string = await accessToken(credentials.amCaseWorker);
        let serviceTokenForCaseWorker: string = await getSSCSServiceToken();
        let caseWorkerId: string = await accessId(credentials.amCaseWorker);
        await new Promise(f => setTimeout(f, 20000)); //Delay required for the Case to be ready
        await performEventOnCaseForActionFurtherEvidence(caseWorkerToken.trim(),
            serviceTokenForCaseWorker.trim(),caseWorkerId.trim(),'SSCS','Benefit',
            taxCreditCaseId.trim(), 'uploadDocumentFurtherEvidence');*/

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, esaCaseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent(actionFurtherEvidenceTestdata.eventNameCaptor);
        await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
            actionFurtherEvidenceTestdata.sender,
            actionFurtherEvidenceTestdata.other,
            actionFurtherEvidenceTestdata.testfileone
        );
        await this.homePage.signOut();
        await new Promise(f => setTimeout(f, 2000)); //Delay required for the Case to be ready

        await this.loginUserWithCaseId(credentials.judge, false, esaCaseId);
        await new Promise(f => setTimeout(f, 12000)); //Delay required for the Case to be ready
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent("Issue directions notice");

        await this.issueDirectionPage.verifyPageContent();
        await this.issueDirectionPage.populatePostHearingESAAppealToProceed(
            issueDirectionTestdata.postHearingType,
            'Appeal to Proceed',
            issueDirectionTestdata.docTitle);

        await this.eventNameAndDescriptionPage.verifyPageContent("Issue directions notice",
            true, "Direction type");
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.verifyHistoryTabDetails("Issue directions notice");
        // await performAppealDormantOnCase(esaCaseId);
    }

    async performIssueDirectionNoticePostHearingDLAAppealToProceed() {

        //let pipCaseId = await createCaseBasedOnCaseType('DLASANDL');
        let pipCaseId = await createCaseBasedOnCaseType('PIP');
        await new Promise(f => setTimeout(f, 30000)); //Delay required for the Case to be ready
        logger.info('The value of the response writer : ' + credentials.dwpResponseWriter.email)
        /*let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            pipCaseId.trim(), 'dwpUploadResponse', 'dwp');*/

        /*logger.info('The value of the response writer : '+credentials.amCaseWorker.email)
        let caseWorkerToken: string = await accessToken(credentials.amCaseWorker);
        let serviceTokenForCaseWorker: string = await getSSCSServiceToken();
        let caseWorkerId: string = await accessId(credentials.amCaseWorker);
        await new Promise(f => setTimeout(f, 20000)); //Delay required for the Case to be ready
        await performEventOnCaseForActionFurtherEvidence(caseWorkerToken.trim(),
            serviceTokenForCaseWorker.trim(),caseWorkerId.trim(),'SSCS','Benefit',
            taxCreditCaseId.trim(), 'uploadDocumentFurtherEvidence');*/

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, pipCaseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent(actionFurtherEvidenceTestdata.eventNameCaptor);
        await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
            actionFurtherEvidenceTestdata.sender,
            actionFurtherEvidenceTestdata.other,
            actionFurtherEvidenceTestdata.testfileone
        );
        await this.homePage.signOut();
        await new Promise(f => setTimeout(f, 2000)); //Delay required for the Case to be ready

        await this.loginUserWithCaseId(credentials.judge, false, pipCaseId);
        await new Promise(f => setTimeout(f, 12000)); //Delay required for the Case to be ready
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent("Issue directions notice");

        await this.issueDirectionPage.verifyPageContent();
        await this.issueDirectionPage.populatePostHearingDLAProvideInformation(
            issueDirectionTestdata.postHearingType,
            'Provide information',
            issueDirectionTestdata.docTitle);
        await this.eventNameAndDescriptionPage.verifyPageContent("Issue directions notice",
            true, "Direction type");
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.verifyHistoryTabDetails("Issue directions notice");
    }


    async performIssueDirectionErrorMessages() {
        //let pipCaseId = await createCaseBasedOnCaseType('DLASANDL');
        let pipCaseId = await createCaseBasedOnCaseType('PIP');
        await new Promise(f => setTimeout(f, 30000)); //Delay required for the Case to be ready
        logger.info('The value of the response writer : ' + credentials.dwpResponseWriter.email)
        /*let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            pipCaseId.trim(), 'dwpUploadResponse', 'dwp');*/

        /*logger.info('The value of the response writer : '+credentials.amCaseWorker.email)
        let caseWorkerToken: string = await accessToken(credentials.amCaseWorker);
        let serviceTokenForCaseWorker: string = await getSSCSServiceToken();
        let caseWorkerId: string = await accessId(credentials.amCaseWorker);
        await new Promise(f => setTimeout(f, 20000)); //Delay required for the Case to be ready
        await performEventOnCaseForActionFurtherEvidence(caseWorkerToken.trim(),
            serviceTokenForCaseWorker.trim(),caseWorkerId.trim(),'SSCS','Benefit',
            taxCreditCaseId.trim(), 'uploadDocumentFurtherEvidence');*/

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, pipCaseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent(actionFurtherEvidenceTestdata.eventNameCaptor);
        await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
            actionFurtherEvidenceTestdata.sender,
            actionFurtherEvidenceTestdata.other,
            actionFurtherEvidenceTestdata.testfileone
        );
        await this.homePage.signOut();
        await new Promise(f => setTimeout(f, 2000)); //Delay required for the Case to be ready

        await this.loginUserWithCaseId(credentials.judge, false, pipCaseId);
        await new Promise(f => setTimeout(f, 12000)); //Delay required for the Case to be ready
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent("Issue directions notice");
        await this.issueDirectionPage.verifyPageContent();
        await this.issueDirectionPage.confirmSubmission();

        await this.issueDirectionPage.verifyErrorMsg(true,false,false);
        await this.issueDirectionPage.chooseRecipients('#confidentialityType-confidential');
        await this.issueDirectionPage.confirmSubmission();
        await this.issueDirectionPage.verifyErrorMsg(false,true,false);

        await this.issueDirectionPage.chooseNoticeType('#generateNotice_Yes');
        await this.issueDirectionPage.confirmSubmission();
        await this.issueDirectionPage.verifyErrorMsg(false,false,true);
    }
}
