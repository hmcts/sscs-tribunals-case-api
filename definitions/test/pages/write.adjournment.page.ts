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

  async inputTypeOfAppealPageData(generateNotice = true, appealType = 'PIP') {
    if (appealType === 'PIP') {
      const id = generateNotice
        ? "[for='adjournCaseGenerateNotice_Yes']"
        : "[for='adjournCaseGenerateNotice_No']";
      await webActions.clickElementById(id);
    }
  }

  async selectPanelMemsNeeded(){
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.panelMembers.excludedHeading);
    await webActions.clickElementById("[for='adjournCasePanelMembersExcluded-No']");
  }

  async inputPanelMembers() {
    await webActions.verifyPageLabel('h1.govuk-heading-l',  writeAdjournmentDecisionData.panelMembers.heading);
    await webActions.inputField(
          '#adjournCasePanelMember1',
          writeAdjournmentDecisionData.panelMembers.members[0]
        );
        await webActions.inputField(
          '#adjournCasePanelMember2',
          writeAdjournmentDecisionData.panelMembers.members[1]
        );
  }

  async selectTypeOfHearingHeld() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.hearing.typeHeading);
    await webActions.clickElementById("[for='adjournCaseTypeOfHearing-faceToFace']");
  }

  async selectToBeListed(setToRTL: boolean) {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.hearing.caseDirectionsHeading);
    const id = setToRTL
      ? "[for='adjournCaseCanCaseBeListedRightAway_Yes']"
      : "[for='adjournCaseCanCaseBeListedRightAway_No']";
    await webActions.clickElementById(id);
  }

  async selectDirectionToParties() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.hearing.directionToPartiesHeading);
    await webActions.clickElementById("[for='adjournCaseAreDirectionsBeingMadeToParties_Yes']");
  }

  async selectDirectionDueDates() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.hearing.directionsDueDateHeading);
    await webActions.clickElementById("[for='adjournCaseDirectionsDueDateDaysOffset-28']");
  }
  
  async selectNextHearingType() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.hearing.nextFormatHeading);
    await webActions.clickElementById("[for='adjournCaseTypeOfNextHearing-faceToFace']");
  }

  async selectHearingVenue() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.hearing.nextVenueHeading);
    await webActions.clickElementById("[for='adjournCaseNextHearingVenue-sameVenue']");
  }

  async selectStandardTimeSlot() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.hearing.timeSlotHeading);
    await webActions.clickElementById("[for='adjournCaseNextHearingListingDurationType-standardTimeSlot']");
  }

  async selectNoInterpreterRequired() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.hearing.languageInterpreterHeading);
    await webActions.clickElementById("[for='adjournCaseInterpreterRequired_No']");
  }

  async selectFirstAvailableDate() {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.hearing.nextDateHeading);
    await webActions.clickElementById("[for='adjournCaseNextHearingDateType-firstAvailableDate']");
  }

  async addReasonForAdjournment(reason: string) {
    await webActions.verifyPageLabel('h1.govuk-heading-l', writeAdjournmentDecisionData.adjournmentReasonHeading);
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
        writeAdjournmentDecisionData.issueAdjournmentEventName
      );
    await webActions.verifyPageLabel(
      'h1.govuk-heading-l',
      writeAdjournmentDecisionData.previewAdjournmentNotice.heading
    );
    await webActions.verifyPageLabel(
      '.form-label',
      writeAdjournmentDecisionData.previewAdjournmentNotice.label
    );
    await webActions.verifyPageLabel(
      '.form-hint',
      writeAdjournmentDecisionData.previewAdjournmentNotice.guidanceText
    );
  }

  async submitContinueBtn(): Promise<void> {
    await webActions.clickButton('Continue');
  }
}