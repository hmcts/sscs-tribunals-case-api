import { expect, Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import addUpdateOtherPartyData from "./content/update.other.party.data_en.json";
import addUpdateSubscriptionData from "./content/update.subscription.data_en.json"


let webAction: WebAction;

export class updateOtherPartyDataPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webAction.verifyPageLabel('.govuk-caption-l', addUpdateOtherPartyData.updateOtherPartyDataHeading); //Above heading Text
        await webAction.isLinkClickable('Cancel');
    }

    // Applying other party data for the Mandatory fields only
    async applyOtherPartyData() {
        await this.page.getByRole('button', { name: 'Add new' }).click(); //fields are expanded here
        await this.page.getByText("First Name");
        await this.page.locator('#otherParties_0_name_firstName').fill(addUpdateOtherPartyData.updateOtherPartyDataFirstName);
        await this.page.locator('#otherParties_0_name_lastName').fill(addUpdateOtherPartyData.updateOtherPartyDataLastName);
        await this.page.locator('#otherParties_0_address_line1').fill(addUpdateOtherPartyData.updateOtherPartyDataAddressLine);
        await this.page.locator('#otherParties_0_address_town').fill(addUpdateOtherPartyData.updateOtherPartyDataAddressTown);
        await this.page.locator('#otherParties_0_address_postcode').fill(addUpdateOtherPartyData.updateOtherPartyDataAddressPostCode);
        await this.page.locator('#otherParties_0_confidentialityRequired_No').click();
        await this.page.locator('#otherParties_0_unacceptableCustomerBehaviour_No').click();
        await this.page.locator('#otherParties_0_role_name').selectOption({ label: 'Paying parent' }); //selecting role drop down

        await webAction.clickButton("Submit");
    }

    async applyOtherPartyDataTaxCredit() {
        await this.page.getByRole('button', { name: 'Add new' }).click(); //fields are expanded here
        await this.page.getByText("First Name");
        await this.page.locator('#otherParties_0_name_firstName').fill(addUpdateOtherPartyData.updateOtherPartyDataFirstName);
        await this.page.locator('#otherParties_0_name_lastName').fill(addUpdateOtherPartyData.updateOtherPartyDataLastName);
        await this.page.locator('#otherParties_0_address_line1').fill(addUpdateOtherPartyData.updateOtherPartyDataAddressLine);
        await this.page.locator('#otherParties_0_address_town').fill(addUpdateOtherPartyData.updateOtherPartyDataAddressTown);
        await this.page.locator('#otherParties_0_address_postcode').fill(addUpdateOtherPartyData.updateOtherPartyDataAddressPostCode);
        await this.page.locator('#otherParties_0_confidentialityRequired_No').click();
        await this.page.locator('#otherParties_0_unacceptableCustomerBehaviour_No').click();
        // Role is NOT a Mandatory field for Tax Credit cases // await this.page.locator('#otherParties_0_role_name').selectOption({ label: 'Paying parent' });

        await webAction.clickButton("Submit");
    }

    // Applying Other parties subscriptions
    async applyOtherPartiesSubscription() {
        await this.page.locator('#otherParties_0_otherPartySubscription_wantSmsNotifications_Yes').click();
        await this.page.locator('#otherParties_0_otherPartySubscription_tya').fill(addUpdateSubscriptionData.updateSubscriptionTrackYAotherParty);
        await this.page.locator('#otherParties_0_otherPartySubscription_email').fill(addUpdateSubscriptionData.updateSubscriptionEmailotherParty);
        await this.page.locator('#otherParties_0_otherPartySubscription_mobile').fill(addUpdateSubscriptionData.updateSubscriptionMobileNumberotherParty);
        await this.page.locator('#otherParties_0_otherPartySubscription_subscribeEmail_Yes').click();
        await this.page.locator('#otherParties_0_otherPartySubscription_subscribeSms_Yes').click();

        await webAction.clickButton("Submit");
    }

    async cancelEvent(): Promise<void> {
        await webAction.clickLink("Cancel");
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickSubmitButton();
    }
}