import { BaseStep } from './base';
import { Page } from '@playwright/test';
import { credentials } from '../../config/config';
import { StepsHelper } from '../../helpers/stepsHelper';

const listingRequirementsTestData = require('../../pages/content/listing.requirements.json');
const uploadResponseTestdata = require('../../pages/content/upload.response_en.json');

export class UpdateListingRequirement extends BaseStep {
  readonly page: Page;
  protected stepsHelper: StepsHelper;

  constructor(page) {
    super(page);
    this.page = page;
    this.stepsHelper = new StepsHelper(this.page);
  }

  async performUploadResponse(caseId) {

    await this.loginUserWithCaseId(credentials.hmrcSuperUser, false, caseId);
    await this.stepsHelper.uploadResponseHelper(
      uploadResponseTestdata.pipIssueCode,
      'No'
    );
    await this.checkYourAnswersPage.confirmSubmission();
  }

  async updateAndVerifyJoHTiers() {
    await this.homePage.chooseEvent('Update Listing Requirements');
    await this.homePage.delay(4000);
    await this.listingRequirementPage.updateHearingValues();
    await this.listingRequirementPage.amendJoH();
    await this.listingRequirementPage.submitUpdatedValues();

    await this.homePage.navigateToTab('Listing Requirements').catch(async () => {
      await this.page.locator('button.mat-tab-header-pagination-after').click();
      await this.homePage.navigateToTab('Listing Requirements');
    });
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
       listingRequirementsTestData.durationField,
       listingRequirementsTestData.overrideDurationValue
    );
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
       listingRequirementsTestData.interpreterField,
       listingRequirementsTestData.interpreterValue
    );
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
       listingRequirementsTestData.johJudgeField,
       listingRequirementsTestData.johRegionalJudgeValue
    );
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
       listingRequirementsTestData.johMedicalMemField,
       listingRequirementsTestData.johRegionalMemField
    );
    await this.listingRequirementsTab.verifyContentNotPresent(
        listingRequirementsTestData.johTribunalDisabilityMemField,
        listingRequirementsTestData.johTribunalDisabilityMemValue
    );
  }
}