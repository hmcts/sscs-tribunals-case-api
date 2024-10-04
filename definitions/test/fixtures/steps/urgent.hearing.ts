import { BaseStep } from "./base";
import { expect, Page } from '@playwright/test';
import { credentials } from "../../config/config";
import task from '../../pages/content/review.urgent.hearing.request.task_en.json';
import sendCaseToTcwData from '../../pages/content/send.case.to.tcw_en.json';
import amendInterlocReviewStateData from "../../pages/content/amend.interloc.review.state_en.json";
import dateUtilsComponent from '../../utils/DateUtilsComponent';
import { SendToAdmin } from "./send.to.admin";
import { VoidCase } from "./void.case";
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import logger from "../../utils/loggerUtil";
import {accessId, accessToken, getSSCSServiceToken} from "../../api/client/idam/idam.service";
import {performEventOnCaseWithUploadResponse} from "../../api/client/sscs/factory/appeal.update.factory";
import eventTestData from "../../pages/content/event.name.event.description_en.json";

const actionFurtherEvidenceTestdata = require('../../pages/content/action.further.evidence_en.json');
const issueDirectionTestdata = require('../../pages/content/issue.direction_en.json');


export class UrgentHearing extends BaseStep {

    readonly page: Page;

    constructor(page){
        
        super(page);
        this.page = page;
    }

    async requestAndGrantAnUrgentHearing(caseId: string) {

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent(actionFurtherEvidenceTestdata.eventName);
        await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
            actionFurtherEvidenceTestdata.sender, 
            actionFurtherEvidenceTestdata.urgentDocType,
            actionFurtherEvidenceTestdata.testfileone
        );
        await this.eventNameAndDescriptionPage.verifyPageContent(actionFurtherEvidenceTestdata.eventName);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.summaryTab.verifyPageContentByKeyValue('Urgent case', 'Yes');

        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifydueDates('Urgent hearing registered');
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('Urgent hearing outcome', 'In progress');
        await this.verifyHistoryTabDetails('With FTA', 'Mark case as urgent');
        await this.historyTab.verifyPageContentByKeyValue('Interlocutory review state', 'Review by Judge');
        await this.homePage.reloadPage();

        await this.loginUserWithCaseId(credentials.judge, true, caseId);
        await this.homePage.chooseEvent(issueDirectionTestdata.eventNameCaptor);

