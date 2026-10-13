import { expect, Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import addOtherPartyData from '../pages/content/add.other.party.data.json';

let webActions: WebAction;

export class AddOtherPartyDataPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webActions = new WebAction(this.page);
  }

  async verifyPageContent() {
    await webActions.verifyPageLabel(
      'h1.govuk-heading-l',
      addOtherPartyData.eventName
    );
    await webActions.verifyPageLabel(
      '#addOtherPartyDetailsLabel p',
      addOtherPartyData.description
    );
    await webActions.verifyPageLabel(
      '#otherParties > div > h2',
      addOtherPartyData.otherPartiesHeading
    );
    await webActions.verifyPageLabel(
      '#awareOfAnyAdditionalOtherParties span',
      addOtherPartyData.areYouAwareOtherPartiesQuestion
    );
    await webActions.verifyPageLabel(
      'label[for=awareOfAnyAdditionalOtherParties_Yes]',
      addOtherPartyData.areYouAwareOtherPartiesQuestionYesLabel
    );
    await webActions.verifyPageLabel(
      'label[for=awareOfAnyAdditionalOtherParties_No]',
      addOtherPartyData.areYouAwareOtherPartiesQuestionNoLabel
    );
    await webActions.verifyElementVisibility(
      '#awareOfAnyAdditionalOtherParties_Yes'
    );
    await webActions.verifyElementVisibility(
      '#awareOfAnyAdditionalOtherParties_No'
    );
  }

  async addOtherPartyData() {
    await webActions.clickButton('Add new');

    const otherPartyFormData: Record<string, string> = {
      name_title: addOtherPartyData.otherPartyDataTable.Title,
      name_firstName: addOtherPartyData.otherPartyDataTable.FirstName,
      name_lastName: addOtherPartyData.otherPartyDataTable.LastName,
      address_line1: addOtherPartyData.otherPartyDataTable.AddressLine1,
      address_line2: addOtherPartyData.otherPartyDataTable.AddressLine2,
      address_town: addOtherPartyData.otherPartyDataTable.Town,
      address_county: addOtherPartyData.otherPartyDataTable.County,
      address_postcode: addOtherPartyData.otherPartyDataTable.Postcode,
      contact_phone: addOtherPartyData.otherPartyDataTable.ContactNumber,
      contact_mobile: addOtherPartyData.otherPartyDataTable.MobileNumber,
      contact_email: addOtherPartyData.otherPartyDataTable.ContactEmail
    };

    for (const [selectorSuffix, value] of Object.entries(otherPartyFormData)) {
      await webActions.inputField(`#otherParties_0_${selectorSuffix}`, value);
    }

    await webActions.clickElementById(
      `#otherParties_0_unacceptableCustomerBehaviour_No`
    );
    await webActions.chooseOptionByLabel(
      '#otherParties_0_role_name',
      addOtherPartyData.otherPartyDataTable.Role
    );
    await webActions.inputField(
      '#otherParties_0_role_description',
      addOtherPartyData.otherPartyDataTable.RoleDescription
    );
    await webActions.clickElementById('#otherParties_0_domesticViolenceMarker-No');
    await webActions.clickElementById('#awareOfAnyAdditionalOtherParties_No');
    await webActions.clickSubmitButton();
    await webActions.waitForSpinnerToDisappear();
    
  }
}
