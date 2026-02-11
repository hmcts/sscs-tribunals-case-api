import { expect, Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webActions: WebAction;

export class ActionFurtherEvidencePage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async verifyPageContent() {
    await webActions.verifyPageLabel(
      'h1.govuk-heading-l',
      'Handle Further Evidence'
    );
  }

  async selectFEOption() {
    await webActions.chooseOptionByIndex('#furtherEvidenceAction', 2);
  }

  async selectReviewByJudge() {
    await webActions.chooseOptionByIndex('#furtherEvidenceAction', 5);
  }

  async selectIssueToAllParties() {
    await webActions.chooseOptionByIndex('#furtherEvidenceAction', 1);
  }

  async selectSenderOption(optionVal: string) {
    await webActions.chooseOptionByLabel('#originalSender', optionVal);
  }

  async selectDocType(optionVal: string) {
    await webActions.chooseOptionByLabel('#scannedDocuments_0_type', optionVal);
  }

  async uploadDocs(fileName: string): Promise<void> {
    await webActions.uploadFileUsingAFileChooser(
      '#scannedDocuments_0_url',
      fileName
    );
    await this.page.waitForTimeout(7000);
  }

  async uploadEditedDocs(fileName: string): Promise<void> {
    await webActions.uploadFileUsingAFileChooser(
      '#scannedDocuments_0_editedUrl',
      fileName
    );
    await this.page.waitForTimeout(7000);
  }

  async enterFileName() {
    await webActions.inputField('#scannedDocuments_0_fileName', 'testfile');
  }

  async enterScannedDate() {
    await webActions.inputField('#scannedDate-day', '21');
    await webActions.inputField('#scannedDate-month', '1');
    await webActions.inputField('#scannedDate-year', '2021');
    await webActions.inputField('#scannedDate-second', '00');
    await this.page.locator('#scannedDate').first().click();
    await expect(
      this.page.locator('#scannedDate span.error-message')
    ).toBeHidden();
  }

  async selectbundle() {
    const numberofDocs = await this.page.locator("//span[normalize-space()='Include in bundle? (Optional)']").count();
    for (let i = 0; i < numberofDocs; i++) {
      await webActions.clickElementById(
        `label[for='scannedDocuments_${i}_includeInBundle_Yes']`
      );
    }
  }

  async confirmSubmission(): Promise<void> {
    await webActions.clickButton('Submit');
  }

  async clickAddNewButton(): Promise<void> {
    await webActions.clickButton('Add new');
  }

  async submitActionFurtherEvidence(
    senderOption: string,
    docType: string,
    fileName: string
  ): Promise<void> {
    await this.verifyPageContent();
    await this.selectFEOption();
    await this.selectSenderOption(senderOption);
    await this.selectDocType(docType);
    await this.uploadDocs(fileName);
    await this.enterFileName();
    await this.enterScannedDate();
    await this.selectbundle();
    await this.confirmSubmission();
  }

  async confirmAndSignOut(): Promise<void> {
    const pageUrl = this.page.url();
    await this.confirmSubmission();
    await expect(this.page).not.toHaveURL(pageUrl);
    await webActions.clickElementById('li a.hmcts-header__navigation-link');
    await expect(this.page.locator('#username')).toBeVisible();
  }

  async submitActionFurtherEvidenceForConfRequest(
    senderOption: string,
    docType: string,
    fileName: string
  ): Promise<void> {
    await this.verifyPageContent();
    await this.selectReviewByJudge();
    await this.selectSenderOption(senderOption);

    await this.clickAddNewButton();
    await this.selectDocType(docType);
    await this.uploadDocs(fileName);
    await this.enterFileName();
    await this.enterScannedDate();
    await this.selectbundle();
    await this.confirmSubmission();
  }

  async submitActionFurtherEvidenceForRequest(
    senderOption: string,
    fileName: string
  ): Promise<void> {
    await this.verifyPageContent();
    await this.selectIssueToAllParties();
    await this.selectSenderOption(senderOption);

    // await this.selectDocType(docType);
    await this.uploadEditedDocs(fileName);
    // await this.enterFileName();
    // await this.enterScannedDate();
    // await this.selectbundle();
    await this.confirmSubmission();
  }

  async verifyEncryptedFileErrorMsg(): Promise<void> {
    await webActions.verifyElementVisibility('#error-summary-title');
    await webActions.verifyElementVisibility('span.error-message')
    await webActions.verifyTextVisibility('Your upload file is password protected.');
  }

  async verifyCorruptedFileErrorMsg(): Promise<void> {
    await webActions.verifyElementVisibility('#errors');
    await webActions.verifyTextVisibility(
      'The below PDF document(s) are not readable, please correct this'
    );
  }
}
