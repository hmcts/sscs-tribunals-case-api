import { expect, Page } from '@playwright/test';
import { WebAction } from '../../common/web.action'
import { HomePage } from '../common/homePage';
import addUpdateSubscriptionData from "../content/update.subscription.data_en.json";


let webActions: WebAction;

export class Subscriptions {

    readonly page: Page;
    protected homePage: HomePage;

    constructor(page: Page) {
        this.page = page;
        this.homePage = new HomePage(this.page);
        webActions = new WebAction(this.page);
    }
    
    async verifyPageContentByKeyValue(fieldLabel: string, fieldValue: string) {
        // await expect(this.page
        //    .locator(`//*[normalize-space()="${fieldLabel}"]/../..//td[normalize-space()="${fieldValue}"]`)).toBeVisible();
        //Appellant
        await expect(this.page
            .locator(`//*[normalize-space()="Track Your Appeal Number"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionTrackYAappellant}"]`)).toBeVisible();
        await expect(this.page
            .locator(`//*[normalize-space()="Email Address"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionEmailappellant}"]`)).toBeVisible();
        await expect(this.page
            .locator(`//*[normalize-space()="Mobile Number"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionMobileNumberappellant}"]`)).toBeVisible();
        //Representative
        await expect(this.page
            .locator(`//*[normalize-space()="Track Your Appeal Number"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionTrackYArepresentative}"]`)).toBeVisible();
        await expect(this.page
            .locator(`//*[normalize-space()="Email Address"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionEmailrepresentative}"]`)).toBeVisible();
        await expect(this.page
            .locator(`//*[normalize-space()="Mobile Number"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionMobileNumberrepresentative}"]`)).toBeVisible();
        //Appointee
        await expect(this.page
            .locator(`//*[normalize-space()="Track Your Appeal Number"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionTrackYAappointee}"]`)).toBeVisible();
        await expect(this.page
            .locator(`//*[normalize-space()="Email Address"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionEmailappointee}"]`)).toBeVisible();
        await expect(this.page
            .locator(`//*[normalize-space()="Mobile Number"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionMobileNumberappointee}"]`)).toBeVisible();
        //Joint Party
        await expect(this.page
            .locator(`//*[normalize-space()="Track Your Appeal Number"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionTrackYAjointParty}"]`)).toBeVisible();
        await expect(this.page
            .locator(`//*[normalize-space()="Email Address"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionEmailjointParty}"]`)).toBeVisible();
        await expect(this.page
            .locator(`//*[normalize-space()="Mobile Number"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionMobileNumberjointParty}"]`)).toBeVisible();
        //Supporter
        await expect(this.page
            .locator(`//*[normalize-space()="Track Your Appeal Number"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionTrackYAsupportParty}"]`)).toBeVisible();
        await expect(this.page
            .locator(`//*[normalize-space()="Email Address"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionEmailsupportParty}"]`)).toBeVisible();
        await expect(this.page
            .locator(`//*[normalize-space()="Mobile Number"]/../..//td[normalize-space()="${addUpdateSubscriptionData.updateSubscriptionMobileNumbersupportParty}"]`)).toBeVisible();
    }
}