import { BaseStep } from './base';
import { expect, Page } from '@playwright/test';
import { StepsHelper } from '../../helpers/stepsHelper';
import { credentials } from '../../config/config';
import IssueDirection from '../../pages/content/issue.direction_en.json';

export class UcConfidentiality extends BaseStep {
  readonly page: Page;
  protected stepsHelper: StepsHelper;

  constructor(page: Page) {
    super(page);
    this.page = page;
    this.stepsHelper = new StepsHelper(this.page);
  }

  async addOtherPartyToUcCase(caseId: string) {
    await this.loginUserWithCaseId(credentials.dwpResponseWriter, true, caseId);
    //it is a known bug that it is visible for the user before other party added, so not asserting on it here
    //await expect(this.homePage.summaryTab).not.toBeVisible();
    await this.homePage.chooseEvent('Add other party data');
    await this.addOtherPartyDataPage.verifyPageContent();
    await this.addOtherPartyDataPage.addOtherPartyData();
    await this.summaryTab.waitForSummaryState(
      'Await Confidentiality Requirements'
    );
    await this.homePage.navigateToTab('Confidentiality');
    await this.confidentialityTab.verifyConfidentialityRows([
      {
        party: 'Appellant',
        name: 'Joe Bloggs',
        status: 'Undetermined',
        confirmed: 'blank'
      },
      {
        party: 'Other Party 1',
        name: 'Test User',
        status: 'Undetermined',
        confirmed: 'blank'
      }
    ]);
  }

  async issueHef(caseId: string) {
    await this.loginUserWithCaseId(credentials.caseWorker, true, caseId);
    await this.homePage.chooseEvent('Issue hearing enquiry form');
    await this.issueHefPage.submitCompleteIssueHefEvent();
    await this.summaryTab.waitForSummaryState(
      'Await Confidentiality Requirements'
    );
    await this.homePage.navigateToTab('History');
    await this.historyTab.verifyHistoryPageEventLink(
      'Issue hearing enquiry form'
    );
    await this.historyTab.verifyPageContentByKeyValue(
      'Event',
      'Issue hearing enquiry form'
    );
    await this.historyTab.verifyPageContentByKeyValue(
      'End state',
      'Await Confidentiality Requirements'
    );
    await this.historyTab.verifyInterlocReviewState('HEF Issued');
  }

  async setConfidentialityForAppellant(isConfidentialityRequired: boolean) {
    await this.homePage.chooseEvent('Update to case data');
    await this.createUpdateToCaseDataPage.setConfidentialityAppellant(
      isConfidentialityRequired
    );
    await this.homePage.navigateToTab('History');
    await this.historyTab.verifyHistoryPageEventLink('Update to case data');
    await this.historyTab.verifyPageContentByKeyValue(
      'Event',
      'Update to case data'
    );
  }

  async setConfidentialityForOtherParty(isConfidentialityRequired: boolean) {
    await this.homePage.chooseEvent('Update other party data');
    await this.updateOtherPartyDataPage.setConfidentialityForOtherParty(
      isConfidentialityRequired
    );
    await this.homePage.navigateToTab('History');
    await this.historyTab.verifyHistoryPageEventLink('Update other party data');
    await this.historyTab.verifyPageContentByKeyValue(
      'Event',
      'Update other party data'
    );
  }

  async confirmConfidentialityGrantedSetSuccessfully() {
    await this.homePage.navigateToTab('Confidentiality');
    await this.confidentialityTab.verifyConfidentialityRows([
      {
        party: 'Appellant',
        name: 'Joe Bloggs',
        status: 'Yes',
        confirmed: 'today'
      },
      {
        party: 'Other Party 1',
        name: 'Test User',
        status: 'Yes',
        confirmed: 'today'
      }
    ]);
    await this.confidentialityTab.verifyConfidentialityFlagVisibility(true);
  }

  async submitConfidentialityConfirmedEvent(caseId: string) {
    await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId)
    await this.homePage.chooseEvent('Confidentiality Confirmed');
    await this.confidentialityConfirmedPage.verifyAndSubmitConfidentialityConfirmedEvent();
    await this.summaryTab.waitForSummaryState('With FTA');
    await this.homePage.navigateToTab('History');
    await this.historyTab.verifyHistoryPageEventLink(
      'Confidentiality Confirmed'
    );
    await this.historyTab.verifyPageContentByKeyValue(
      'Event',
      'Confidentiality Confirmed'
    );
    await this.historyTab.verifyPageContentByKeyValue('End state', 'With FTA');
  }

  async refuseAppellantConfidentialityViaIssueDirectionNotice(caseId: string) {
    await this.loginUserWithCaseId(credentials.judge, true, caseId);
    await this.homePage.chooseEvent('Issue directions notice');
    await this.issueDirectionPage.verifyPageContent();
    await this.issueDirectionPage.submitIssueDirection(
      IssueDirection.preHearingType,
      IssueDirection.confidentialityRefused,
      IssueDirection.docTitle
    );
    await this.eventNameAndDescriptionPage.verifyPageContent(
      IssueDirection.eventNameCaptor
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await this.homePage.navigateToTab('History');
    await this.historyTab.verifyHistoryPageEventLink('Issue directions notice');
    await this.historyTab.verifyPageContentByKeyValue(
      'Event',
      'Issue directions notice'
    );
  }

  async confirmConfidentialityRefusedSetSuccessfully() {
    await this.homePage.navigateToTab('Confidentiality');
    await this.confidentialityTab.verifyConfidentialityRows([
      {
        party: 'Appellant',
        name: 'Joe Bloggs',
        status: 'No',
        confirmed: 'today'
      },
      {
        party: 'Other Party 1',
        name: 'Test User',
        status: 'No',
        confirmed: 'today'
      }
    ]);
    await this.confidentialityTab.verifyConfidentialityFlagVisibility(false);
  }

}
