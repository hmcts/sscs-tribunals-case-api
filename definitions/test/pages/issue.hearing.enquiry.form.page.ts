import { expect, Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import issueHef from '../pages/content/issue.hearing.enquiry.form.json';
import eventTestData from '../pages/content/event.name.event.description_en.json';

let webActions: WebAction;

export class IssueHearingEnquiryFormPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async verifyAndCompleteAddOtherPartyPage() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', issueHef.EventName);
    await webActions.verifyPageLabel(
      '#otherPartySelection_0_0 span',
      issueHef.SelectPartyDropdownLabel
    );
    await webActions.verifyElementVisibility(
      '#otherPartySelection_0_otherPartiesList'
    );

    await webActions.chooseOptionByIndex(
      '#otherPartySelection_0_otherPartiesList',
      1
    );
    await webActions.clickButton('Continue');
    await webActions.waitForSpinnerToDisappear();
  }

  async verifyAndCompleteAddAnyDocsPage() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', issueHef.EventName);
    await webActions.verifyPageLabel(
      '#addDocuments span',
      issueHef.AddDocumentsRadioLabel
    );
    await webActions.verifyElementVisibility('#addDocuments_Yes');
    await webActions.verifyElementVisibility('#addDocuments_No');
    await webActions.verifyPageLabel(
      'label[for=addDocuments_Yes]',
      issueHef.AddDocumentsYesLabel
    );
    await webActions.verifyPageLabel(
      'label[for=addDocuments_No]',
      issueHef.AddDocumentsNoLabel
    );

    await webActions.clickElementById('#addDocuments_Yes');
    await webActions.verifyPageLabel(
      'label[for=documentSelection_0_documentsList] span',
      issueHef.SelectDocumentDropdownLabel
    );
    await webActions.chooseOptionByIndex(
      '#documentSelection_0_documentsList',
      1
    );
    await webActions.clickSubmitButton();
    await webActions.waitForSpinnerToDisappear();
  }

  async verifyIssueHefCyaPageContent(
    otherPartyName: string,
    addedDocs: boolean
  ) {
    await webActions.verifyPageLabel('.govuk-heading-l', issueHef.EventName); //Heading Text
    await webActions.verifyPageLabel(
      'form.check-your-answers h2.heading-h2',
      eventTestData.eventSummarycheckYourAnswersHeading
    );

    await webActions.verifyPageLabel(
      "//span[text()='Other party']/ancestor::tr//ccd-read-dynamic-list-field/span",
      otherPartyName
    );

    const docsAdded = addedDocs ? 'Yes' : 'No';
    await webActions.verifyPageLabel(
      "//span[text()='Do you want to add any documents?']//ancestor::tr//ccd-read-yes-no-field/span",
      docsAdded
    );

    if (addedDocs)
      await webActions.verifyElementVisibility(
        "//span[text()='Document']/ancestor::tr//ccd-read-dynamic-list-field/span"
      );
  }

  async submitCompleteIssueHefEvent(){
    await this.verifyAndCompleteAddOtherPartyPage();
    await this.verifyAndCompleteAddAnyDocsPage();
    await this.verifyIssueHefCyaPageContent(
      'Other party 1 - Test User',
      true
    );
    await webActions.clickSubmitButton();
    await webActions.waitForSpinnerToDisappear();
  }
}
