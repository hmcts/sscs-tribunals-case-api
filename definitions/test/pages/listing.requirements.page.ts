import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webAction: WebAction;

export class ListingRequirementPage {
  readonly page;

  constructor(page: Page) {
    this.page = page;
    webAction = new WebAction(this.page);
  }

  async updateHearingValues(addDuration: boolean = false) {
    if(addDuration) await webAction.inputField('#overrideFields_duration', '120');
    await webAction.clickElementById(
      '#overrideFields_appellantInterpreter_isInterpreterWanted_Yes'
    );
    await webAction.chooseOptionByLabel(
      '#overrideFields_appellantInterpreter_interpreterLanguage',
      'Dutch'
    );
    await webAction.clickElementById('#overrideFields_autoList_Yes');
    if(addDuration) await webAction.clickButton('Continue');
  }

  async setAutolistOverrideValue(selection: boolean) {
    await webAction.clickElementById('#overrideFields_autoList_' + (selection ? "Yes" : "No"));
    await webAction.clickButton('Continue');
    await webAction.clickSubmitButton();
    await webAction.verifyElementVisibility("#field-trigger-summary");
  }

  async submitUpdatedValues() {
    await webAction.clickElementById('#amendReasons-adminreq');
    await webAction.clickSubmitButton();
    await webAction.verifyElementVisibility("#field-trigger-summary");
    await webAction.clickSubmitButton();
  }

  async submitUpdatedValuesNoReasons() {
    await webAction.clickSubmitButton();
    await webAction.verifyElementVisibility("#field-trigger-summary");
    await webAction.clickSubmitButton();
  }

  async amendJoH() {
    await webAction.clickElementById('#panelMemberComposition_panelCompositionJudge-74');
    await webAction.clickElementById('#panelMemberComposition_panelCompositionMemberMedical1-69');
    await webAction.unCheckAnCheckBox('#panelMemberComposition_panelCompositionDisabilityAndFqMember-44');
    await webAction.clickButton('Continue');
  }

  async submitEventNoChange() {
    await webAction.clickButton('Continue');
    await webAction.verifyElementVisibility('#amendReasons-adminreq');
    await webAction.clickSubmitButton();
    await webAction.verifyElementVisibility("#field-trigger-summary");
    await webAction.clickSubmitButton();
  }
  
  async submitEventNoChangeNoHearingRequested() {
    await webAction.clickButton('Continue');
    await webAction.clickSubmitButton();
    await webAction.verifyElementVisibility("#field-trigger-summary");
    await webAction.clickSubmitButton();
  }

  async updateHearingToDirection() {
    await webAction.clickElementById(
      '#overrideFields_hmcHearingType-BBA3-DIR'
    );
    await webAction.clickButton('Continue');
  }
}
