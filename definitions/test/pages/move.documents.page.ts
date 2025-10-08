import { Page, expect } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webActions: WebAction;

export class MoveDocumentsPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async verifyMoveDocumentsPage(): Promise<void> {
    await webActions.verifyPageLabel('h1.govuk-heading-l', 'Manage documents');
  }

  async verifyMoveToInternalDocumentsChoices(filename: string): Promise<void> {
    let locator = this.page
      .locator('#moveDocumentToInternalDocumentsTabDL div.multiple-choice')
      .filter({ hasText: filename });
    await expect(locator).toBeVisible();
  }

  async selectDocumentToMove(filename: string): Promise<void> {
    let locator = this.page
      .locator('#moveDocumentToInternalDocumentsTabDL div.multiple-choice')
      .filter({ hasText: filename });
    await locator.locator('input').first().check();
  }

  async verifyIssuedQuestion() {
    await webActions.verifyPageLabel(
      '#shouldBeIssued span',
      'Should this document be issued out to parties?'
    );
    await webActions.verifyPageLabel('label[for="shouldBeIssued_Yes"]', 'Yes');
    await webActions.verifyPageLabel('label[for="shouldBeIssued_No"]', 'No');
  }

  async verifyMoveToDocumentsChoices(filename: string): Promise<void> {
    let locator = this.page
      .locator('#moveDocumentToDocumentsTabDL div.multiple-choice')
      .filter({ hasText: filename });
    await expect(locator).toBeVisible();
  }

  async selectInternalDocumentToMove(filename: string): Promise<void> {
    let locator = this.page
      .locator('#moveDocumentToDocumentsTabDL div.multiple-choice')
      .filter({ hasText: filename });
    await locator.locator('input').first().check();
  }

  async verifyNoDocumentsError(): Promise<void> {
    await webActions.clickButton('Submit');
    await webActions.verifyPageLabel(
      'div.error-summary ul li',
      'Please select at least one document to move'
    );
  }

  async verifyDocumentIssuedError(): Promise<void> {
    await webActions.clickButton('Submit');
    await webActions.verifyPageLabel(
      'a.validation-error',
      'Should this document be issued out to parties? is required'
    );
  }

  async confirmSubmission(): Promise<void> {
    const pageUrl = this.page.url();
    await webActions.clickButton('Submit');
    await expect(this.page).not.toHaveURL(pageUrl);
  }

  async submit(): Promise<void> {
    await webActions.clickButton('Submit');
  }

  async selectNotIssued(): Promise<void> {
    await webActions.clickElementById('#shouldBeIssued_No');
  }

  async selectIssued(): Promise<void> {
    await webActions.clickElementById('#shouldBeIssued_Yes');
  }
}
