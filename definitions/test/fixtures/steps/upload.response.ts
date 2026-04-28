import { APIRequestContext, expect, Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials, environment } from '../../config/config';
import createCaseBasedOnCaseType from '../../api/client/sscs/factory/appeal.type.factory';
import { StepsHelper } from '../../helpers/stepsHelper';
import sendToInterlocData from '../../pages/content/send.to.interloc_en.json';

const responseReviewedTestData = require('../../pages/content/response.reviewed_en.json');
const uploadResponseTestdata = require('../../pages/content/upload.response_en.json');
const ucbTestData = require('../../pages/content/update.ucb_en.json');
const listingRequirementsTestData = require('../../pages/content/listing.requirements.json');
const eventTestData = require('../../pages/content/event.name.event.description_en.json');

export class UploadResponse extends BaseStep {
  private static caseId: string;
  readonly page: Page;
  protected stepsHelper: StepsHelper;

  private presetLinks: string[] = [
    'Upload response',
    'Ready to list',
    'Update to case data'
  ];

  constructor(page: Page) {
    super(page);
    this.page = page;
    this.stepsHelper = new StepsHelper(this.page);
  }

  private async chooseFirstAvailableEvent(eventNames: string[]) {
    const availableOptions = (
      await this.page.locator('#next-step option').allTextContents()
    )
      .map((option) => option.trim())
      .filter((option) => option !== '');

    const eventName = eventNames.find((candidate) =>
      availableOptions.includes(candidate)
    );

    if (!eventName) {
      throw new Error(
        `None of the expected events are available: ${eventNames.join(', ')}`
      );
    }

    await this.homePage.chooseEvent(eventName);
    return eventName;
  }

  private async isEventAvailable(eventName: string) {
    const availableOptions = (
      await this.page.locator('#next-step option').allTextContents()
    )
      .map((option) => option.trim())
      .filter((option) => option !== '');

    return availableOptions.includes(eventName);
  }

  async validateHistory(caseId: string, needsToLogin: boolean = true) {
    let historyLinks = this.presetLinks;
    if (needsToLogin) {
      await this.loginUserWithCaseId(credentials.hmrcSuperUser, false, caseId);
    }
    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPresenceOfText('Ready to list');
    if(environment.name == "aat") await this.homePage.clickBeforeTabBtn();
    await this.homePage.navigateToTab('History');
    await Promise.all(
        historyLinks.map((linkName) => this.verifyHistoryTabLink(linkName))
    );
  }

