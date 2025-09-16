import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import writeAdjournmentDecisionData from './content/write.adjournment.decision_en.json';

let webActions: WebAction;

export class WriteAdjournmentPages {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async inputTypeOfAppealPageData(
    generateNotice = true,
    appealType = 'PIP'
  ) {
    switch (appealType) {
      case 'PIP': {
        if (generateNotice === true) {
          await webActions.clickElementById(
            "[for='adjournCaseGenerateNotice_Yes']"
          );
        } else {
          await webActions.clickElementById(
            "[for='adjournCaseGenerateNotice_No']"
          );
        }
        break;
      }
      default: {
        //statements;
        break;
      }
    }
  }

  async selectPanelMemsNeeded(){
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.panelMemsNeededPageHeading);
    await webActions.clickElementById("[for='adjournCasePanelMembersExcluded-No']");
  }

  async inputPanelMembers() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.panelMembersPageHeading);
    await webActions.inputField(
          '#adjournCasePanelMember1',
          writeAdjournmentDecisionData.panelMember1
        );
        await webActions.inputField(
          '#adjournCasePanelMember2',
          writeAdjournmentDecisionData.panelMember2
        );
  }

  async selectTypeOfHearingHeld() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.hearingTypeheading);
    await webActions.clickElementById("[for='adjournCaseTypeOfHearing-faceToFace']");
  }

  async selectToBeListed(setToRTL: boolean) {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.caseDirectionsPageHeading);
    if(setToRTL) { 
      await webActions.clickElementById("[for='adjournCaseCanCaseBeListedRightAway_Yes']");
    } else {
      await webActions.clickElementById("[for='adjournCaseCanCaseBeListedRightAway_No']");
    }
  }

  async selectDirectionToParties() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.directionToPartiesPageHeading);
    await webActions.clickElementById("[for='adjournCaseAreDirectionsBeingMadeToParties_Yes']");
  }

  async selectDirectionDueDates() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.directionsDueDatePageHeading);
    await webActions.clickElementById("[for='adjournCaseDirectionsDueDateDaysOffset-28']");
  }
  
  async selectNextHearingType() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.nextHearingFormatPageHeading);
    await webActions.clickElementById("[for='adjournCaseTypeOfNextHearing-faceToFace']");
  }

  async selectHearingVenue() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.nextHearingVenuePageHeading);
    await webActions.clickElementById("[for='adjournCaseNextHearingVenue-sameVenue']");
  }

  async selectStandardTimeSlot() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.hearingTimeSlotPageHeading);
    await webActions.clickElementById("[for='adjournCaseNextHearingListingDurationType-standardTimeSlot']");
  }

  async selectNoInterpreterRequired() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.languageInterpreterPageHeading);
    await webActions.clickElementById("[for='adjournCaseInterpreterRequired_No']");
  }

  async selectFirstAvailableDate() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.nextHearingDatePageHeading);
    await webActions.clickElementById("[for='adjournCaseNextHearingDateType-firstAvailableDate']");
  }

  async addReasonForAdjournment(reason: string) {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.adjournmentReasonPageHeading);
    await this.page.getByRole('button', { name: 'Add new' }).click();
    await webActions.inputField('#adjournCaseReasons_value', reason);
  }

  async confirmSubmission(): Promise<void> {
    await this.page.waitForTimeout(3000);
    await webActions.clickSubmitButton();
    await this.page.waitForTimeout(3000);
  }

  async verifyPageContentForPreviewDecisionNoticePage() {
      await webActions.verifyPageLabel(
        '.govuk-caption-l',
        writeAdjournmentDecisionData.issueAdjDecisionEventNameCaptor
      );
    await webActions.verifyPageLabel(
      'h1.govuk-heading-l',
      writeAdjournmentDecisionData.previewAdjournmentNoticePageHeading
    );
    await webActions.verifyPageLabel(
      '.form-label',
      writeAdjournmentDecisionData.previewAdjournmentNoticeLabel
    );
    await webActions.verifyPageLabel(
      '.form-hint',
      writeAdjournmentDecisionData.previewDecisionNoticeGuidanceText
    );
  }

  async submitContinueBtn(): Promise<void> {
    await webActions.clickButton('Continue');
  }
}