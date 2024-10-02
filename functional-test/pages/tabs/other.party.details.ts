import { expect, Page } from '@playwright/test';
import { WebAction } from '../../common/web.action'
import { HomePage } from '../common/homePage';

let webActions: WebAction;

export class OtherPartyDetails {

    readonly page: Page;
    protected homePage: HomePage;

    constructor(page: Page) {
        this.page = page;
        this.homePage = new HomePage(this.page);
        webActions = new WebAction(this.page);
    }

    async verifyPageContentByKeyValue(fieldLabel: string, fieldValue: string) {
        await expect(this.page
            .locator(`//*[normalize-space()="${fieldLabel}"]/../..//td[normalize-space()="${fieldValue}"]`)).toBeVisible();
   }
}