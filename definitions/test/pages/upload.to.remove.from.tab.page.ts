import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webActions: WebAction;

export class UploadToRemoveFromTabPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async verifyUploadRemovePage(): Promise<void> {
    await webActions.verifyPageLabel(
      'h1.govuk-heading-l',
      'Upload/remove document from which tab'
    );
    await webActions.verifyPageLabel('div.multiple-choice', [
      'Upload to/remove from Documents tab',
      'Upload to/remove from Tribunal internal documents tab'
    ]);
  }

  async selectDocumentsTab(): Promise<void> {
    await webActions.clickElementById('#uploadRemoveDocumentType-document');
    await webActions.clickButton('Continue');
  }

  async selectInternalDocumentsTab(): Promise<void> {
    await webActions.clickElementById(
      '#uploadRemoveDocumentType-internalDocument'
    );
    await webActions.clickButton('Continue');
  }
}
