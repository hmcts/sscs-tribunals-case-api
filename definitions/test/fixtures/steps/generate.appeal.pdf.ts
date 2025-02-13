import { BaseStep } from "./base";
import { Page } from "@playwright/test";


export class GenerateAppealPdfSteps extends BaseStep {

  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  async generateAppealPdfEvent() {
    await this.homePage.chooseEvent("Generate appeal PDF");
    await this.eventNameAndDescriptionPage.inputData('test reason', 'test description');
    await this.eventNameAndDescriptionPage.confirmSubmission();
  }

  async verifyDocumentsTab(filename) {
    await this.homePage.navigateToTab("Documents");
    await this.documentsTab.verifyPageContentByKeyValue('Type', 'SSCS8');
    await this.documentsTab.verifyPageContentByKeyValue('Original document URL', filename);
    await this.documentsTab.verifydueDates('Date added');
    await this.documentsTab.verifyPageContentByKeyValue('File name', filename);
  }
}
