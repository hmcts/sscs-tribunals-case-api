import { Page, expect } from '@playwright/test';
import { BaseStep } from '../base';
import { credentials } from '../../../config/config';
import { ListingError } from '../listing.error';
import { ReadyToList } from '../ready.to.list';
import { VoidCase } from '../void.case';
import task from '../../../pages/content/review.listing.error.task_en.json';


export class CtscReviewListingError extends BaseStep {
  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
  }

   async verifyCtscAdminWithoutCaseAllocatorRoleCanViewReviewListingErrorTask(caseId: string) {

        let readyToList = new ReadyToList(this.page);
        let listingError = new ListingError(this.page);

        // Login as CTSC Admin and complete Ready to list event
        await readyToList.performReadyToListEvent(caseId);

        // Login as CTSC Admin and complete Listing error event to initiate Review Listing Error task
        await listingError.performListingErrorEvent(caseId, false);

        // Verify CTSC Admin can view the unassigned Review Listing Error task
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);
    }

    async verifyCtscAdminWithCaseAllocatorRoleCanViewReviewListingErrorTask(caseId: string) {

        /* Login as CTSC Administrator with case allocator role and view the 
           unassigned Review Listing Error task */
        await this.loginUserWithCaseId(credentials.amCtscTeamLeaderNwLiverpool, true, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptionsForCaseAllocator);
    }

    async verifyCtscAdminWithoutCaseAllocatorRoleCanCompleteReviewListingErrorTask(caseId: string) {

        // Login as CTSC Administrator and view the unassigned Review Listing Error task
        await this.loginUserWithCaseId(credentials.amCtscAdminNwLiverpool, true, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);

        // CTSC Administrator self assigns task and verifies assigned task details

        await this.tasksTab.selfAssignTask(task.name);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedTo);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptions);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Select update listing requirements next step and complete the event
        await this.tasksTab.clickNextStepLink(task.updateListingRequirements.link);
        await this.listingRequirementPage.submitEventNoChangeNoHearingRequested();
        await expect(this.homePage.summaryTab).toBeVisible();
    
        // Verify task is removed from the tasks list within Tasks tab
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifyReviewListingErrorTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId: string) {

        let readyToList = new ReadyToList(this.page);
        let listingError = new ListingError(this.page);
        let voidCase = new VoidCase(this.page);

        // Login as CTSC Admin and complete Ready to list event
        await readyToList.performReadyToListEvent(caseId);

        // As a CTSC Admin complete Listing error event to initiate Review Listing Error task
        await listingError.performListingErrorEvent(caseId, false);

        // Verify CTSC Admin can view the unassigned Review Listing Error task
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);

        // CTSC Administrator self assigns task and verifies assigned task details
        await this.tasksTab.selfAssignTask(task.name);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedTo);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptions);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // CTSC Administrator voids the case
        await voidCase.performVoidCase(caseId, false);

        // Verify task is removed from the tasks list within Tasks tab
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }
  
}
