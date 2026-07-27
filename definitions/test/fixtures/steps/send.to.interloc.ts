import { Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from '../../config/config';
import sendToInterlocData from '../../pages/content/send.to.interloc_en.json';
import eventTestData from '../../pages/content/event.name.event.description_en.json';

export class SendToInterloc extends BaseStep {
  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  async signOut() {
    await this.homePage.signOut();
  }

  async performSendToInterloc(caseId: string) {
    await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
    await this.homePage.chooseEvent('Send to interloc');

    await this.sendToInterlocPage.verifyPageContent();
    await this.sendToInterlocPage.selectHearingType(
      sendToInterlocData.sendToInterlocHearingSelectValue
    );
    await this.sendToInterlocPage.selectCaseReview(
      sendToInterlocData.sendToInterlocCaseReviewSelectValue
    );
    await this.sendToInterlocPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocReasonReferredValue
    );
    await this.sendToInterlocPage.confirmSubmission();

    await this.eventNameAndDescriptionPage.verifyPageContent(
      'Send to interloc'
    );
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();

    await this.verifyHistoryTabDetails(
      'With FTA',
      'Send to interloc',
      eventTestData.eventDescriptionInput
    );
  }

  async performSendToInterlocReferralReasonOver300Pages(caseId: string) {
    await this.loginUserWithCaseId(
      credentials.amTribunalCaseWorker,
      false,
      caseId
    );
    await this.homePage.chooseEvent('Send to interloc');

    await this.sendToInterlocPage.verifyPageContent();
    await this.sendToInterlocPage.selectHearingType(
      sendToInterlocData.sendToInterlocHearingSelectValue
    );
    await this.sendToInterlocPage.selectCaseReview(
      sendToInterlocData.sendToInterlocCaseReviewSelectValue
    );
    await this.sendToInterlocPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocReferralReasonOver300Pages
    );
    await this.sendToInterlocPage.confirmSubmission();

    await this.eventNameAndDescriptionPage.verifyPageContent(
      'Send to interloc'
    );
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();

    await this.verifyHistoryTabDetails(
      'With FTA',
      'Send to interloc',
      eventTestData.eventDescriptionInput
    );
  }

  async performSendToInterlocReferralReasonComplexCase(caseId: string) {
    await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
    await this.homePage.chooseEvent('Send to interloc');

    await this.sendToInterlocPage.verifyPageContent();
    await this.sendToInterlocPage.selectHearingType(
      sendToInterlocData.sendToInterlocHearingSelectValue
    );
    await this.sendToInterlocPage.selectCaseReview(
      sendToInterlocData.sendToInterlocCaseReviewSelectValueJudge
    );
    await this.sendToInterlocPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocReferralReasonComplexCase
    );
    await this.sendToInterlocPage.confirmSubmission();

    await this.eventNameAndDescriptionPage.verifyPageContent(
      'Send to interloc'
    );
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();

    await this.verifyHistoryTabDetails(
      'With FTA',
      'Send to interloc',
      eventTestData.eventDescriptionInput
    );
  }

  async performSendToInterlocJudge(caseId: string) {
    await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
    await this.homePage.chooseEvent('Send to interloc');

    await this.sendToInterlocPage.verifyPageContent();
    await this.sendToInterlocPage.selectHearingType(
      sendToInterlocData.sendToInterlocHearingSelectValue
    );
    await this.sendToInterlocPage.selectCaseReview(
      sendToInterlocData.sendToInterlocCaseReviewSelectValueJudge
    );
    await this.sendToInterlocPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocReasonReferredValue
    );
    await this.sendToInterlocPage.confirmSubmission();

    await this.eventNameAndDescriptionPage.verifyPageContent(
      'Send to interloc'
    );
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();

    await this.verifyHistoryTabDetails(
      'With FTA',
      'Send to interloc',
      eventTestData.eventDescriptionInput
    );
  }

  async verifyConfidentialityReferralReasons(caseId: string, user) {
    await this.loginUserWithCaseId(user, false, caseId);
    await this.homePage.chooseEvent('Send to interloc');

    await this.sendToInterlocPage.verifyPageContent();
    await this.sendToInterlocPage.selectHearingType(
      sendToInterlocData.sendToInterlocHearingSelectValue
    );
    await this.sendToInterlocPage.selectCaseReview(
      sendToInterlocData.sendToInterlocCaseReviewSelectValue
    );
    await this.sendToInterlocPage.verifyReasonReferredOptions([
      sendToInterlocData.sendToInterlocConfidentialityReasonValue,
      sendToInterlocData.sendToInterlocReviewConfidentialityRequestValue
    ]);
    await this.sendToInterlocPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocConfidentialityReasonValue
    );
    await this.sendToInterlocPage.verifySelectedReasonReferred(
      sendToInterlocData.sendToInterlocConfidentialityReasonValue
    );
  }

  async submitConfidentialityReferralAndVerifySummary(caseId: string, user) {
    await this.loginUserWithCaseId(user, false, caseId);
    await this.homePage.chooseEvent('Send to interloc');

    await this.sendToInterlocPage.verifyPageContent();
    await this.sendToInterlocPage.selectHearingType(
      sendToInterlocData.sendToInterlocHearingSelectValue
    );
    await this.sendToInterlocPage.selectCaseReview(
      sendToInterlocData.sendToInterlocCaseReviewSelectValueJudge
    );
    await this.sendToInterlocPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocConfidentialityReasonValue
    );

    await this.sendToInterlocPage.selectSelectedParty(1);

    await this.sendToInterlocPage.confirmSubmission();

    await this.eventNameAndDescriptionPage.verifyPageContent(
      'Send to interloc'
    );
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPageSectionByKeyValue(
      sendToInterlocData.sendToInterlocReasonReferredFieldLabel,
      sendToInterlocData.sendToInterlocConfidentialityReasonValue
    );
    await this.summaryTab.verifyPageContentByKeyValue(
      sendToInterlocData.sendToInterLocSelectPartyFieldLabel,
      sendToInterlocData.sendToInterlocPartyConfidentialityValue_2
    );

    await this.homePage.navigateToTab('History');
    await this.historyTab.verifyHistoryPageEventLink('Send to interloc');
    await this.historyTab.verifyPageContentByKeyValue(
      'Event',
      'Send to interloc'
    );
    await this.historyTab.verifyInterlocReviewState('Review by Judge');
  }

  async submitConfidentialityReferralChildSupportAndVerifySummary(
    caseId: string,
    user
  ) {
    await this.loginUserWithCaseId(user, false, caseId);
    await this.homePage.chooseEvent('Send to interloc');

    await this.sendToInterlocPage.verifyPageContent();
    await this.sendToInterlocPage.selectHearingType(
      sendToInterlocData.sendToInterlocHearingSelectValue
    );
    await this.sendToInterlocPage.selectCaseReview(
      sendToInterlocData.sendToInterlocCaseReviewSelectValue
    );
    await this.sendToInterlocPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocConfidentialityReasonValue
    );
    await this.sendToInterlocPage.selectPartyConfidentiality(
      sendToInterlocData.sendToInterlocPartyConfidentialityValue
    );
    await this.sendToInterlocPage.confirmSubmission();

    await this.eventNameAndDescriptionPage.verifyPageContent(
      'Send to interloc'
    );
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();

    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPageSectionByKeyValue(
      sendToInterlocData.sendToInterlocReasonReferredFieldLabel,
      sendToInterlocData.sendToInterlocConfidentialityReasonValue
    );
    await this.summaryTab.verifyPageContentByKeyValue(
      sendToInterlocData.sendToInterLocSelectPartyFieldLabel,
      sendToInterlocData.sendToInterlocPartyConfidentialityValue
    );
  }

  async verifyPreValidConfidentialityReferralReasons(caseId: string, user) {
    await this.loginUserWithCaseId(user, false, caseId);
    await this.homePage.chooseEvent('Send to interloc - pre-valid');

    await this.sendToInterlocPrevalidPage.verifyPageContentForTheInterlocReferralPage();
    await this.sendToInterlocPrevalidPage.verifyReasonReferredOptions([
      sendToInterlocData.sendToInterlocConfidentialityReasonValue,
      sendToInterlocData.sendToInterlocReviewConfidentialityRequestValue
    ]);
    await this.sendToInterlocPrevalidPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocConfidentialityReasonValue
    );
    await this.sendToInterlocPrevalidPage.verifySelectedReasonReferred(
      sendToInterlocData.sendToInterlocConfidentialityReasonValue
    );
  }
}