        await this.issueDirectionPage.submitIssueDirection(
            issueDirectionTestdata.preHearingType,
            issueDirectionTestdata.grantHearingOption, 
            issueDirectionTestdata.docTitle
        );
        await this.eventNameAndDescriptionPage.verifyPageContent(issueDirectionTestdata.eventNameCaptor);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.summaryTab.verifyPageContentByKeyValue('Urgent case', 'Yes');
        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifydueDates('Urgent hearing registered');
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('Urgent hearing outcome', 'Granted');
        await this.verifyHistoryTabDetails('With FTA', 'Issue directions notice');
        await this.historyTab.verifyPageContentByKeyValue('Interlocutory review state', 'Awaiting Admin Action');
    }

    async requestAndRefuseAnUrgentHearing(caseId: string) {
        
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent(actionFurtherEvidenceTestdata.eventName);
        await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
            actionFurtherEvidenceTestdata.sender, 
            actionFurtherEvidenceTestdata.urgentDocType,
            actionFurtherEvidenceTestdata.testfileone
        );

        await this.eventNameAndDescriptionPage.verifyPageContent(actionFurtherEvidenceTestdata.eventName);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.summaryTab.verifyPageContentByKeyValue('Urgent case', 'Yes');

        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifydueDates('Urgent hearing registered');
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('Urgent hearing outcome', 'In progress');
        await this.verifyHistoryTabDetails('With FTA', 'Mark case as urgent');
        await this.historyTab.verifyPageContentByKeyValue('Interlocutory review state', 'Review by Judge');
        await this.homePage.reloadPage();

        await this.loginUserWithCaseId(credentials.judge, true, caseId);
        await this.homePage.chooseEvent(issueDirectionTestdata.eventNameCaptor);

        await this.issueDirectionPage.submitIssueDirection(
            issueDirectionTestdata.preHearingType,
            issueDirectionTestdata.refuseHearingOption, 
            issueDirectionTestdata.docTitle
        );
        await this.eventNameAndDescriptionPage.verifyPageContent(issueDirectionTestdata.eventNameCaptor);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.summaryTab.verifyPageContentByKeyValue('Urgent case', 'No');
        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifydueDates('Urgent hearing registered');
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('Urgent hearing outcome', 'Refused');
        await this.verifyHistoryTabDetails('With FTA', 'Issue directions notice');
        await this.historyTab.verifyPageContentByKeyValue('Interlocutory review state', 'N/A');
    }

    async requestAnUrgentHearingForAWelshCase() {

        let welshPipCaseId : string = await createCaseBasedOnCaseType('WELSHPIP');
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, welshPipCaseId);
        await this.homePage.reloadPage();

        //The below Steps is required as a Welsh Case is assumed to have it's creation document to
        // be translated and thus an auto stop for Send to FTA till that happens
        await this.homePage.chooseEvent("Welsh - cancel translations");
        await this.eventNameAndDescriptionPage.verifyPageContent("Welsh - cancel translations");
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await new Promise(f => setTimeout(f, 10000)); //Delay required for the Case to be ready

        /*logger.info('The value of the response writer : ' + credentials.dwpResponseWriter.email)
        let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            welshPipCaseId.trim(), 'dwpUploadResponse', 'dwp');*/

        await this.homePage.chooseEvent(actionFurtherEvidenceTestdata.eventName);
        await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
            actionFurtherEvidenceTestdata.sender,
            actionFurtherEvidenceTestdata.urgentDocType,
            actionFurtherEvidenceTestdata.testfileone
        );
        await this.eventNameAndDescriptionPage.verifyPageContent(actionFurtherEvidenceTestdata.eventName);
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready
        await this.summaryTab.verifyPageContentByKeyValueDoesNotExist('Urgent case', 'No');

        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready
        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifyAppealDetailsPageContentDoesNotExistByKeyValue('Urgent hearing outcome', 'Refused');

        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready
        await this.homePage.navigateToTab("History");
        await this.historyTab.verifyPageContentDoesNotExistByKeyValue('Interlocutory review state', 'N/A');

        await new Promise(f => setTimeout(f, 1000)); //Delay required for the Case to be ready
        await this.homePage.navigateToTab("Documents");
        await this.documentsTab.verifyFieldVisible("Evidence issued");
        await this.documentsTab.verifyFieldVisible("Yes");
        await this.documentsTab.verifyFieldVisible("Bundle addition");
        await this.documentsTab.verifyFieldVisible("Translation status");
        await this.documentsTab.verifyFieldVisible("Translation Required");

        await this.homePage.chooseEvent("Welsh - request translations");
        await this.eventNameAndDescriptionPage.verifyPageContent("Welsh - request translations");
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await new Promise(f => setTimeout(f, 5000)); //Delay required for the Case to be ready
        await this.homePage.navigateToTab("Documents");
        await this.documentsTab.verifyFieldVisible("Evidence issued");
        await this.documentsTab.verifyFieldVisible("Yes");
        await this.documentsTab.verifyFieldVisible("Bundle addition");
        await this.documentsTab.verifyFieldVisible("Translation status");
        await this.documentsTab.verifyFieldVisible("Translation Requested");
        //TODO - Further Verification that the Urgent Hearing Fields and the Interloculary State
        // should be verified post the Fix for SSCSCI-1013

    }

    async uploadEncryptedFiles(caseId: string) {

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent(actionFurtherEvidenceTestdata.eventName);
        await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
            actionFurtherEvidenceTestdata.sender,
            actionFurtherEvidenceTestdata.docType,
            actionFurtherEvidenceTestdata.encrytpedFile
        );
        await this.actionFurtherEvidencePage.verifyEncryptedFileErrorMsg();

        await this.actionFurtherEvidencePage.uploadDocs(actionFurtherEvidenceTestdata.corruptedFile);
        await this.actionFurtherEvidencePage.verifyEncryptedFileErrorMsg();
    }

    async allocateCaseToInterlocutoryJudge(caseId: string) {
        // CTSC Admin with case allocator role allocates case to Interlocutory Judge
        await this.loginUserWithCaseId(credentials.amCaseWorkerWithCaseAllocatorRole, false, caseId);
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);
        await this.homePage.navigateToTab('Roles and access');
        await this.rolesAndAccessTab.allocateInterlocutoryJudge(credentials.salariedJudge.email);
    }

    async requestAnUrgentHearing(caseId: string): Promise<void> {

        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent(actionFurtherEvidenceTestdata.eventName);
        await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
            actionFurtherEvidenceTestdata.sender, 
            actionFurtherEvidenceTestdata.urgentDocType,
            actionFurtherEvidenceTestdata.testfileone
        );
        await this.eventNameAndDescriptionPage.verifyPageContent(actionFurtherEvidenceTestdata.eventName);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.summaryTab.verifyPageContentByKeyValue('Urgent case', 'Yes');

        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifydueDates('Urgent hearing registered');
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('Urgent hearing outcome', 'In progress');
        await this.verifyHistoryTabDetails('With FTA', 'Mark case as urgent');
        await this.historyTab.verifyPageContentByKeyValue('Interlocutory review state', 'Review by Judge');
    }

    async verifyInterlocutoryJudgeCanViewTheAssignedReviewUrgentHearingRequestTask(caseId: string): Promise<void> {
         
        // Verify Review Urgent Hearing Request - Judge task is auto assigned to the Interlocutory Judge
         await this.loginUserWithCaseId(credentials.salariedJudge, false, caseId);
         await this.homePage.navigateToTab('Tasks');
         await this.tasksTab.verifyTaskIsDisplayed(task.name);
         await this.tasksTab.verifyPriortiy(task.name, task.priority);
         await this.tasksTab.verifyPageContentByKeyValue(task.name, 
            'Task created', dateUtilsComponent.formatDateToSpecifiedDateFormat(new Date()));
         await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToSalariedJudge);
         await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForSalariedJudge);
    }

    async verifyInterlocutoryJudgeCanCompleteTheAssignedReviewUrgentHearingRequestTask(caseId: string): Promise<void> {

        // Interlocutory Judge verfies the auto assigned task task details
        await this.loginUserWithCaseId(credentials.salariedJudge, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToSalariedJudge);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForSalariedJudge);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Interlocutory Judge clicks Issue directions notice next step link and completes the event
        await this.tasksTab.clickNextStepLink(task.issueDirectionsNotice.link);

        await this.issueDirectionPage.submitIssueDirection(
            issueDirectionTestdata.preHearingType,
            issueDirectionTestdata.grantHearingOption, 
            issueDirectionTestdata.docTitle
        );
        await this.eventNameAndDescriptionPage.verifyPageContent(issueDirectionTestdata.eventNameCaptor);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.summaryTab.verifyPageContentByKeyValue('Urgent case', 'Yes');
        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifydueDates('Urgent hearing registered');
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('Urgent hearing outcome', 'Granted');
        await this.verifyHistoryTabDetails('With FTA', 'Issue directions notice');
        await this.historyTab.verifyPageContentByKeyValue('Interlocutory review state', 'Awaiting Admin Action');

        // Interlocutory Judge verifies task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifySalariedJudgeCanViewTheUnassignedReviewUrgentHearingRequestTask(caseId: string): Promise<void> {
         
        // Verify Review Urgent Hearing Request - Judge task is displayed to the Salaried Judge
         await this.loginUserWithCaseId(credentials.salariedJudge, false, caseId);
         await this.homePage.navigateToTab('Tasks');
         await this.tasksTab.verifyTaskIsDisplayed(task.name);
         await this.tasksTab.verifyPriortiy(task.name, task.priority);
         await this.tasksTab.verifyPageContentByKeyValue(task.name, 
            'Task created', dateUtilsComponent.formatDateToSpecifiedDateFormat(new Date()));
         await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
         await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);
    }

    async verifySalariedJudgeCanCompleteTheUnassignedReviewUrgentHearingRequestTask(caseId: string): Promise<void> {

        // Verify Salaried Judge self assigns the task
        await this.loginUserWithCaseId(credentials.salariedJudge, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.selfAssignTask(task.name);
        
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToSalariedJudge);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForSalariedJudge);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Select Send casee to TCW next step and complete the event
        await this.tasksTab.clickNextStepLink(task.sendCaseToTcw.link);

        await this.sendCaseToTcwPage.verifyPageContent();
        await this.sendCaseToTcwPage.selectInterlocutoryReviewState(sendCaseToTcwData.interlocReviewStateSelectValue);
        await this.sendCaseToTcwPage.enterNote(sendCaseToTcwData.enterNoteInput);
        await this.sendCaseToTcwPage.confirmSubmission();

        await this.eventNameAndDescriptionPage.verifyPageContent(sendCaseToTcwData.caption);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.summaryTab.verifyPageContentByKeyValue('Urgent case', 'Yes');

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifySalariedJudgeCanReassignTheReviewUrgentHearingRequestTaskToTcw(caseId: string): Promise<void> {

        // Verify Salaried Judge self assigns the task
        await this.loginUserWithCaseId(credentials.salariedJudge, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.selfAssignTask(task.name);
        
        await this.tasksTab.reassignTaskToTcwUser(task.name, credentials.amTribunalCaseWorker.email);
    }

    async verifyFeePaidJudgeCanViewTheUnassignedReviewUrgentHearingRequestTask(caseId: string): Promise<void> {
         
        // Verify Review Urgent Hearing Request - Judge task is displayed to the Fee-Paid Judge
         await this.loginUserWithCaseId(credentials.feePaidJudge, false, caseId);
         await this.homePage.navigateToTab('Tasks');
         await this.tasksTab.verifyTaskIsDisplayed(task.name);
         await this.tasksTab.verifyPriortiy(task.name, task.priority);
         await this.tasksTab.verifyPageContentByKeyValue(task.name, 
            'Task created', dateUtilsComponent.formatDateToSpecifiedDateFormat(new Date()));
         await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
         await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);
    }

    async verifyFeePaidJudgeCanCompleteTheUnassignedReviewUrgentHearingRequestTask(caseId: string): Promise<void> {

        // Fee Paid Judge self assigns the task
        await this.loginUserWithCaseId(credentials.feePaidJudge, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.selfAssignTask(task.name);
        
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToFeePaidJudge);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForFeePaidJudge);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Select Action Further Evidence next step and complete the event
        await this.tasksTab.clickNextStepLink(task.sendToAdmin.link);

        let sendToAdmin = new SendToAdmin(this.page);
        await sendToAdmin.comepleteSendToAdmin();

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifyTcwCanCompleteTheAssignedReviewUrgentHearingRequestTask(caseId: string): Promise<void> {

        // Verify Review Urgent Hearing request task is assigned to TCW
        await this.loginUserWithCaseId(credentials.amTribunalCaseWorker, false, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToTCW);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForTCW);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // TCW clicks Amend interloc review state next step and completes the event
        await this.tasksTab.clickNextStepLink(task.amendInterlocReviewState.link);

        await this.amendInterlocReviewStatePage.verifyPageContent();
        await this.amendInterlocReviewStatePage.selectReviewState(amendInterlocReviewStateData.interlocReviewStateSelectValue);
        await this.amendInterlocReviewStatePage.confirmSelection();
        await this.amendInterlocReviewStatePage.confirmSubmission();

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifySalariedJudgeCanCompleteTheReviewUrgentHearingRequestTaskManually(caseId: string): Promise<void> {

        // Verify Salaried Judge self assigns the task
        await this.loginUserWithCaseId(credentials.salariedJudge, false, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.selfAssignTask(task.name);
        
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToSalariedJudge);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForSalariedJudge);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Judge selects to mark the task as done
        await this.tasksTab.markTheTaskAsDone(task.name);

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifySalariedJudgeCanViewAndSelfAssignTheReviewUrgentHearingRequestTask(caseId: string): Promise<void> {

        // Verify Review Urgent Hearing Request - Judge task is displayed to the Salaried Judge
        await this.verifySalariedJudgeCanViewTheUnassignedReviewUrgentHearingRequestTask(caseId);
        await this.tasksTab.selfAssignTask(task.name);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToSalariedJudge);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForSalariedJudge);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);
    }

    async verifyReviewUrgentHearingRequestTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId: string): Promise<void> {

        // CTSC Admin voids the case
        let voidCase = new VoidCase(this.page);
        await voidCase.performVoidCase(caseId);

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }
}
