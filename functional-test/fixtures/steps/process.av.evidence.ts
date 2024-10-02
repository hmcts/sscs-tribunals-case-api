import { BaseStep } from "./base";
import { expect, Page } from '@playwright/test';
import { credentials } from "../../config/config";
import { timingSafeEqual } from "crypto";
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import {accessId, accessToken, getSSCSServiceToken} from "../../api/client/idam/idam.service";
import {
    performEventOnCaseWithUploadResponse
} from "../../api/client/sscs/factory/appeal.update.factory";
import logger from "../../utils/loggerUtil";
import task from '../../pages/content/process.audio.video.evidence_en.json';
import { VoidCase } from './void.case';
import { UploadDocumentFurtherEvidence } from "./upload.document.further.evidence";

const actionFurtherEvidenceTestdata = require('../../pages/content/action.further.evidence_en.json');
const issueDirectionTestdata = require('../../pages/content/issue.direction_en.json');


const uploadResponseTestdata = require('../../pages/content/upload.response_en.json');
const avEvidenceTestdata = require('../../pages/content/audio.video.evidence_en.json');

export class ProcessAVEvidence extends BaseStep {

    readonly page: Page;

    constructor(page){
        
        super(page);
        this.page = page;
    }

    async acceptEvidenceUploadedByDWP(caseId: string) {

        // let caseId = await createCaseBasedOnCaseType('PIP');
        // await new Promise(f => setTimeout(f, 10000)); //Delay required for the Case to be ready
        // logger.info('The value of the response writer : ' + credentials.dwpResponseWriter.email)
        // let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        // let serviceToken: string = await getSSCSServiceToken();
        // let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        // await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
        //     serviceToken.trim(), responseWriterId.trim(),
        //     'SSCS', 'Benefit',
        //     caseId.trim(), 'dwpUploadResponse', 'av');

        // await this.homePage.delay(5000);
        
        await this.loginUserWithCaseId(credentials.judge, false, caseId);
        await this.homePage.navigateToTab(avEvidenceTestdata.avTab);

        await this.avTab.verifyPageContentByKeyValue(avEvidenceTestdata.typeField, avEvidenceTestdata.typeValue);
        await this.avTab.verifyPageContentByKeyValue(avEvidenceTestdata.docField, uploadResponseTestdata.testaudiofile);
        await this.avTab.verifyPageContentByKeyValue(avEvidenceTestdata.partyField, avEvidenceTestdata.ftaValue);
        await this.avTab.verifydueDates('Date added');

        await this.homePage.chooseEvent(avEvidenceTestdata.eventNameCaptor);
        await this.processAVPage.selectRequestedEvidence('test_av.mp3');
        await this.processAVPage.verifyPageContent(avEvidenceTestdata.ftaValue);
        await this.processAVPage.grantApprovalEvidence();
        await this.processAVPage.continueOnPreviewDoc();

        await this.homePage.navigateToTab(avEvidenceTestdata.docTab);
        await this.documentsTab.verifyPageContentByKeyValue(avEvidenceTestdata.docTypeField, avEvidenceTestdata.docTypeValue);
        await this.documentsTab.verifyPageContentByKeyValue("Bundle addition", "A");
        await this.documentsTab.verifydueDates('Date added');

        await this.homePage.navigateToTab('FTA Documents');
        await this.documentsTab.verifyPageContentByKeyValue("Document type", "Audio document");
        await this.documentsTab.verifyPageContentByKeyValue("Document file name", "test_av.mp3");
        await this.documentsTab.verifyPageContentByKeyValue("Party uploaded", "dwp");
        await this.documentsTab.verifydueDates('Date added');

        await this.homePage.navigateToTab('History');
        await this.verifyHistoryTabDetails('Process audio/video evidence');
    }


