import { Page } from '@playwright/test';
import { BaseStep } from '../base';
import { credentials } from '../../../config/config';
import task from '../../../pages/content/review.incomplete.appeal.task_en.json'


export class ReviewIncompleteApplication extends BaseStep {
  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
  }
  async assignTaskToSelf(caseId: string) {
    await this.loginUserWithCaseId(credentials.amCtscTeamLeader, false, caseId);
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.selfAssignTask(task.name);
    await this.tasksTab.verifyTaskIsAssigned(task.name);
    await this.homePage.navigateToMyWork();
    await this.myWorkPage.verifyTaskAssignedToMe();
  }

  async completeTask(caseId: string) {
    await this.loginPage.goToCase(caseId)
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.markTheTaskAsDone(task.name);
    await this.homePage.navigateToMyWork();
    await this.myWorkPage.verifyNoAssignedTasks();
  }

}
