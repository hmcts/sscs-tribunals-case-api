import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webActions: WebAction;

export class MyWorkPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async verifyTaskAssignedToMe(
    caseName = 'Test Appellant',
    caseCategory = 'Personal Independence Payment',
    location = 'CARDIFF',
    task = 'Review Incomplete Appeal - CTSC',
    priority = 'low') {
    const expectedText = [caseName, caseCategory, location, task, priority];
    for (const text of expectedText) {
      await webActions.verifyTextVisibility(text);
    }
  }

  async verifyNoAssignedTasks() {
    await webActions.verifyTextVisibility('You have no assigned tasks.')
  }
}

