import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import confidentialityConfirmedContent from './content/confidentiality.confirmed_en.json';

let webActions: WebAction;

export class ConfidentialityConfirmed {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async verifyAndSubmitConfidentialityConfirmedEvent() {
    await webActions.verifyPageLabel(
      '.govuk-heading-l',
      confidentialityConfirmedContent.eventName
    );
    await webActions.verifyPageLabel(
      '#confidentialityConfirmedLabel p',
      confidentialityConfirmedContent.eventNameLabel
    );
    await webActions.clickSubmitButton();
    await webActions.waitForSpinnerToDisappear();
  }
}