    async excludeEvidenceUploadedByDWP(caseId: string) {

        // let caseId = await createCaseBasedOnCaseType('PIP');
        // await new Promise(f => setTimeout(f, 10000)); //Delay required for the Case to be ready
        // logger.info('The value of the response writer : ' + credentials.dwpResponseWriter.email)
        // let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        // let serviceToken: string = await getSSCSServiceToken();
        // let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        // await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
        //     serviceToken.trim(), responseWriterId.trim(),
        //     'SSCS', 'Benefit',
        //     caseId.trim(), 'dwpUploadResponse', 'av');

        // await this.homePage.delay(5000);
        
        await this.loginUserWithCaseId(credentials.judge, false, caseId);
        await this.homePage.navigateToTab('Audio/Video Evidence');

        await this.avTab.verifyPageContentByKeyValue('Document Type', 'audioDocument');
        await this.avTab.verifyPageContentByKeyValue('Audio/video document url', uploadResponseTestdata.testaudiofile);
        await this.avTab.verifyPageContentByKeyValue('Audio/video party uploaded', 'FTA');
        await this.avTab.verifydueDates('Date added');

        await this.homePage.chooseEvent('Process audio/video evidence');
        await this.processAVPage.selectRequestedEvidence('test_av.mp3');
        await this.processAVPage.verifyPageContent('FTA');
        await this.processAVPage.rejectApprovalEvidence();
        await this.processAVPage.continueOnPreviewDoc();

        await this.homePage.navigateToTab('Documents');
        await this.documentsTab.verifyPageContentByKeyValue("Type", "Audio/Video evidence direction notice");
        await this.documentsTab.verifyPageContentByKeyValue("Bundle addition", "A");
        await this.documentsTab.verifydueDates('Date added');

        await this.homePage.navigateToTab('FTA Documents');
        await this.documentsTab.verifyPageContentNotPresentByKeyValue("Document type", "Audio document");
        await this.documentsTab.verifyPageContentNotPresentByKeyValue("Document file name", "test_av.mp3");
        await this.documentsTab.verifyPageContentNotPresentByKeyValue("Party uploaded", "dwp");

        await this.homePage.navigateToTab('History');
        await this.verifyHistoryTabDetails('Process audio/video evidence');
    }

    async acceptEvidenceUploadedByCTSC(caseId: string) {
        
        await this.homePage.delay(3000);
        await this.loginUserWithCaseId(credentials.judge, false, caseId);
        await this.homePage.navigateToTab('Audio/Video Evidence');

        await this.avTab.verifyPageContentByKeyValue('Document Type', 'audioDocument');
        await this.avTab.verifyPageContentByKeyValue('Audio/video document url', uploadResponseTestdata.testaudiofile);
        await this.avTab.verifyPageContentByKeyValue('Audio/video party uploaded', 'CTSC clerk');
        await this.avTab.verifyPageContentByKeyValue('Original Sender', 'Appellant');
        await this.avTab.verifydueDates('Date added');

        await this.homePage.chooseEvent('Process audio/video evidence');
        await this.processAVPage.selectRequestedEvidence('Test file');
        await this.processAVPage.verifyPageContent('CTSC clerk');
        await this.processAVPage.grantApprovalEvidence();
        await this.processAVPage.continueOnPreviewDoc();

        await this.homePage.navigateToTab('Documents');
        await this.documentsTab.verifyPageContentByKeyValue("Type", "Audio/Video evidence direction notice");
        await this.documentsTab.verifyPageContentByKeyValue("Bundle addition", "A");
        await this.documentsTab.verifydueDates('Date added');
        await this.documentsTab.verifyPageContentByKeyValue("Type", "Audio document");
        await this.documentsTab.verifyPageContentByKeyValue("Audio/video document", "test_av.mp3");
        await this.documentsTab.verifyPageContentByKeyValue("Party uploaded", "ctsc");
        await this.documentsTab.verifyPageContentByKeyValue("Original Sender", "Appellant");
        await this.documentsTab.verifydueDates('Date approved');

        await this.homePage.navigateToTab('History');
        await this.verifyHistoryTabDetails('Process audio/video evidence');
    }

    async excludeEvidenceUploadedByCTSC(caseId: string) {
        
        await this.homePage.delay(3000);
        await this.loginUserWithCaseId(credentials.judge, false, caseId);
        await this.homePage.navigateToTab('Audio/Video Evidence');

        await this.avTab.verifyPageContentByKeyValue('Document Type', 'audioDocument');
        await this.avTab.verifyPageContentByKeyValue('Audio/video document url', uploadResponseTestdata.testaudiofile);
        await this.avTab.verifyPageContentByKeyValue('Audio/video party uploaded', 'CTSC clerk');
        await this.avTab.verifyPageContentByKeyValue('Original Sender', 'Appellant');
        await this.avTab.verifydueDates('Date added');

        await this.homePage.chooseEvent('Process audio/video evidence');
        await this.processAVPage.selectRequestedEvidence('Test file');
        await this.processAVPage.verifyPageContent('CTSC clerk');
        await this.processAVPage.rejectApprovalEvidence();
        await this.processAVPage.continueOnPreviewDoc();

        await this.homePage.navigateToTab('Documents');
        await this.documentsTab.verifyPageContentByKeyValue("Type", "Audio/Video evidence direction notice");
        await this.documentsTab.verifyPageContentByKeyValue("Bundle addition", "A");
        await this.documentsTab.verifydueDates('Date added');
        await this.documentsTab.verifyPageContentNotPresentByKeyValue("Type", "Audio document");
        await this.documentsTab.verifyPageContentNotPresentByKeyValue("Audio/video document", "test_av.mp3");
        await this.documentsTab.verifyPageContentNotPresentByKeyValue("Party uploaded", "ctsc");
        await this.documentsTab.verifyPageContentNotPresentByKeyValue("Original Sender", "Appellant");

        await this.homePage.navigateToTab('History');
        await this.verifyHistoryTabDetails('Process audio/video evidence');
    }

