import { expect, Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import addUpdateOtherPartyData from './content/update.other.party.data_en.json';
import addUpdateSubscriptionData from './content/update.subscription.data_en.json';

let webAction: WebAction;

export class updateOtherPartyDataPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    webAction = new WebAction(this.page);
  }

  async verifyPageContent(
    heading: string = addUpdateOtherPartyData.updateOtherPartyDataHeading
  ) {
    const captionCount = await this.page.locator('.govuk-caption-l').count();

    if (captionCount > 0) {
      await webAction.verifyPageLabel(
        '.govuk-caption-l',
        heading
      );
    } else {
      await expect(
        this.page.getByRole('heading', { name: heading, exact: true }).first()
      ).toBeVisible();
    }

    await webAction.isButtonClickable('Cancel');
  }

  // Applying other party data for the Mandatory fields only
  async applyOtherPartyData(
    caseType: string = "NONIBC",
    confidentialityRequired: string = addUpdateOtherPartyData.updateOtherPartyDataConfidentiality
  ) {
    await this.page.getByRole('button', { name: 'Add new' }).click(); //fields are expanded here
    await this.page.getByText('First Name');
    await this.page
      .locator('#otherParties_0_name_firstName')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataFirstName);
    await this.page
      .locator('#otherParties_0_name_lastName')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataLastName);
    await this.page
      .locator('#otherParties_0_address_line1')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataAddressLine);
    await this.page
      .locator('#otherParties_0_address_town')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataAddressTown);
    if( caseType != "NONIBC") {
      await this.page
        .locator('#otherParties_0_address_country')
        .fill(addUpdateOtherPartyData.updateOtherPartyDataAddressCountry);
    }
    await this.page
      .locator('#otherParties_0_address_postcode')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataAddressPostCode);      
    await this.page
      .locator(
        confidentialityRequired ===
          addUpdateOtherPartyData.updateOtherPartyDataConfidentialityYes
          ? '#otherParties_0_confidentialityRequired_Yes'
          : '#otherParties_0_confidentialityRequired_No'
      )
      .click();
    await this.page
      .locator('#otherParties_0_unacceptableCustomerBehaviour_No')
      .click();
      if( caseType == "NONIBC") {
        await this.page
          .locator('#otherParties_0_role_name')
          .selectOption({ label: 'Paying parent' }); //selecting role drop down
      } else if( caseType == "IBC" ) {
        await this.page
          .locator('#otherParties_0_name_title')
          .fill(addUpdateOtherPartyData.updateOtherPartyDataTitle);
        await this.page
          .locator('#otherParties_0_address_inMainlandUk_Yes')
          .click(); //ToDo : Change to dynamic value
      }
    await webAction.clickButton('Submit');
  }

  async applyChildSupportConfidentialOtherPartyData() {
    await this.page.getByRole('button', { name: 'Add new' }).click();
    await this.page.getByText('First Name');
    await this.page
      .locator('#otherParties_0_name_firstName')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataFirstName);
    await this.page
      .locator('#otherParties_0_name_lastName')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataLastName);
    await this.page
      .locator('#otherParties_0_address_line1')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataAddressLine);
    await this.page
      .locator('#otherParties_0_address_town')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataAddressTown);
    await this.page
      .locator('#otherParties_0_address_postcode')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataAddressPostCode);
    await this.page.locator('#otherParties_0_confidentialityRequired_Yes').click();
    await this.page
      .locator('#otherParties_0_unacceptableCustomerBehaviour_No')
      .click();
    await this.page.locator('#otherParties_0_role_name').selectOption({ label: 'Paying parent' });
    await this.page
      .getByRole('group', { name: /Wants To Attend/i })
      .getByLabel('Yes')
      .check();
    await this.page
      .getByRole('group', { name: /Wants Support/i })
      .getByLabel('No')
      .check();
    await this.page
      .getByRole('group', { name: /Unavailable dates/i })
      .getByLabel('No')
      .check();
    await this.page
      .getByRole('group', { name: /Agree less notice/i })
      .getByLabel('Yes')
      .check();

    await webAction.clickButton('Submit');
  }

  async applyChildSupportFtaAddOtherPartyData() {
    await this.applyChildSupportFtaAddOtherPartyDataWithValues();
  }

  async applyChildSupportFtaAddOtherPartyDataWithValues(values = {
    title: addUpdateOtherPartyData.updateOtherPartyDataTitle,
    firstName: addUpdateOtherPartyData.updateOtherPartyDataFirstName,
    lastName: addUpdateOtherPartyData.updateOtherPartyDataLastName,
    addressLine1: addUpdateOtherPartyData.updateOtherPartyDataAddressLine,
    town: addUpdateOtherPartyData.updateOtherPartyDataAddressTown,
    postcode: addUpdateOtherPartyData.updateOtherPartyDataAddressPostCode
  }) {
    await this.page.getByRole('button', { name: 'Add new' }).click();
    await this.page.locator('#otherParties_0_name_title').fill(values.title);
    await this.page
      .locator('#otherParties_0_name_firstName')
      .fill(values.firstName);
    await this.page
      .locator('#otherParties_0_name_lastName')
      .fill(values.lastName);
    await this.page
      .locator('#otherParties_0_address_line1')
      .fill(values.addressLine1);
    await this.page
      .locator('#otherParties_0_address_town')
      .fill(values.town);
    await this.page
      .locator('#otherParties_0_address_postcode')
      .fill(values.postcode);
    await this.page
      .locator('#otherParties_0_unacceptableCustomerBehaviour_No')
      .click();
    await this.page
      .locator('#otherParties_0_role_name')
      .selectOption({ label: addUpdateOtherPartyData.updateOtherPartyDataDropdownValue });
    await this.page.locator('#awareOfAnyAdditionalOtherParties_No').click();

    await webAction.clickButton('Submit');
  }

  async applyOtherPartyDataTaxCredit() {
    await this.page.getByRole('button', { name: 'Add new' }).click(); //fields are expanded here
    await this.page.getByText('First Name');
    await this.page
      .locator('#otherParties_0_name_firstName')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataFirstName);
    await this.page
      .locator('#otherParties_0_name_lastName')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataLastName);
    await this.page
      .locator('#otherParties_0_address_line1')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataAddressLine);
    await this.page
      .locator('#otherParties_0_address_town')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataAddressTown);
    await this.page
      .locator('#otherParties_0_address_postcode')
      .fill(addUpdateOtherPartyData.updateOtherPartyDataAddressPostCode);
    await this.page
      .locator('#otherParties_0_confidentialityRequired_No')
      .click();
    await this.page
      .locator('#otherParties_0_unacceptableCustomerBehaviour_No')
      .click();
    // Role is NOT a Mandatory field for Tax Credit cases // await this.page.locator('#otherParties_0_role_name').selectOption({ label: 'Paying parent' });

    await webAction.clickButton('Submit');
  }

  // Applying Other parties subscriptions
  async applyOtherPartiesSubscription() {
    await this.page
      .locator(
        '#otherParties_0_otherPartySubscription_wantSmsNotifications_Yes'
      )
      .click();
    await this.page
      .locator('#otherParties_0_otherPartySubscription_tya')
      .fill(addUpdateSubscriptionData.updateSubscriptionTrackYAotherParty);
    await this.page
      .locator('#otherParties_0_otherPartySubscription_email')
      .fill(addUpdateSubscriptionData.updateSubscriptionEmailotherParty);
    await this.page
      .locator('#otherParties_0_otherPartySubscription_mobile')
      .fill(addUpdateSubscriptionData.updateSubscriptionMobileNumberotherParty);
    await this.page
      .locator('#otherParties_0_otherPartySubscription_subscribeEmail_Yes')
      .click();
    await this.page
      .locator('#otherParties_0_otherPartySubscription_subscribeSms_Yes')
      .click();

    await webAction.clickButton('Submit');
  }

  async cancelEvent(): Promise<void> {
    await webAction.clickLink('Cancel');
  }

  async confirmSubmission(): Promise<void> {
    await webAction.clickSubmitButton();
  }
}
