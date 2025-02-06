import { Page, expect } from '@playwright/test';
import { WebAction } from '../common/web.action';
import supplementaryResponseData from './content/supplementary.response_en.json';

let webActions: WebAction;

export class SupplementaryResponsePage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async verifyPageContent() {
    await webActions.verifyPageLabel(
      'h1.govuk-heading-l',
      supplementaryResponseData.pageHeading
    ); //Page heading
    await webActions.verifyPageLabel(
      'div#dwpSupplementaryResponseDoc_dwpSupplementaryResponseDoc h2.heading-h2',
      supplementaryResponseData.supplementaryResponseSectionHeading
    ); //Section heading
    await webActions.verifyPageLabel(
      'div#dwpOtherDoc_dwpOtherDoc h2.heading-h2',
      supplementaryResponseData.otherDocumentsSectionHeading
    ); //Section heading
  }

  async uploadSupplementaryResponseDoc(fileName: string): Promise<void> {
    await webActions.uploadFileUsingAFileChooser(
      '#dwpSupplementaryResponseDoc_documentLink',
      fileName
    );
  }

  async uploadOtherDoc(fileName: string): Promise<void> {
    await webActions.uploadFileUsingAFileChooser(
      '#dwpOtherDoc_documentLink',
      fileName
    );
  }

  async selectFtaState(issueCode: string): Promise<void> {
    await webActions.chooseOptionByLabel('#dwpState', issueCode);
  }

  async chooseFtaRecommendPoToAttend(optionVal: string): Promise<void> {
    await webActions.clickElementById(`#dwpIsOfficerAttending_${optionVal}`);
  }

  async continueSubmission(): Promise<void> {
    await webActions.clickButton('Continue');
  }

  async verifyCYAPageContent(): Promise<void> {
    await webActions.verifyPageLabel(
      '.govuk-heading-l',
      supplementaryResponseData.pageHeading
    ); //Heading Text
    await webActions.verifyPageLabel(
      'form.check-your-answers h2.heading-h2',
      supplementaryResponseData.cyaHeading
    ); //Check your answers Text.
  }

  async confirmSubmission(): Promise<void> {
    await webActions.clickButton('Submit');
  }

  async delay(ms: number) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}
