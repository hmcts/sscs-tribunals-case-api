import { APIRequestContext, expect, Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials, environment } from '../../config/config';
import createCaseBasedOnCaseType from '../../api/client/sscs/factory/appeal.type.factory';
import { StepsHelper } from '../../helpers/stepsHelper';
import task from '../../pages/content/review.fta.response.task_en.json';
import { VoidCase } from './void.case';

const responseReviewedTestData = require('../../pages/content/response.reviewed_en.json');
const uploadResponseTestdata = require('../../pages/content/upload.response_en.json');
const ucbTestData = require('../../pages/content/update.ucb_en.json');
const listingRequirementsTestData = require('../../pages/content/listing.requirements.json');

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

  async validateHistory(caseId: string, needsToLogin: boolean = true) {
    let historyLinks = this.presetLinks;
    historyLinks.push('Add a hearing');
    if (needsToLogin) {
      await this.loginUserWithCaseId(credentials.hmrcSuperUser, false, caseId);
    }
    await this.homePage.navigateToTab('Summary');
    await this.homePage.delay(1000);
    await this.homePage.reloadPage();
    try {
      await this.homePage.navigateToTab('Summary');
      await this.summaryTab.verifyPresenceOfText('Ready to list');
      await this.homePage.clickBeforeTabBtn();
      await this.homePage.navigateToTab('History');
      await Promise.all(
        historyLinks.map((linkName) => this.verifyHistoryTabLink(linkName))
      );
    } catch {
      await this.homePage.reloadPage();
      await this.homePage.navigateToTab('Summary');
      await this.summaryTab.verifyPresenceOfText('Ready to list');
      await this.homePage.clickBeforeTabBtn();
      await this.homePage.navigateToTab('History');
      await Promise.all(
        historyLinks.map((linkName) => this.verifyHistoryTabLink(linkName))
      );
    }
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
    await this.summaryTab.verifyPresenceOfText('Ready to list');
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

  async performUploadResponseOnAUniversalCreditWithJP(ucCaseId: string) {
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

    await this.validateHistory(ucCaseId);
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
    await this.uploadResponsePage.continueSubmission();

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
    await this.uploadResponsePage.continueSubmission();

    await this.checkYourAnswersPage.confirmSubmission();
    await this.checkYourAnswersPage.verifyIssueCodeErrorMsg();
    // await performAppealDormantOnCase(UploadResponse.caseId);
  }

  async verifyCtscAdminWithCaseAllocatorRoleCanViewReviewFTAResponseTask(
    caseId: string
  ) {
    await this.uploadResponseWithFurtherInfoAsDwpCaseWorker(caseId);

    /* Login as CTSC Administrator with case allocator role and view the 
           unassigned Review FTA Response task */
    await this.loginUserWithCaseId(
      credentials.amCaseWorkerWithCaseAllocatorRole,
      true,
      caseId
    );
    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPresenceOfText('Response received');
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.verifyPriortiy(task.name, task.priority);
    await this.tasksTab.verifyPageContentByKeyValue(
      task.name,
      'Assigned to',
      task.assignedToWhenNotAssigned
    );
    await this.tasksTab.verifyManageOptions(
      task.name,
      task.unassignedManageOptionsForCaseAllocator
    );
  }

  async verifyCtscAdminWithoutCaseAllocatorRoleCanCompleteReviewFTAResponseTask(
    caseId: string
  ) {
    // Login as CTSC Administrator and view the unassigned Review FTA Response task
    await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.verifyPriortiy(task.name, task.priority);
    await this.tasksTab.verifyPageContentByKeyValue(
      task.name,
      'Assigned to',
      task.assignedToWhenNotAssigned
    );

    // CTSC Administrator self assigns task and verifies assigned task details
    await this.tasksTab.selfAssignTask(task.name);
    await this.tasksTab.verifyPageContentByKeyValue(
      task.name,
      'Assigned to',
      task.assignedTo
    );
    await this.tasksTab.verifyManageOptions(
      task.name,
      task.assignedManageOptions
    );
    await this.tasksTab.verifyNextStepsOptions(
      task.name,
      task.nextStepsOptions
    );

    // Select Response reviewed next step and complete the event
    await this.tasksTab.clickNextStepLink(task.responseReviewed.link);
    await this.responseReviewedPage.verifyPageContent(
      responseReviewedTestData.captionValue,
      responseReviewedTestData.headingValue
    );
    await this.responseReviewedPage.chooseInterlocOption('No');
    await this.responseReviewedPage.confirmSubmission();

    await expect(this.homePage.summaryTab).toBeVisible();

    // Verify task is removed from the tasks list within Tasks tab
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsHidden(task.name);
  }

  async verifyReviewFTAResponseTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(
    caseId: string
  ) {
    await this.uploadResponseWithFurtherInfoAsDwpCaseWorker(caseId);

    // Verify CTSC Admin can view the unassigned Review FTA Response task
    await this.homePage.signOut();
    await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.verifyManageOptions(
      task.name,
      task.unassignedManageOptions
    );

    // CTSC Administrator self assigns task and verifies assigned task details
    await this.tasksTab.selfAssignTask(task.name);
    await this.tasksTab.verifyPageContentByKeyValue(
      task.name,
      'Assigned to',
      task.assignedTo
    );
    await this.tasksTab.verifyManageOptions(
      task.name,
      task.assignedManageOptions
    );
    await this.tasksTab.verifyNextStepsOptions(
      task.name,
      task.nextStepsOptions
    );

    // CTSC Administrator voids the case
    let voidCase = new VoidCase(this.page);
    await voidCase.performVoidCase(caseId, false);

    // Verify task is removed from the tasks list within Tasks tab
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsHidden(task.name);
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
