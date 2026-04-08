import { WebAction } from '../common/web.action';
import { Page } from '@playwright/test';
import updateNotListableData from './content/update.not.listable_en.json';
import dateUtilsComponent from '../utils/DateUtilsComponent';

let webActions: WebAction;

export class UpdateNotListablePage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }
  async verifyPageContent() {
    await webActions.verifyPageLabel(
      '.govuk-caption-l',
      updateNotListableData.updateNotListableEventCaption
    ); //Caption Text
    await webActions.verifyPageLabel(
      '.govuk-heading-l',
      updateNotListableData.updateNotListableEventHeading
    ); //Heading Text
    await webActions.verifyTextVisibility(
      'Have the requirements in the direction been fulfilled?'
    );
  }

  async requirementsFulfilled() {
    await this.page.locator('#updateNotListableDirectionsFulfilled_Yes').waitFor({ state: 'visible' });
    await this.page.click('#updateNotListableDirectionsFulfilled_Yes');
    await this.page.getByText('Continue').click();
  }

  async requirementsNotFulfilled() {
    await this.page.locator('#updateNotListableDirectionsFulfilled_No').waitFor({ state: 'visible' });
    await this.page.click('#updateNotListableDirectionsFulfilled_No');
    await this.page.getByText('Continue').click();
  }

  async interlocutoryReviewStateNotRequired() {
    await this.page.locator('#updateNotListableInterlocReview_No').waitFor({ state: 'visible' });
    await this.page.click('#updateNotListableInterlocReview_No');
    await this.page.getByText('Continue').click();
  }

  async interlocutoryReviewRequiredTCW() {
    await this.page.locator('#updateNotListableInterlocReview_Yes').waitFor({ state: 'visible' });
    await this.page.click('#updateNotListableInterlocReview_Yes');
    await webActions.chooseOptionByLabel(
      '#updateNotListableWhoReviewsCase',
      'A TCW'
    );
    await this.page.getByText('Continue').click();
  }

  async interlocutoryReviewRequiredJudge() {
    await this.page.locator('#updateNotListableInterlocReview_Yes').waitFor({ state: 'visible' });
    await this.page.click('#updateNotListableInterlocReview_Yes');
    await webActions.chooseOptionByLabel(
      '#updateNotListableWhoReviewsCase',
      'A Judge'
    );
    await this.page.getByText('Continue').click();
  }

  async noNewDueDateRequired() {
    await this.page.locator('#updateNotListableSetNewDueDate_No').waitFor({ state: 'visible' });
    await this.page.click('#updateNotListableSetNewDueDate_No');
    await this.page.getByText('Continue').click();
  }

  async newDueDateRequired() {

    const nextYear =  await dateUtilsComponent.getNextYear();

    await this.page.locator('#updateNotListableSetNewDueDate_Yes').waitFor({ state: 'visible' });
    await this.page.click('#updateNotListableSetNewDueDate_Yes');
    await this.page.locator('#updateNotListableDueDate-day').waitFor({ state: 'visible' });
    await webActions.inputField('#updateNotListableDueDate-day', '13');
    await webActions.inputField('#updateNotListableDueDate-month', '01');
    await webActions.inputField('#updateNotListableDueDate-year', String(nextYear));
    await this.page.click('#updateNotListableSetNewDueDate_Yes');
    await this.page.getByText('Continue').click();
  }

  async noNewDueDateMoveCaseToWithFTA() {
    await this.page.locator('#updateNotListableSetNewDueDate_No').waitFor({ state: 'visible' });
    await this.page.click('#updateNotListableSetNewDueDate_No');
    await this.page.getByText('Continue').click();
    await this.page.locator('#updateNotListableWhereShouldCaseMoveTo-withDwp').waitFor({ state: 'visible' });
    await this.page.click('#updateNotListableWhereShouldCaseMoveTo-withDwp');
    await this.page.getByText('Continue').click();
  }

  async moveCaseToReadyToList() {
    await this.page.locator('#updateNotListableWhereShouldCaseMoveTo-readyToList').waitFor({ state: 'visible' });
    await this.page.click(
      '#updateNotListableWhereShouldCaseMoveTo-readyToList'
    );
    await this.page.getByText('Continue').click();
  }

  async confirmSubmission(): Promise<void> {
    await this.page.getByText('Submit').waitFor({ state: 'visible' });
    await this.page.getByText('Submit').click();
  }

  async continueEvent(): Promise<void> {
    await this.page.getByText('Continue').waitFor({ state: 'visible' });
    await this.page.getByText('Continue').click();
  }
}
