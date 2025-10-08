import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webActions: WebAction;

export class MoveToTabPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async verifyMovePage(): Promise<void> {
    await webActions.verifyPageLabel(
      'h1.govuk-heading-l',
      'Move document to which tab'
    );
    await webActions.verifyPageLabel('div.multiple-choice', [
      'Move document to Tribunal internal documents tab',
      'Move document to Documents tab'
    ]);
  }

  async selectDocumentsTab(): Promise<void> {
    await webActions.clickElementById('#moveDocumentTo-document');
    await webActions.clickButton('Continue');
  }

  async selectInternalDocumentsTab(): Promise<void> {
    await webActions.clickElementById('#moveDocumentTo-internalDocument');
    await webActions.clickButton('Continue');
  }

  async verifyErrorNoDocuments(): Promise<void> {
    await webActions.verifyPageLabel(
      'div.error-summary ul li',
      'No documents available to move'
    );
  }

  async verifyErrorNoInternalDocuments(): Promise<void> {
    await webActions.verifyPageLabel(
      'div.error-summary ul li',
      'No Tribunal Internal documents available to move'
    );
  }
}
