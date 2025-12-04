import { Page, expect } from '@playwright/test';
import { BaseStep } from '../base';
import { credentials } from '../../../config/config';
import { StepsHelper } from '../../../helpers/stepsHelper';
import task from '../../../pages/content/review.fta.response.task_en.json';
import { VoidCase } from '../void.case';
import responseReviewedTestData from '../../../pages/content/response.reviewed_en.json';
import uploadResponseTestdata from '../../../pages/content/upload.response_en.json';

export class CtscReviewFtaResponse extends BaseStep {
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
  
async verifyCtscAdminWithCaseAllocatorRoleCanViewReviewFTAResponseTask(caseId: string) {
    await this.loginUserWithCaseId(
      credentials.amCaseWorkerWithCaseAllocatorRole,
      true,
      caseId
    );
    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPresenceOfText('Response received');
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.verifyPriortiy(task.name, task.priority);
    await this.tasksTab.verifyPageContentByKeyValue(
      task.name,
      'Assigned to',
      task.assignedToWhenNotAssigned
    );
    await this.tasksTab.verifyManageOptions(
      task.name,
      task.unassignedManageOptionsForCaseAllocator
    );
  }

  async verifyCtscAdminWithoutCaseAllocatorRoleCanCompleteReviewFTAResponseTask(
    caseId: string
  ) {
    // Login as CTSC Administrator and view the unassigned Review FTA Response task
    await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.verifyPriortiy(task.name, task.priority);
    await this.tasksTab.verifyPageContentByKeyValue(
      task.name,
      'Assigned to',
      task.assignedToWhenNotAssigned
    );

    // CTSC Administrator self assigns task and verifies assigned task details
    await this.tasksTab.selfAssignTask(task.name);
    await this.tasksTab.verifyPageContentByKeyValue(
      task.name,
      'Assigned to',
      task.assignedTo
    );
    await this.tasksTab.verifyManageOptions(
      task.name,
      task.assignedManageOptions
    );
    await this.tasksTab.verifyNextStepsOptions(
      task.name,
      task.nextStepsOptions
    );

    // Select Response reviewed next step and complete the event
    await this.tasksTab.clickNextStepLink(task.responseReviewed.link);
    await this.responseReviewedPage.verifyPageContent(
      responseReviewedTestData.captionValue,
      responseReviewedTestData.headingValue
    );
    await this.responseReviewedPage.chooseInterlocOption('No');
    await this.responseReviewedPage.confirmSubmission();

    await expect(this.homePage.summaryTab).toBeVisible();

    // Verify task is removed from the tasks list within Tasks tab
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsHidden(task.name);
  }

  async verifyReviewFTAResponseTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId: string) {
    // Verify CTSC Admin can view the unassigned Review FTA Response task
    await this.homePage.signOut();
    await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.verifyManageOptions(
      task.name,
      task.unassignedManageOptions
    );

    // CTSC Administrator self assigns task and verifies assigned task details
    await this.tasksTab.selfAssignTask(task.name);
    await this.tasksTab.verifyPageContentByKeyValue(
      task.name,
      'Assigned to',
      task.assignedTo
    );
    await this.tasksTab.verifyManageOptions(
      task.name,
      task.assignedManageOptions
    );
    await this.tasksTab.verifyNextStepsOptions(
      task.name,
      task.nextStepsOptions
    );

    // CTSC Administrator voids the case
    let voidCase = new VoidCase(this.page);
    await voidCase.performVoidCase(caseId, false);

    // Verify task is removed from the tasks list within Tasks tab
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsHidden(task.name);
  }
}
