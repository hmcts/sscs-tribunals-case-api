import { expect, Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import addUpdateSubscriptionData from "./content/update.subscription.data_en.json";


let webAction: WebAction;

export class UpdateSubscriptionPage {  //updated class

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webAction.verifyPageLabel('.govuk-caption-l', addUpdateSubscriptionData.updateSubscriptionHeading); //Above heading Text
        await webAction.isLinkClickable('Cancel');
}

    // Applying subscription Yes to email + sms
    async applySubscription() {
        // Appellant
        await this.page.locator('#subscriptions_appellantSubscription_wantSmsNotifications_Yes').click();
        await this.page.locator('#subscriptions_appellantSubscription_tya').fill(addUpdateSubscriptionData.updateSubscriptionTrackYAappellant);
        await this.page.locator('#subscriptions_appellantSubscription_email').fill(addUpdateSubscriptionData.updateSubscriptionEmailappellant);
        await this.page.locator('#subscriptions_appellantSubscription_mobile').fill(addUpdateSubscriptionData.updateSubscriptionMobileNumberappellant);
        await this.page.locator('#subscriptions_appellantSubscription_subscribeEmail_Yes').click();
        await this.page.locator('#subscriptions_appellantSubscription_subscribeSms_Yes').click();
        // Representative
        await this.page.locator('#subscriptions_representativeSubscription_wantSmsNotifications_Yes').click();
        await this.page.locator('#subscriptions_representativeSubscription_tya').fill(addUpdateSubscriptionData.updateSubscriptionTrackYArepresentative);
        await this.page.locator('#subscriptions_representativeSubscription_email').fill(addUpdateSubscriptionData.updateSubscriptionEmailrepresentative);
        await this.page.locator('#subscriptions_representativeSubscription_mobile').fill(addUpdateSubscriptionData.updateSubscriptionMobileNumberrepresentative);
        await this.page.locator('#subscriptions_representativeSubscription_subscribeEmail_Yes').click();
        await this.page.locator('#subscriptions_representativeSubscription_subscribeSms_Yes').click();
        // Appointee
        await this.page.locator('#subscriptions_appointeeSubscription_wantSmsNotifications_Yes').click();
        await this.page.locator('#subscriptions_appointeeSubscription_tya').fill(addUpdateSubscriptionData.updateSubscriptionTrackYAappointee);
        await this.page.locator('#subscriptions_appointeeSubscription_email').fill(addUpdateSubscriptionData.updateSubscriptionEmailappointee);
        await this.page.locator('#subscriptions_appointeeSubscription_mobile').fill(addUpdateSubscriptionData.updateSubscriptionMobileNumberappointee);
        await this.page.locator('#subscriptions_appointeeSubscription_subscribeEmail_Yes').click();
        await this.page.locator('#subscriptions_appointeeSubscription_subscribeSms_Yes').click();
        // Joint Party
        await this.page.locator('#subscriptions_jointPartySubscription_wantSmsNotifications_Yes').click();
        await this.page.locator('#subscriptions_jointPartySubscription_tya').fill(addUpdateSubscriptionData.updateSubscriptionTrackYAjointParty);
        await this.page.locator('#subscriptions_jointPartySubscription_email').fill(addUpdateSubscriptionData.updateSubscriptionEmailjointParty);
        await this.page.locator('#subscriptions_jointPartySubscription_mobile').fill(addUpdateSubscriptionData.updateSubscriptionMobileNumberjointParty);
        await this.page.locator('#subscriptions_jointPartySubscription_subscribeEmail_Yes').click();
        await this.page.locator('#subscriptions_jointPartySubscription_subscribeSms_Yes').click();
        // Supporter
        await this.page.locator('#subscriptions_supporterSubscription_wantSmsNotifications_Yes').click();
        await this.page.locator('#subscriptions_supporterSubscription_tya').fill(addUpdateSubscriptionData.updateSubscriptionTrackYAsupportParty);
        await this.page.locator('#subscriptions_supporterSubscription_email').fill(addUpdateSubscriptionData.updateSubscriptionEmailsupportParty);
        await this.page.locator('#subscriptions_supporterSubscription_mobile').fill(addUpdateSubscriptionData.updateSubscriptionMobileNumbersupportParty);
        await this.page.locator('#subscriptions_supporterSubscription_subscribeEmail_Yes').click();
        await this.page.locator('#subscriptions_supporterSubscription_subscribeSms_Yes').click();

        await webAction.clickButton("Submit");
    }

    async cancelEvent(): Promise<void> {
        await webAction.clickLink("Cancel");
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickSubmitButton();
    }
}