import { expect, Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from "../../config/config";
import task from '../../pages/content/review.fta.time.extension.request.task_en.json';
import { VoidCase } from './void.case';
import { IssueDirectionsNotice } from './issue.directions.notice';
import issueDirectionTestdata from '../../pages/content/issue.direction_en.json';

const reqTimeExtData = require("../../pages/content/request.time.extension_en.json");


export class RequestTimeExtension extends BaseStep {

    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performAndVerifyRequestTimeExtension(caseId: string) {

        await this.loginUserWithCaseId(credentials.dwpResponseWriter,false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent(reqTimeExtData.eventNameCaptor);

        await this.requestTimeExtensionPage.verifyPageContent();
        await this.requestTimeExtensionPage.uploadTimeExtensionDoc();
        await this.requestTimeExtensionPage.confirmSubmission();
 
        await this.loginUserWithCaseId(credentials.amCaseWorker,true, caseId);
        await this.homePage.navigateToTab('History');
        await this.historyTab.verifyHistoryPageEventLink(reqTimeExtData.eventName);

        await this.homePage.navigateToTab('Appeal Details');
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue(reqTimeExtData.stateField, reqTimeExtData.stateValue);
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue(reqTimeExtData.reasonField, reqTimeExtData.reasonValue);
    }

    async verifyTcwWithoutCaseAllocatorRoleCanViewReviewRequestTimeExtensionTask(caseId: string) {

        // Verify TCW can view the unassigned Review FTA Time Extension Request task
        await this.performAndVerifyRequestTimeExtension(caseId);
        await this.loginUserWithCaseId(credentials.amTribunalCaseWorker,true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);
    }

    async verifyTcwWithCaseAllocatorRoleCanViewAndAssignRequestTimeExtensionTask(caseId: string) {

        /* Login as Senior TCW with case allocator role and view the 
           unassigned Review FTA Time Extension Request task and assign it to another TCW */
        await this.loginUserWithCaseId(credentials.amSeniorTribunalCaseWorkerWithCaseAllocatorRole, false, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptionsForCaseAllocator);
        await this.tasksTab.assignTaskToTcwUser(task.name, credentials.amTribunalCaseWorker.email);
    }

    async verifyTcwAsAnAssignedUserForReviewFtaTimeExtensionRequestTaskCanViewAndCompleteTheTask(caseId: string) {

        // Login as TCW and view the unassigned Review FTA Time Extension Request task
        await this.loginUserWithCaseId(credentials.amTribunalCaseWorker, false, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedTo);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptions);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Select issue direction next step and complete the event
        await this.tasksTab.clickNextStepLink(task.issueDirectionsNotice.link);
        await this.issueDirectionPage.submitIssueDirection(
            issueDirectionTestdata.preHearingType,
            issueDirectionTestdata.allowTimeExtensionDirectionType, 
            issueDirectionTestdata.docTitle
        );
        await this.eventNameAndDescriptionPage.verifyPageContent(issueDirectionTestdata.eventNameCaptor);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        await this.verifyHistoryTabDetails('With FTA', 'Issue directions notice');

        // Verify task is removed from the tasks list within Tasks tab
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifyRequestTimeExtensionTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId: string) {

        // Verify TCW with case allocator role can view the unassigned Review FTA Time Extension Request task
        await this.loginUserWithCaseId(credentials.amSeniorTribunalCaseWorkerWithCaseAllocatorRole, false, caseId);
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
