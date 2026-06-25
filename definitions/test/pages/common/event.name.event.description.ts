import { expect, Page } from '@playwright/test';
import { WebAction } from '../../common/web.action';

const eventTestData = require('../content/event.name.event.description_en.json');

let webActions: WebAction;

export class EventNameEventDescriptionPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async verifyPageContent(
    headingValue: string,
    checkYourAnswersFlag: boolean = false,
    key?: string,
    value?: string
  ) {
    await webActions.verifyPageLabel('.govuk-heading-l', headingValue); //Heading Text
    if (checkYourAnswersFlag) {
      await webActions.verifyPageLabel(
        '.heading-h2',
        eventTestData.eventSummarycheckYourAnswersHeading
      ); //Check your answers Text.
      await webActions.verifyPageLabel(
        '.check-your-answers h2.heading-h2 + span',
        eventTestData.eventSummaryCheckTheInformationText
      );
      await webActions.verifyPageLabel('.case-field-label > .text-16', key);
      if (typeof value === 'undefined') {
      } else {
        await webActions.verifyPageLabel(
          'ccd-read-text-area-field > span',
          value
        );
      }
    }
    await webActions.verifyPageLabel(
      "[for='field-trigger-summary']",
      eventTestData.eventSummaryLabel
    ); //Field Label
    //await webActions.verifyPageLabel('.form-hint', eventTestData["event-summary-guidance-text"]); //Guidance Text
    await webActions.verifyPageLabel(
      "[for='field-trigger-description']",
      eventTestData.eventSummaryDescription
    ); //Field Label
  }

  async verifyCyaPageContent(
    headingValue: string,
    keys: string[],
    values: string[]
  ) {
    await webActions.verifyPageLabel('.govuk-heading-l', headingValue); //Heading Text
    await webActions.verifyPageLabel(
      '.heading-h2',
      eventTestData.eventSummarycheckYourAnswersHeading
    );
    await webActions.verifyPageLabel(
      '.check-your-answers h2.heading-h2 + span',
      eventTestData.eventSummaryCheckTheInformationText
    );
    await webActions.verifyPageLabel('.case-field-label > .text-16', keys);
    await webActions.verifyPageLabel(
      '.case-field-content span.text-16',
      values
    );
    await webActions.verifyPageLabel(
      "[for='field-trigger-summary']",
      eventTestData.eventSummaryLabel
    );
    await webActions.verifyPageLabel(
      "[for='field-trigger-description']",
      eventTestData.eventSummaryDescription
    );
  }

  async inputData(
    eventSummary: string,
    eventDescription: string
  ): Promise<void> {
    await webActions.inputField('#field-trigger-summary', eventSummary);
    await webActions.inputField('#field-trigger-description', eventDescription);
  }

  async confirmSubmission(): Promise<void> {
    const pageUrl = this.page.url();
    await webActions.clickSubmitButton();
    try {
      await expect(this.page).not.toHaveURL(pageUrl);
    } catch {
      await webActions.clickSubmitButton();
      await expect(this.page).not.toHaveURL(pageUrl);
    }
  }

  async confirmWithoutNavigation(): Promise<void> {
    await webActions.clickSubmitButton();
  }

  async confirmAndSignOut(): Promise<void> {
    let pageUrl = this.page.url();
    await this.confirmSubmission();
    await expect(this.page).not.toHaveURL(pageUrl);
    pageUrl = this.page.url();
    await webActions.clickElementById('li a.hmcts-header__navigation-link');
    await expect(this.page).not.toHaveURL(pageUrl);
  }

  async submitBtn(): Promise<void> {
    await webActions.clickButton('Submit');
  }

  async delay(ms: number) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}
