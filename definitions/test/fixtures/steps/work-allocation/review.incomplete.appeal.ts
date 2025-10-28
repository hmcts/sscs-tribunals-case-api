import { Page } from '@playwright/test';
import { BaseStep } from '../base';
import { credentials } from '../../../config/config';
import task from '../../../pages/content/review.incomplete.appeal.task_en.json';
import communicateWithFta from '../../../pages/content/fta.communication_en.json';
import { VoidCase } from '../void.case';


export class ReviewIncompleteAppeal extends BaseStep {
  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  async verifyCtscAdminWithoutCaseAllocatorRoleCanViewReviewIncompleteAppealTask(caseId: string) {
    // Verify CTSC Admin can view the unassigned Review Incomplete Appeal task
    await this.loginUserWithCaseId(credentials.amCtscAdminNwLiverpool, true, caseId);
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.verifyPriortiy(task.name, task.priority);
    await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
    await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);
  }

  async assignTaskToSelf(caseId: string) {
    await this.loginUserWithCaseId(credentials.amCtscTeamLeaderNwLiverpool, false, caseId);
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.selfAssignTask(task.name);
    await this.tasksTab.verifyTaskIsAssigned(task.name);
    await this.homePage.navigateToMyWork();
    await this.myWorkPage.verifyTaskAssignedToMe();
  }

  async completeReviewIncompleteAppealTask(caseId: string) {
    await this.loginPage.goToCase(caseId)
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.clickNextStepLink(task.communicateWithFta.eventTitle);
    await this.communicateWithFtaPage.selectCommunicationType('New Request');
    await this.communicateWithFtaPage.fillOutNewRequestData('MRN/Review Decision Notice Details', 'FTA')
    await this.homePage.navigateToTab('Tasks');
    await this.homePage.navigateToMyWork();
    await this.myWorkPage.verifyNoAssignedTasks();
  }

  async verifyCtscAdminWithCaseAllocatorRoleCanViewAndAssignReviewIncompleteAppealTask(caseId: string) {
    /* Login as CTSC Administrator with case allocator role and view the 
       unassigned Review Incomplete Appeal task and assign it to another CTSC Admin */
    await this.loginUserWithCaseId(credentials.amCtscTeamLeaderNwLiverpool, true, caseId);
    await this.homePage.navigateToTab('Tasks')
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.verifyPriortiy(task.name, task.priority);
    await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
    await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptionsForCaseAllocator);
    await this.tasksTab.assignTaskToCtscUser(task.name, credentials.amCtscAdminNwLiverpool.email);
    await this.signOut();
    await this.loginUserWithCaseId(credentials.amCtscAdminNwLiverpool, false, caseId);
    await this.homePage.navigateToMyWork();
    await this.myWorkPage.verifyTaskAssignedToMe();

    //unassign task to not clutter the user's My Work page
    await this.myWorkPage.unassignTask(task.name);
  }

  async verifyReviewIncompleteAppealTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId: string) {

    let voidCase = new VoidCase(this.page);

    // Verify CTSC Admin with case allocator role can view the unassigned Review Incomplete Appeal task
    await this.loginUserWithCaseId(credentials.amCtscTeamLeaderNwLiverpool, false, caseId);
    await this.homePage.navigateToTab('Tasks')
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptionsForCaseAllocator);

    // CTSC Administrator with case allocator role assigns task to another CTSC user
    await this.tasksTab.assignTaskToCtscUser(task.name, credentials.amCtscAdminNwLiverpool.email);
    await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedTo);
    await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForCaseAllocator);

    // CTSC Administrator voids the case
    await voidCase.performVoidCase(caseId, false);


    // Verify task is removed from the tasks list within Tasks tab
    await this.homePage.navigateToTab('Tasks');
    await this.homePage.delay(100000);
    await this.tasksTab.verifyTaskIsHidden(task.name);
  }
}
