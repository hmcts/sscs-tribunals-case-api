import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webActions: WebAction;

export class AmendElementPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async updateElementAndIssueCode(): Promise<void> {
    await webActions.verifyPageLabel(
      'h1.govuk-heading-l',
      'Elements disputed'
    );
    await webActions.unCheckAnCheckBox('#elementsDisputedList-childElement');
    await webActions.checkAnCheckBox('Childcare');
    await webActions.clickButton('Continue');

    await webActions.verifyTextVisibility('Childcare');
    await this.page.getByRole('button', { name: 'Add new' }).click();
    await webActions.chooseOptionByLabel(
        '#elementsDisputedChildCare_0_issueCode',
        'GC'
    );
    await webActions.clickButton('Continue');
    await webActions.clickSubmitButton();
  }
}