    async completeProcessAudioVideoEvidenceEvent() {

        await this.processAVPage.selectRequestedEvidence('Test file');
        await this.processAVPage.verifyPageContent(avEvidenceTestdata.ftaValue);
        await this.processAVPage.grantApprovalEvidence();
        await this.processAVPage.continueOnPreviewDoc();

        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);
        await this.homePage.navigateToTab(avEvidenceTestdata.docTab);
        await this.documentsTab.verifyPageContentByKeyValue(avEvidenceTestdata.docTypeField, avEvidenceTestdata.docTypeValue);
        await this.documentsTab.verifyPageContentByKeyValue("Bundle addition", "A");
        await this.documentsTab.verifydueDates('Date added');

        await this.homePage.navigateToTab('History');
        await this.verifyHistoryTabDetails('Process audio/video evidence');
    }

    async verifyTcwWithoutCaseAllocatorRoleCanViewProcessAudioVideoEvidenceTask(caseId: string) {

        // Verify TCW can view the unassigned Process Audio/Video Evidence task
        // await this.performUploadDocumentFurtherEvidence(caseId);
        await this.loginUserWithCaseId(credentials.amTribunalCaseWorker,true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);
    }

    async verifyTcwWithCaseAllocatorRoleCanViewAndAssignProcessAudioVideoEvidenceTask(caseId: string) {

        /* Login as Senior TCW with case allocator role and view the 
           unassigned Process Audio/Video Evidence task and assign it to another TCW */
        await this.loginUserWithCaseId(credentials.amSeniorTribunalCaseWorkerWithCaseAllocatorRole, true, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptionsForCaseAllocator);
        await this.tasksTab.assignTaskToTcwUser(task.name, credentials.amTribunalCaseWorker.email);
    }

    async verifyTcwCanMarkProcessAudioVideoEvidenceTaskCanViewAndCompleteTheTaskMarkAsDone(caseId: string) {

        // Login as TCW and view the unassigned Process Audio/Video Evidence task and Mark as done
        await this.loginUserWithCaseId(credentials.amTribunalCaseWorker, true, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedTo);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptions);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);
        await this.tasksTab.markTheTaskAsDone(task.name);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifyTcwAsAnAssignedUserForProcessAudioVideoEvidenceTaskCanViewAndCompleteTheTask(caseId: string) {

        // Login as TCW and view the unassigned Process Audi Video Evidence task
        await this.loginUserWithCaseId(credentials.amTribunalCaseWorker, true, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedTo);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptions);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Select Process audio/video evidence next step and complete the event
        await this.tasksTab.clickNextStepLink(task.processAudioVideoEvidence.link);
        await this.completeProcessAudioVideoEvidenceEvent();

        // Verify task is removed from the tasks list within Tasks tab
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifyProcessAudioVideoEvidenceTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId: string) {

        // Verify TCW with case allocator role can view the unassigned Process Audio/Video Evidence task
        // await this.performUploadDocumentFurtherEvidence(caseId);
        await this.loginUserWithCaseId(credentials.amSeniorTribunalCaseWorkerWithCaseAllocatorRole, true, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptionsForCaseAllocator);

        // TCW with case allocator role assigns task to another TCW user
        await this.tasksTab.assignTaskToTcwUser(task.name, credentials.amTribunalCaseWorker.email);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedTo);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForCaseAllocator);

        // TCW voids the case
        let voidCase = new VoidCase(this.page);
        await voidCase.performVoidCase(caseId, false);

        // Verify task is removed from the tasks list within Tasks tab
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }
}
