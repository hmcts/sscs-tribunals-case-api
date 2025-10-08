import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webActions: WebAction;

export class UploadRemoveOrMoveDocumentPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async verifyUploadRemoveOrMovePage(): Promise<void> {
    await webActions.verifyPageLabel(
      'h1.govuk-heading-l',
      'Upload/remove or move a document'
    );
    await webActions.verifyPageLabel('div.multiple-choice', [
      'Upload/remove document',
      'Move document'
    ]);
  }

  async selectUploadRemoveDocument(): Promise<void> {
    await webActions.clickElementById(
      '#uploadRemoveOrMoveDocument-uploadRemove'
    );
    await webActions.clickButton('Continue');
  }

  async selectMoveDocument(): Promise<void> {
    await webActions.clickElementById('#uploadRemoveOrMoveDocument-move');
    await webActions.clickButton('Continue');
  }
}
