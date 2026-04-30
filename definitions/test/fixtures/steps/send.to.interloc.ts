import { Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from '../../config/config';
import { SendToInterlocPage } from '../../pages/send.to.interloc.page';
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
    await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
    await this.homePage.chooseEvent('Send to interloc');

    const sendToInterlocPage = new SendToInterlocPage(this.page);
    await sendToInterlocPage.verifyPageContent();
    await sendToInterlocPage.selectHearingType(
      sendToInterlocData.sendToInterlocHearingSelectValue
    );
    await sendToInterlocPage.selectCaseReview(
      sendToInterlocData.sendToInterlocCaseReviewSelectValue
    );
    await sendToInterlocPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocReasonReferredValue
    );
    await sendToInterlocPage.confirmSubmission();

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
      true,
      caseId
    );
    await this.homePage.chooseEvent('Send to interloc');

    const sendToInterlocPage = new SendToInterlocPage(this.page);
    await sendToInterlocPage.verifyPageContent();
    await sendToInterlocPage.selectHearingType(
      sendToInterlocData.sendToInterlocHearingSelectValue
    );
    await sendToInterlocPage.selectCaseReview(
      sendToInterlocData.sendToInterlocCaseReviewSelectValue
    );
    await sendToInterlocPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocReferralReasonOver300Pages
    );
    await sendToInterlocPage.confirmSubmission();

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
    await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
    await this.homePage.chooseEvent('Send to interloc');

    const sendToInterlocPage = new SendToInterlocPage(this.page);
    await sendToInterlocPage.verifyPageContent();
    await sendToInterlocPage.selectHearingType(
      sendToInterlocData.sendToInterlocHearingSelectValue
    );
    await sendToInterlocPage.selectCaseReview(
      sendToInterlocData.sendToInterlocCaseReviewSelectValueJudge
    );
    await sendToInterlocPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocReferralReasonComplexCase
    );
    await sendToInterlocPage.confirmSubmission();

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
    await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
    await this.homePage.chooseEvent('Send to interloc');

    const sendToInterlocPage = new SendToInterlocPage(this.page);
    await sendToInterlocPage.verifyPageContent();
    await sendToInterlocPage.selectHearingType(
      sendToInterlocData.sendToInterlocHearingSelectValue
    );
    await sendToInterlocPage.selectCaseReview(
      sendToInterlocData.sendToInterlocCaseReviewSelectValueJudge
    );
    await sendToInterlocPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocReasonReferredValue
    );
    await sendToInterlocPage.confirmSubmission();

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
    await this.loginUserWithCaseId(user, true, caseId);
    await this.homePage.chooseEvent('Send to interloc');

    const sendToInterlocPage = new SendToInterlocPage(this.page);
    await sendToInterlocPage.verifyPageContent();
    await sendToInterlocPage.selectHearingType(
      sendToInterlocData.sendToInterlocHearingSelectValue
    );
    await sendToInterlocPage.selectCaseReview(
      sendToInterlocData.sendToInterlocCaseReviewSelectValue
    );
    await sendToInterlocPage.verifyReasonReferredOptions([
      sendToInterlocData.sendToInterlocConfidentialityReasonValue,
      sendToInterlocData.sendToInterlocReviewConfidentialityRequestValue
    ]);
    await sendToInterlocPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocConfidentialityReasonValue
    );
    await sendToInterlocPage.verifySelectedReasonReferred(
      sendToInterlocData.sendToInterlocConfidentialityReasonValue
    );
  }

  async submitConfidentialityReferralAndVerifySummary(caseId: string, user) {
    await this.loginUserWithCaseId(user, true, caseId);
    await this.homePage.chooseEvent('Send to interloc');

    const sendToInterlocPage = new SendToInterlocPage(this.page);
    await sendToInterlocPage.verifyPageContent();
    await sendToInterlocPage.selectHearingType(
      sendToInterlocData.sendToInterlocHearingSelectValue
    );
    await sendToInterlocPage.selectCaseReview(
      sendToInterlocData.sendToInterlocCaseReviewSelectValue
    );
    await sendToInterlocPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocConfidentialityReasonValue
    );
    await sendToInterlocPage.confirmSubmission();

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
  }

  async verifyPreValidConfidentialityReferralReasons(caseId: string, user) {
    await this.loginUserWithCaseId(user, true, caseId);
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
