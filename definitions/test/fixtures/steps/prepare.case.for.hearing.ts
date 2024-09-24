import { expect, Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from "../../config/config";
import prepareCaseForHearingData from '../../pages/content/prepare.case.for.hearing_en.json';
import task from '../../pages/content/allocate.case.roles.and.create.bundle.task_en.json'

export class PrepareCaseForHearing extends BaseStep {

    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async prepareCaseForHearing(caseId: string, signOutRequired: boolean = false): Promise<void>{

        if(signOutRequired) {
            await this.homePage.signOut();
        }
        await this.loginUserWithCaseId(credentials.amSuperUser, false, caseId);
        await this.homePage.chooseEvent(prepareCaseForHearingData.eventName);
        await this.prepareCaseForHearingPage.verifyPageContent();
        await this.prepareCaseForHearingPage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();

        await this.homePage.delay(3000);
        await this.homePage.navigateToTab("History");
        await this.verifyHistoryTabDetails(prepareCaseForHearingData.eventName);
    }

    async allocateCaseToRegionalCenterAdmin(caseId: string) {

        // CTSC Admin with case allocator role allocates case to Regional Center Admin
        await this.homePage.signOut();
        await this.loginUserWithCaseId(credentials.amCaseWorkerWithCaseAllocatorRole, false, caseId);
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);
        await this.homePage.navigateToTab('Roles and access');
        await this.rolesAndAccessTab.allocateAdminRole(credentials.amRegionalCenterAdmin.email);
    }

    async verifyRegionalCenterAdminCanViewTheAllocateCaseRolesAndCreateBundleTask(caseId: string) {

        // Regional Center Admin views the Allocate Case Roles and Create Bundle - RPC task
        await this.loginUserWithCaseId(credentials.amRegionalCenterAdmin, false, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptionsForRegionalCenterAdmin);

         // Regional Center Admin self assigns the task
         await this.tasksTab.selfAssignTask(task.name)
         await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToRegionalCenterAdmin);
         await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForRegionalCenterAdmin);
         await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);
    }

    async verifyRegionalCenterAdminCanViewAndCompleteTheAllocateCaseRolesAndCreateBundleTask(caseId: string) {
        
        await this.verifyRegionalCenterAdminCanViewTheAllocateCaseRolesAndCreateBundleTask(caseId);        

        // Regional Center Admin completes the Create bundle next step event
        await this.tasksTab.clickNextStepLink(task.createBundle.link);
        await this.completeCreateBundle();

        // Regional Center Admin verifies that the task is removed from the Tasks tab
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifyRegionalCenterTeamLeaderCanViewAndCompleteTheAllocateCaseRolesAndCreateBundleTask(caseId: string) {
        
        // Regional Center Team Leader views the Allocate Case Roles and Create Bundle - RPC task
        await this.loginUserWithCaseId(credentials.amRegionalCenterTeamLeader, false, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptionsForRegionalCenterTeamLeader);

        // Regional Center Team Leader self assigns the task
        await this.tasksTab.selfAssignTask(task.name)
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToRegionalCenterTeamLeader);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForRegionalCenterTeamLeader);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Regional Center Team Leader completes the Create bundle next step event
        await this.tasksTab.clickNextStepLink(task.createBundle.link);
        await this.completeCreateBundle();

        // Regional Center Team Leader verifies that the task is removed from the Tasks tab
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifyAllocateCaseRolesAndCreateBundleIsCancelledAutomaticallyWhenTheCaseIsStruckOut(caseId: string) {

        // Regional Center Admin views the task
        await this.loginUserWithCaseId(credentials.amRegionalCenterAdmin, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToRegionalCenterAdmin);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForRegionalCenterAdmin);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // CTSC Admin strikes out the case
        await this.homePage.chooseEvent('Strike out case');
        await this.eventNameAndDescriptionPage.verifyPageContent("Strike out case");
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();

        await this.homePage.delay(3000);
        await this.homePage.navigateToTab("History");
        await this.verifyHistoryTabDetails('Dormant', 'Strike out case');

        // Verify task is removed from the tasks list within Tasks tab
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async completeCreateBundle() {
        await this.createBundlePage.verifyPageContent();
        await this.createBundlePage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);
        await this.homePage.navigateToTab("History");
        await this.verifyHistoryTabDetails("Create bundle");
    }
}
