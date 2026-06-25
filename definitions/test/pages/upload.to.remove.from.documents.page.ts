import { Page, expect } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webActions: WebAction;

export class UploadToRemoveFromDocumentsPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async verifyUploadRemoveDocumentsPage(): Promise<void> {
    await webActions.verifyPageLabel('h1.govuk-heading-l', 'Manage documents');
  }

  async addNewDocument(type: string, filename: string): Promise<void> {
    await webActions.clickButton('Add new');
    let typeLocator = this.page.getByLabel('Type (Optional)').locator('visible=true').last();
    try {
      await expect(typeLocator).toBeVisible({ timeout: 10_000 });
    } catch {
      await expect(typeLocator).toBeVisible({ timeout: 10_000 });
    }
    await typeLocator.selectOption({ label: type });
    let typeId = await this.page
      .getByLabel('Type (Optional)')
      .locator('visible=true')
      .last()
      .getAttribute('id');
    let uploadId = typeId.replace('documentType', 'documentLink');
    await webActions.uploadFileUsingAFileChooser(`#${uploadId}`, filename);
  }

  async confirmSubmission(): Promise<void> {
    const pageUrl = this.page.url();
    await webActions.clickButton('Submit');
    await expect(this.page).not.toHaveURL(pageUrl);
  }

  async removeDocument(tab: string, filename: string): Promise<void> {
    let fileNamesLocator = await this.page
      .locator('ccd-read-document-field > button')
      .all();
    let fileNames = await Promise.all(
      fileNamesLocator.map(async (file) => await file.textContent())
    );
    let fileNameIndex = fileNames.findIndex((file) => file.includes(filename));
    let removeButton = this.page
      .getByLabel('Remove Case documents')
      .nth(fileNameIndex);
    if (tab === 'Tribunal Internal Documents') {
      removeButton = this.page
        .getByLabel('Remove Tribunal Internal documents')
        .nth(fileNameIndex);
    }
    let removePopUp = this.page.getByRole('button', {
      name: 'Remove',
      exact: true
    });
    await expect(removeButton).toBeVisible();
    await expect(removeButton).toBeEnabled();
    await removeButton.click();
    await expect(removePopUp).toBeVisible();
    await removePopUp.first().click();
    await expect(removePopUp).toBeHidden();
    expect(await this.page.locator('ccd-read-document-field > button').count()).toBe(
      fileNames.length - 1
    );
  }
}