  async performUploadResponseWithFurtherInfoOnAPIPAndReviewResponse(pipCaseId: string) {
    await this.uploadResponseWithFurtherInfoAsDwpCaseWorker(pipCaseId);
    await this.homePage.clickSignOut();

    await this.loginUserWithCaseId(credentials.amCaseWorker, false, pipCaseId);
    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPresenceOfText('Response received');

    await this.homePage.chooseEvent('Response reviewed');
    await this.responseReviewedPage.verifyPageContent(
      responseReviewedTestData.captionValue,
      responseReviewedTestData.headingValue
    );
    await this.responseReviewedPage.chooseInterlocOption('No');
    await this.checkYourAnswersPage.confirmAndSignOut();

    await this.validateHistory(pipCaseId);

    await this.homePage.navigateToTab('Listing Requirements').catch(async () => {
      await this.page.locator('button.mat-tab-header-pagination-after').click();
      await this.homePage.navigateToTab('Listing Requirements');
    });
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
      listingRequirementsTestData.johJudgeField,
      listingRequirementsTestData.johJudgeValue
    );
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
      listingRequirementsTestData.johMedicalMemField,
      listingRequirementsTestData.johMedicalMemValue
    );
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
      listingRequirementsTestData.johTribunalDisabilityMemField,
      listingRequirementsTestData.johTribunalDisabilityMemValue
    );
  }

  async performUploadResponseWithPHEOnAPIPAndReviewResponse(caseId: string) {
    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      caseId
    );
    await this.stepsHelper.uploadResponseHelper(
      uploadResponseTestdata.pipIssueCode,
      'No',
      true
    );

    await this.checkYourAnswersPage.verifyCYAPageContentWithPHE(
      'Upload response',
      uploadResponseTestdata.pipBenefitCode,
      uploadResponseTestdata.pipIssueCode
    );
    await this.checkYourAnswersPage.confirmAndSignOut();

    await this.homePage.delay(3000);
    await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);

    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPresenceOfText('Response received');
    await this.summaryTab.verifyPresenceOfTitle(
      'PHE on this case: Under Review'
    );
    await this.homePage.clickSignOut();
  }

  async performUploadResponseWithUCBOnAPIP(caseId: string) {
    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      caseId
    );
    await this.stepsHelper.uploadResponseHelper(
      uploadResponseTestdata.pipIssueCode,
      'No',
      undefined,
      true
    );

    await this.checkYourAnswersPage.verifyCYAPageContentWithUCB(
      'Upload response',
      uploadResponseTestdata.pipBenefitCode,
      uploadResponseTestdata.pipIssueCode
    );
    await this.checkYourAnswersPage.confirmAndSignOut();

    await this.homePage.delay(3000);
    await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);

    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPresenceOfText('Ready to list');
    try {
      await this.homePage.navigateToTab('Listing Requirements');
    } catch {
      await this.page.locator('button.mat-tab-header-pagination-after').click();
      await this.homePage.navigateToTab('Listing Requirements');
    }
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
      ucbTestData.ucbFieldLabel,
      ucbTestData.ucbFieldValue_Yes
    );
  }

  async performUploadResponseWithAVEvidenceOnAPIP(caseId: string) {
    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      caseId
    );
    await this.stepsHelper.uploadResponseHelper(
      uploadResponseTestdata.pipIssueCode,
      'No',
      undefined,
      undefined,
      true
    );
    await this.checkYourAnswersPage.confirmAndSignOut();
  }

  async performUploadResponse(caseId: string, caseType: string) {
    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      caseId
    );
    if (caseType === 'dla')
      await this.stepsHelper.uploadResponseHelper(
        uploadResponseTestdata.dlaIssueCode,
        'No',
        undefined,
        undefined,
        undefined
      );
    if (caseType === 'pip')
      await this.stepsHelper.uploadResponseHelper(
        uploadResponseTestdata.pipIssueCode,
        'No',
        undefined,
        undefined,
        undefined
      );
    await this.checkYourAnswersPage.confirmAndSignOut();
  }

  async performUploadResponseWithoutFurtherInfoOnATaxCredit() {
    let taxCaseId = await createCaseBasedOnCaseType('TAX CREDIT');
    await this.loginUserWithCaseId(credentials.hmrcUser, false, taxCaseId);
    await this.stepsHelper.uploadResponseHelper(
      uploadResponseTestdata.taxIssueCode,
      'No'
    );

    await this.checkYourAnswersPage.verifyCYAPageContent(
      'Upload response',
      uploadResponseTestdata.taxBenefitCode,
      uploadResponseTestdata.taxIssueCode
    );
    await this.checkYourAnswersPage.confirmAndSignOut();

    await this.validateHistory(taxCaseId);

    await this.homePage.navigateToTab('Listing Requirements').catch(async () => {
      await this.page.locator('button.mat-tab-header-pagination-after').click();
      await this.homePage.navigateToTab('Listing Requirements');
    });
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
      listingRequirementsTestData.johJudgeField,
      listingRequirementsTestData.johJudgeValue
    );
  }

  async performUploadResponseOnAUniversalCredit(
    ucCaseId: string,
    needsToLogin: boolean = true
  ) {
    if (needsToLogin) {
      await this.loginUserWithCaseId(
        credentials.dwpResponseWriter,
        false,
        ucCaseId
      );
    }
    await this.homePage.chooseEvent('Upload response');
    await this.uploadResponsePage.verifyPageContent();
    await this.uploadResponsePage.uploadDocs();
    await this.uploadResponsePage.chooseAssistOption('No');
    await this.uploadResponsePage.continueSubmission();

    await this.uploadResponsePage.selectElementDisputed('childElement');
    await this.uploadResponsePage.continueSubmission();

    await this.uploadResponsePage.clickAddNewButton();
    await this.uploadResponsePage.selectUcIssueCode(
      uploadResponseTestdata.elementDisputedForChildElementIssueCodes,
      uploadResponseTestdata.ucIssueCode
    );
    await this.homePage.delay(2000);
    await this.uploadResponsePage.continueSubmission();

    await this.uploadResponsePage.chooseDisputeOption(
      uploadResponseTestdata.ucDisputeOption
    );
    await this.homePage.delay(2000);
    await this.uploadResponsePage.continueSubmission();

    await this.uploadResponsePage.isJPOnTheCase(
      uploadResponseTestdata.ucJointPartyOnCase
    );
    await this.uploadResponsePage.continueSubmission();

    await this.checkYourAnswersPage.verifyCYAPageContent(
      'Upload response',
      null,
      null,
      'UC'
    );
    if (needsToLogin) {
      await this.checkYourAnswersPage.confirmAndSignOut();
    } else {
      await this.checkYourAnswersPage.confirmSubmission();
    }

    await this.validateHistory(ucCaseId, needsToLogin);

    await this.homePage.navigateToTab('Listing Requirements').catch(async () => {
      await this.page.locator('button.mat-tab-header-pagination-after').click();
      await this.homePage.navigateToTab('Listing Requirements');
    });
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
      listingRequirementsTestData.johJudgeField,
      listingRequirementsTestData.johJudgeValue
    );
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
      listingRequirementsTestData.johMedicalMemField,
      listingRequirementsTestData.johMedicalMemValue
    );
  }

  async performUploadResponseOnAUniversalCreditWithJP(
    ucCaseId: string,
    validateHistoryAfterUpload: boolean = true
  ) {
    // let ucCaseId = await createCaseBasedOnCaseType("UC");
    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      ucCaseId
    );

    await this.homePage.chooseEvent('Upload response');
    await this.uploadResponsePage.verifyPageContent();
    await this.uploadResponsePage.uploadDocs();
    await this.uploadResponsePage.chooseAssistOption('No');
    await this.uploadResponsePage.continueSubmission();

    await this.uploadResponsePage.selectElementDisputed('childElement');
    await this.uploadResponsePage.continueSubmission();

    await this.uploadResponsePage.clickAddNewButton();
    await this.uploadResponsePage.selectUcIssueCode(
      uploadResponseTestdata.elementDisputedForChildElementIssueCodes,
      uploadResponseTestdata.ucIssueCode
    );
    await this.homePage.delay(2000);
    await this.uploadResponsePage.continueSubmission();

    await this.uploadResponsePage.chooseDisputeOption(
      uploadResponseTestdata.ucDisputeOption
    );
    await this.homePage.delay(2000);
    await this.uploadResponsePage.continueSubmission();

    await this.uploadResponsePage.isJPOnTheCase(
      uploadResponseTestdata.ucJointPartyOnCase_Yes
    );
    await this.uploadResponsePage.continueSubmission();
    await this.uploadResponsePage.enterJPDetails();
    await this.checkYourAnswersPage.confirmAndSignOut();

    if (validateHistoryAfterUpload) {
      await this.validateHistory(ucCaseId);
    }
  }

  async prepareChildSupportCaseForResponseReviewed(caseId: string) {
    await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
    await this.homePage.chooseEvent('Update to case data');
    await this.page
      .getByRole('textbox', { name: 'Child maintenance number' })
      .fill('00123');
    await this.page
      .getByRole('group', { name: 'Confidentiality Status' })
      .getByLabel('No')
      .check();
    await this.page.getByRole('button', { name: 'Submit', exact: true }).click();
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await expect(this.homePage.summaryTab).toBeVisible();

    const otherPartyEventName = await this.chooseFirstAvailableEvent([
      'Add other party data',
      'Update other party data'
    ]);
    await this.updateOtherPartyDataPage.verifyPageContent(otherPartyEventName);
    await this.updateOtherPartyDataPage.applyOtherPartyData();
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await expect(this.homePage.summaryTab).toBeVisible();

    await this.homePage.chooseEvent('Update subscription');
    await this.updateOtherPartyDataPage.applyOtherPartiesSubscription();
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await expect(this.homePage.summaryTab).toBeVisible();

    if (await this.isEventAvailable('Ready to list')) {
      await this.homePage.chooseEvent('Ready to list');
      await this.eventNameAndDescriptionPage.verifyPageContent('Ready to list');
      await this.eventNameAndDescriptionPage.inputData(
        eventTestData.eventSummaryInput,
        eventTestData.eventDescriptionInput
      );
      await this.eventNameAndDescriptionPage.confirmSubmission();
      await expect(this.homePage.summaryTab).toBeVisible();
    }

    await this.homePage.signOut();

    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      caseId
    );
    for (let attempt = 0; attempt < 2; attempt++) {
      await this.homePage.chooseEvent('Upload response');
      await this.uploadResponsePage.verifyPageContent();
      await this.uploadResponsePage.uploadChildSupportConfidentialDocs();
      await this.uploadResponsePage.continueSubmission();
      await this.uploadResponsePage.addChildSupportOtherParty();
      await this.page
        .getByRole('button', { name: 'Submit', exact: true })
        .click();
      await expect(this.homePage.summaryTab).toBeVisible();

      if (await this.page.getByText('Response received').isVisible()) {
        break;
      }
    }
    await this.homePage.signOut();
  }

  async verifyChildSupportResponseReviewedConfidentialityReferralReasons(
    caseId: string,
    user,
    reviewer: string
  ) {
    await this.loginUserWithCaseId(user, false, caseId);
    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPresenceOfText('Response received');

    await this.homePage.chooseEvent('Response reviewed');
    await this.responseReviewedPage.verifyPageContent(
      responseReviewedTestData.captionValue,
      responseReviewedTestData.headingValue
    );
    await this.responseReviewedPage.chooseInterlocOption('Yes');
    await this.responseReviewedPage.selectCaseReview(reviewer);
    await this.responseReviewedPage.verifyReasonReferredOptions([
      sendToInterlocData.sendToInterlocConfidentialityReasonValue,
      sendToInterlocData.sendToInterlocReviewConfidentialityRequestValue
    ]);
    await this.responseReviewedPage.selectReasonReferred(
      sendToInterlocData.sendToInterlocConfidentialityReasonValue
    );
    await this.responseReviewedPage.verifySelectedReasonReferred(
      sendToInterlocData.sendToInterlocConfidentialityReasonValue
    );
  }

  async verifyErrorsScenariosInUploadResponse() {
    UploadResponse.caseId = await createCaseBasedOnCaseType('PIP');
    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      UploadResponse.caseId
    );

    await this.homePage.chooseEvent('Upload response');
    await this.uploadResponsePage.verifyPageContent();
    await this.uploadResponsePage.uploadPartialDocs();
    await this.uploadResponsePage.selectIssueCode(
      uploadResponseTestdata.pipIssueCode
    );
    await this.uploadResponsePage.chooseAssistOption('Yes');
    await this.uploadResponsePage.continueSubmission();
    await this.uploadResponsePage.delay(1000);
    await this.uploadResponsePage.verifyDocMissingErrorMsg();
    // await performAppealDormantOnCase(pipErrorCaseId);
  }

  async verifyPHMEErrorsScenariosInUploadResponse() {
    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      UploadResponse.caseId
    );

    await this.homePage.chooseEvent('Upload response');
    await this.uploadResponsePage.verifyPageContent();
    await this.uploadResponsePage.uploadDocs();
    await this.uploadResponsePage.selectEvidenceReason(
      'Potentially harmful evidence'
    );
    await this.uploadResponsePage.selectIssueCode(
      uploadResponseTestdata.pipIssueCode
    );
    await this.uploadResponsePage.chooseAssistOption('Yes');
    await this.uploadResponsePage.continueSubmission('Yes');

    await this.checkYourAnswersPage.confirmSubmission();
    await this.checkYourAnswersPage.verifyPHMEErrorMsg();
  }

  async verifyIssueCodeErrorsScenariosInUploadResponse() {
    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      UploadResponse.caseId
    );

    await this.homePage.chooseEvent('Upload response');
    await this.uploadResponsePage.verifyPageContent();
    await this.uploadResponsePage.uploadDocs();
    await this.uploadResponsePage.selectIssueCode('DD');
    await this.uploadResponsePage.chooseAssistOption('Yes');
    await this.uploadResponsePage.continueSubmission('Yes');

    await this.checkYourAnswersPage.confirmSubmission();
    await this.checkYourAnswersPage.verifyIssueCodeErrorMsg();
    // await performAppealDormantOnCase(UploadResponse.caseId);
  }

  async uploadResponseWithFurtherInfoAsDwpCaseWorker(caseId: string) {
    // As DWP caseworker upload response with further info
    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      caseId
    );
    await this.stepsHelper.uploadResponseHelper(
      uploadResponseTestdata.pipIssueCode,
      'Yes'
    );

    await this.checkYourAnswersPage.verifyCYAPageContent(
      'Upload response',
      uploadResponseTestdata.pipBenefitCode,
      uploadResponseTestdata.pipIssueCode
    );
    await this.checkYourAnswersPage.confirmSubmission();
  }

  async uploadResponseWithoutFurtherInfoAsDwpCaseWorker(caseId: string) {
    // As DWP caseworker upload response with further info
    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      caseId
    );
    await this.stepsHelper.uploadResponseHelper(
      uploadResponseTestdata.pipIssueCode,
      'No'
    );

    await this.checkYourAnswersPage.verifyCYAPageContent(
      'Upload response',
      uploadResponseTestdata.pipBenefitCode,
      uploadResponseTestdata.pipIssueCode
    );
    await this.checkYourAnswersPage.confirmSubmission();
  }

  async uploadResponseWithoutFurtherInfoAsDwpCaseWorkerAndMarkCaseAsUrgent(caseId: string) {
    
    await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
    await this.stepsHelper.setCaseAsUrgentHelper();

    // As DWP caseworker upload response with further info
    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      caseId
    );
    await this.stepsHelper.uploadResponseHelper(
      uploadResponseTestdata.pipIssueCode,
      'No'
    );

    await this.checkYourAnswersPage.verifyCYAPageContent(
      'Upload response',
      uploadResponseTestdata.pipBenefitCode,
      uploadResponseTestdata.pipIssueCode
    );
    await this.checkYourAnswersPage.confirmSubmission();
  }

  async uploadResponseUcAppealWcaAndSvIssueCode(caseId: string){
    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      caseId
    );

    await this.homePage.chooseEvent('Upload response');
    await this.uploadResponsePage.verifyPageContent();
    await this.uploadResponsePage.uploadDocs();
    await this.uploadResponsePage.chooseAssistOption('No');
    await this.uploadResponsePage.continueSubmission();

    await this.uploadResponsePage.selectElementDisputed('limitedCapabilityWork');
    await this.uploadResponsePage.continueSubmission();

    await this.uploadResponsePage.clickAddNewButton();
    await this.uploadResponsePage.selectUcIssueCode(
      uploadResponseTestdata.elementDisputedForLimitedWorkIssueCodes,
      uploadResponseTestdata.sccIssueCode
    );
    await this.homePage.delay(2000);
    await this.uploadResponsePage.continueSubmission();

    await this.uploadResponsePage.chooseDisputeOption(
      uploadResponseTestdata.ucDisputeOption
    );
     await this.uploadResponsePage.continueSubmission();

    await this.uploadResponsePage.isJPOnTheCase(uploadResponseTestdata.ucJointPartyOnCase)
    await this.homePage.delay(2000);
    await this.uploadResponsePage.continueSubmission();

    await this.checkYourAnswersPage.confirmAndSignOut();

    await this.validateHistory(caseId);

  }

  async uploadResponseEsaAppealWcaAndSvIssueCode(caseId: string){
    await this.loginUserWithCaseId(
      credentials.dwpResponseWriter,
      false,
      caseId
    );

    await this.homePage.chooseEvent('Upload response');
    await this.uploadResponsePage.verifyPageContent();
    await this.uploadResponsePage.uploadDocs();
    await this.uploadResponsePage.chooseAssistOption('No');
     await this.uploadResponsePage.selectIssueCode(
      uploadResponseTestdata.sccIssueCode
    );
    await this.uploadResponsePage.continueSubmission();
    await this.checkYourAnswersPage.confirmAndSignOut();
    await this.validateHistory(caseId);
  }

  async checkHmcEnvironment(request: APIRequestContext) {
    if (environment.name == 'aat') {
      console.log('Checking HMC AAT is up before attempting test...');
      const response = await request.get(
        'http://hmc-cft-hearing-service-aat.service.core-compute-aat.internal/health'
      )
      expect(response.status()).toBe(200)
    }
  }

  async navigateToHearingsTab() {
    await this.homePage.navigateToTab('Hearings');
  }
}
