import { Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from '../../config/config';
import logger from '../../utils/loggerUtil';

const hearingTestData = require('../../pages/content/hearing.details_en.json');

export class Hearing extends BaseStep {
  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  async setAutolist(selection: boolean) {
    await this.homePage.navigateToTab('Summary');
    await this.homePage.chooseEvent('Update Listing Requirements');
    await this.listingRequirementPage.setAutolistOverrideValue(selection);
    await this.checkYourAnswersPage.confirmSubmission();
  }

  async verifyHearingIsTriggered(caseId: string, caseType: string) {
    await this.loginUserWithCaseId(credentials.hmrcSuperUser, false, caseId);
    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPresenceOfText('Ready to list');

    await this.homePage.navigateToTab(hearingTestData.tabName);
    await this.hearingsTab.verifyHearingStatusSummary(false);
    await this.hearingsTab.clickHearingDetails();

    if (caseType === 'pip') {
      await this.hearingsTab.verifyPageContentByKeyValue(
        hearingTestData.hearingLengthKey,
        hearingTestData.pipHearingLengthValue
      );
      await this.hearingsTab.verifyVenueListForPaperCase(
        hearingTestData.hearingVenueKey,
        33
      );
    } else if (caseType === 'dla') {
      await this.hearingsTab.verifyPageContentByKeyValue(
        hearingTestData.hearingLengthKey,
        hearingTestData.hearingLengthValue
      );
      await this.hearingsTab.verifyPageContentByKeyValue(
        hearingTestData.hearingVenueKey,
        hearingTestData.hearingVenueValue
      );
      await this.hearingsTab.verifyPageContentByKeyValue(
        hearingTestData.hearingAttendanceKey,
        hearingTestData.hearingAttendanceValue
      );
    } else {
      logger.info('No case type passed');
    }

    await this.hearingsTab.verifyPageContentByKeyValue(
      hearingTestData.hearingPriorityKey,
      hearingTestData.hearingPriorityValue
    );
    await this.hearingsTab.verifyExpHearingDateIsGenerated('31');
    await this.hearingsTab.clickBackLink();
  }

  async verifyHearingIsTriggeredForUCCase(isDirectionHearing: boolean) {
    await this.homePage.navigateToTab(hearingTestData.tabName);
    await this.hearingsTab.verifyHearingStatusSummary(isDirectionHearing);
    await this.hearingsTab.clickHearingDetails();
    await this.hearingsTab.verifyPageContentByKeyValue(
      hearingTestData.hearingLengthKey,
      hearingTestData.ucHearingLengthValue
    );
    await this.hearingsTab.verifyPageContentByKeyValue(
      hearingTestData.hearingVenueKey,
      hearingTestData.hearingVenueValue
    );
    await this.hearingsTab.verifyPageContentByKeyValue(
      hearingTestData.hearingPriorityKey,
      hearingTestData.hearingPriorityValue
    );
    await this.hearingsTab.verifyPageContentByKeyValue(
      hearingTestData.hearingAttendanceKey,
      hearingTestData.hearingAttendanceValue
    );
    await this.hearingsTab.verifyExpHearingDateIsGenerated('31');
    await this.hearingsTab.clickBackLink();
  }

  async verifyManualHearingCancellation() {
    await this.hearingsTab.clickCancelLink();
    await this.hearingsTab.submitCancellationReason();
    await this.hearingsTab.verifyCancellationStatusSummary();
    await this.hearingsTab.verifyCancellationDetails('Incomplete Tribunal');
  }

  async verifyAutoHearingCancellation() {
    await this.homePage.clickAfterTabBtn();
    await this.homePage.navigateToTab('Hearings');
    await this.hearingsTab.verifyCancellationStatusSummary();
    await this.hearingsTab.verifyCancellationDetails('Other');
  }

  async updateHearingLengthManually() {
    await this.hearingsTab.clickHearingDetails();
    await this.hearingsTab.updateHearingDuration();
  }

  async verifyUpdatedHearingStatus() {
    await this.hearingsTab.verifyPageContentByKeyValue(
      hearingTestData.hearingLengthKey,
      '2 Hours'
    );
    await this.hearingsTab.sumitUpdate();
    await this.hearingsTab.verifyUpdateStatusSummary();
  }

  async verifyUpdatedHearingStatusViaEvent() {
    await this.homePage.navigateToTab('Hearings');
    await this.hearingsTab.verifyUpdateStatusSummary();
  }

  async updateHearingViaEvent() {
    await this.homePage.chooseEvent('Update Listing Requirements');
    await this.listingRequirementPage.updateHearingValues(true);
    await this.listingRequirementPage.submitUpdatedValues();

    await this.homePage.clickAfterTabBtn();
    await this.homePage.navigateToTab('Listing Requirements');
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
      'Duration of the hearing',
      '120'
    );
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
      'Is an interpreter wanted?',
      'Yes'
    );
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
      'Interpreter Language',
      'Dutch'
    );
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
      'Amend Reason',
      'Admin requested change'
    );
  }

  async updateHearingToDirectionViaEvent() {
    await this.homePage.chooseEvent('Update Listing Requirements');
    await this.listingRequirementPage.updateHearingToDirection();
    await this.listingRequirementPage.submitUpdatedValuesNoReasons();

    await this.homePage.clickAfterTabBtn();
    await this.homePage.navigateToTab('Listing Requirements');
    await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
      'Next hearing type',
      'Direction'
    );
  }

  async updateListingRequirementsNoChange() {
    await this.homePage.chooseEvent('Update Listing Requirements');
    await this.listingRequirementPage.submitEventNoChange();
  }

  async cancelHearingForCleanUp() {
    await this.hearingsTab.clickCancelLink();
    await this.hearingsTab.submitCancellationReason();
  }
}
