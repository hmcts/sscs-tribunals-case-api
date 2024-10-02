import { Page, expect } from '@playwright/test';
import task from '../../pages/content/action.unprocessed.correspondence.task_en.json';
import { BaseStep } from './base';
import { credentials } from '../../config/config';
import eventTestData from '../../pages/content/event.name.event.description_en.json';
import supplementaryResponseData from '../../pages/content/supplementary.response_en.json';
import actionFurtherEvidenceTestdata from '../../pages/content/action.further.evidence_en.json';


export class SupplementaryResponse extends BaseStep {

    readonly page : Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performSupplementaryResponse(caseId: string) {

        // Dwp user performs Supplementary response event
        await this.loginUserWithCaseId(credentials.dwpResponseWriter, false, caseId);
        await this.homePage.reloadPage();
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.chooseEvent('Supplementary response');

        await this.supplementaryResponsePage.verifyPageContent();
        await this.supplementaryResponsePage.uploadSupplementaryResponseDoc(supplementaryResponseData.testfileone);
        await this.supplementaryResponsePage.selectFtaState(supplementaryResponseData.ftaState);
        await this.supplementaryResponsePage.chooseFtaRecommendPoToAttend(supplementaryResponseData.poAttendOption);
        await this.supplementaryResponsePage.continueSubmission();

        await this.supplementaryResponsePage.verifyCYAPageContent();
        await this.supplementaryResponsePage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();
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
        await this.tasksTab.verifyTaskIsDisplayed(task.name);

        // CTSC Admin verifies assigned task details
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedTo);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptions);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);
    }

    async verifyCtscAdminAsAllocatedCaseWorkerCanCompleteTheAssignedActionUnprocessedCorrespondenceTask(caseId: string) {

        // Verify CTSC Admin as allocated caseworker can see the assigned Action Unprocessed Correspondence task
        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(task.name);

        // CTSC Admin verifies assigned task details
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedTo);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptions);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Select Action Further Evidence next step and complete the event
        await this.tasksTab.clickNextStepLink(task.actionFurtherEvidence.link);

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

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifyActionUnprocessedCorrespondenceTaskCanBeCancelledManuallyByAllocatedCtscAdmin(caseId: string) {

        // Verify CTSC Admin as allocated caseworker can view the automatically assigned Action Unprocessed Correspondence task
        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptions);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // CTSC Admin cancels the task manually
        await this.tasksTab.cancelTask(task.name);

        // Verify task is removed from the tasks list within Tasks tab
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }
}