import { Page, expect } from '@playwright/test';
import aucTask from '../../pages/content/action.unprocessed.correspondence.task_en.json';
import rbdTask from '../../pages/content/review.bilingual.document.task_en.json';
import { BaseStep } from './base';
import { credentials } from '../../config/config';
import eventTestData from '../../pages/content/event.name.event.description_en.json';
import { CancelTranslations } from './cancel.translations';
import { RequestTranslations } from './request.translations';


const actionFurtherEvidenceTestdata = require('../../pages/content/action.further.evidence_en.json');
const uploadDocumentFurtherEvidenceData = require('../../pages/content/upload.document.further.evidence_en.json');


export class UploadDocumentFurtherEvidence extends BaseStep {

    readonly page : Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performUploadDocumentFurtherEvidence(caseId: string, uploadAudioFile ?: boolean) {

        await this.loginUserWithCaseId(credentials.amCaseWorkerWithCaseAllocatorRole, false, caseId);
        await this.homePage.reloadPage();
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.chooseEvent('Upload document FE');

        await this.uploadDocumentFurtherEvidencePage.verifyPageContent();
        await this.uploadDocumentFurtherEvidencePage.clickAddNew();
        await this.uploadDocumentFurtherEvidencePage.selectDocumenType(uploadDocumentFurtherEvidenceData.documentType);
        await this.uploadDocumentFurtherEvidencePage.inputFilename(uploadDocumentFurtherEvidenceData.fileName);

        if(uploadAudioFile) {
            await this.uploadDocumentFurtherEvidencePage.uploadFurtherEvidenceDoc(uploadDocumentFurtherEvidenceData.testaudiofile);
        } else {
            await this.uploadDocumentFurtherEvidencePage.uploadFurtherEvidenceDoc(uploadDocumentFurtherEvidenceData.testfileone);
        }
        
        await this.uploadDocumentFurtherEvidencePage.confirmSubmission();

        await this.eventNameAndDescriptionPage.verifyPageContent(uploadDocumentFurtherEvidenceData.eventName);
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.clickSignOut();
    }

    async allocateCaseToCtscUser(caseId: string) {
        // CTSC Admin with case allocator role allocates case to another CTSC Admin
        await this.loginUserWithCaseId(credentials.amCaseWorkerWithCaseAllocatorRole, false, caseId);
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);
        await this.homePage.navigateToTab('Roles and access');
        await this.rolesAndAccessTab.allocateCtscRole(credentials.amCaseWorker.email);
    }

    async verifyCtscAdminAsAllocatedCaseWorkerCanViewTheAutomaticallyAssignedActionUnprocessedCorrespondenceTask(caseId: string) {

        // Action Unprocessed Correspondence task is automatically assigned to allocated CTSC Admin
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(aucTask.name);
    }

    async verifyCtscAdminAsAllocatedCaseWorkerCanViewTheAutomaticallyAssignedReviewBilingualDocumentTask(caseId: string) {

        // Review Bi-Lingual Document - CTSC task is automatically assigned to allocated CTSC Admin
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(rbdTask.name);
    }

    async verifyCtscAdminAsAllocatedCaseWorkerCanCompleteTheAssignedActionUnprocessedCorrespondenceTask(caseId: string) {

        // Verify CTSC Admin as allocated caseworker can see the assigned Action Unprocessed Correspondence task
        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(aucTask.name);

        // CTSC Admin verifies assigned task details
        await this.tasksTab.verifyPriortiy(aucTask.name, aucTask.priority);
        await this.tasksTab.verifyPageContentByKeyValue(aucTask.name, 'Assigned to', aucTask.assignedTo);
        await this.tasksTab.verifyManageOptions(aucTask.name, aucTask.assignedManageOptions);
        await this.tasksTab.verifyNextStepsOptions(aucTask.name, aucTask.nextStepsOptions);

        // Select Action Further Evidence next step and complete the event
        await this.tasksTab.clickNextStepLink(aucTask.actionFurtherEvidence.link);

        await this.actionFurtherEvidencePage.selectFEOption();
        await this.actionFurtherEvidencePage.selectSenderOption(actionFurtherEvidenceTestdata.ftaSender);
        await this.actionFurtherEvidencePage.selectbundle();
        await this.actionFurtherEvidencePage.confirmSubmission();

        await this.eventNameAndDescriptionPage.verifyPageContent(actionFurtherEvidenceTestdata.eventName);
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        await this.tasksTab.verifyTaskIsHidden(aucTask.name);
    }

    async verifyCtscAdminAsAllocatedCaseWorkerCanCompleteTheAssignedReviewBilingualDocumentTask(caseId: string) {

        // Verify CTSC Admin as allocated caseworker can see the assigned Review Bi-Lingual Document - CTSC task
        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(rbdTask.name);

        // CTSC Admin verifies assigned task details
        await this.tasksTab.verifyPriortiy(rbdTask.name, rbdTask.priority);
        await this.tasksTab.verifyPageContentByKeyValue(rbdTask.name, 'Assigned to', rbdTask.assignedTo);
        await this.tasksTab.verifyManageOptions(rbdTask.name, rbdTask.assignedManageOptions);
        await this.tasksTab.verifyNextStepsOptions(rbdTask.name, rbdTask.nextStepsOptions);

        // Select Request translation from WLU next step and complete the event
        await this.tasksTab.clickNextStepLink(rbdTask.requestTranslationFromWlu.link);

        let requestTranslations = new RequestTranslations(this.page);
        await requestTranslations.completeRequestTranslations();

        // CTSC Admin verifies the task is removed from the tasks list
        await this.tasksTab.verifyTaskIsHidden(rbdTask.name);
    }

    async verifyUnassignedActionUnprocessedCorrespondenceTaskCanBeCancelledManuallyByCtscAdmin(caseId: string) {

        // Verify CTSC Admin can view the unassigned Action Unprocessed Correspondence task
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(aucTask.name);
        await this.tasksTab.verifyPageContentByKeyValue(aucTask.name, 'Assigned to', aucTask.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(aucTask.name, aucTask.unassignedManageOptions);

        // CTSC Admin cancels the task manually
        await this.tasksTab.cancelTask(aucTask.name);

        // Verify task is removed from the tasks list within Tasks tab
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(aucTask.name);
    }

    async verifyCtscAdminCanViewBilingualDocumentTaskAndCancelWelshTranslations(caseId: string) {

        // Verify CTSC Admin can view the unassigned Review Bi-Lingual Document task
        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(rbdTask.name);
        await this.tasksTab.verifyPageContentByKeyValue(rbdTask.name, 'Assigned to', rbdTask.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(rbdTask.name, rbdTask.unassignedManageOptions);

        // CTSC Admin cancels translations
        let cancelTranslations = new CancelTranslations(this.page)
        await cancelTranslations.performCancelTranslations(caseId, false);
    }

    async verifyUnassignedReviewBilingualDocumentTaskIsRemovedFromTheTasksList(caseId: string) {

        // CTSC Admin verifies task is removed from the tasks list within Tasks tab
        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(rbdTask.name);
    }
}