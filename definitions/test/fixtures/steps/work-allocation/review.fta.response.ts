import { Page } from '@playwright/test';
import { BaseStep } from '../base';
import { credentials } from '../../../config/config';
import { StepsHelper } from '../../../helpers/stepsHelper';
import task from '../../../pages/content/review.fta.response.task_en.json';

const uploadResponseTestdata = require('../../../pages/content/upload.response_en.json');

export class ReviewFTAResponse extends BaseStep {
  readonly page: Page;
  protected stepsHelper: StepsHelper;

  constructor(page: Page) {
    super(page);
    this.page = page;
    this.stepsHelper = new StepsHelper(page);
  }

  async dwpUploadsFTAResponse(caseId: string) {
    await this.loginUserWithCaseId(credentials.dwpResponseWriter, true, caseId);
    await this.stepsHelper.uploadResponseHelper(
      uploadResponseTestdata.pipIssueCode,
      'Yes'
    );
    await this.checkYourAnswersPage.confirmSubmission();
    await this.signOut();
  }

  async verifyCtscAdminWithoutCaseAllocatorRoleCanViewReviewFTAResponseTask(caseId: string) {
    // Verify CTSC Admin can view the unassigned Review FTA Response task
    // await this.dwpUploadsFTAResponse(caseId);
    await this.loginUserWithCaseId(credentials.amCtscAdminNwLiverpool, true, caseId);
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.verifyPriortiy(task.name, task.priority);
    await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
    await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);
    await this.signOut();
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
}
