import { expect, Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from "../../config/config";
import task from '../../pages/content/referred.by.admin.task_en.json';
import { VoidCase } from './void.case';
import issueDirectionTestdata from '../../pages/content/issue.direction_en.json';

const reqTimeExtData = require("../../pages/content/request.time.extension_en.json");


export class ReferredByAdmin extends BaseStep {

    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async allocateCaseToLegalOpsRole(caseId: string) {
        
        // CTSC Admin with case allocator role allocates case to Tcw
        await this.loginUserWithCaseId(credentials.amSuperUser, false, caseId);
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);
        await this.homePage.navigateToTab('Roles and access');
        await this.rolesAndAccessTab.allocateLegalOpsRole(credentials.amTribunalCaseWorker.email);
    }

    async verifyTcwWithoutCaseAllocatorRoleCanViewReviewReferredByAdminTask(caseId: string) {

        // Verify TCW can view the unassigned Referred by Admin task
        await this.loginUserWithCaseId(credentials.amTribunalCaseWorker,true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedTo);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptions);
    }

    async verifyTcwWithCaseAllocatorRoleCanViewAndAssignReferredByAdminTask(caseId: string) {

        /* Login as Senior TCW with case allocator role and view the 
           unassigned Referred by Admin task and assign it to another TCW */
        await this.loginUserWithCaseId(credentials.amSeniorTribunalCaseWorkerWithCaseAllocatorRole, false, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptionsForCaseAllocator);
        await this.tasksTab.assignTaskToTcwUser(task.name, credentials.amTribunalCaseWorker.email);
    }

    async verifyTcwAsAnAssignedUserForReferredByAdminTaskCanViewAndCompleteTheTask(caseId: string) {

        // Login as TCW and view the unassigned Referred by Admin task
        await this.loginUserWithCaseId(credentials.amTribunalCaseWorker, true, caseId);
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

    async verifyReferredByAdminTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId: string) {

        // Verify TCW with case allocator role can view the unassigned Referred by Admin task
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
