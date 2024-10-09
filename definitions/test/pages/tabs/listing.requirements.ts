import {expect, Page} from '@playwright/test';
import {WebAction} from '../../common/web.action';


let webActions: WebAction;

export class ListingRequirements {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }


    async verifyContentByKeyValueForASpan(fieldLabel: string, fieldValue: string): Promise<void> {
        await expect(this.page
            .locator(`//*[normalize-space()="${fieldLabel}"]/..//span//span[normalize-space()="${fieldValue}"]`).first()).toBeVisible();
    }

    async verifyContentNotPresent(fieldLabel: string, fieldValue: string): Promise<void> {
        await expect(this.page
            .locator(`//*[normalize-space()="${fieldLabel}"]/..//span//span[normalize-space()="${fieldValue}"]`).first()).not.toBeVisible();
    }
}
