import {expect, Page} from '@playwright/test';
import {WebAction} from '../../common/web.action'


let webActions: WebAction;

export class HearingRecordings {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContentByKeyValue(fieldLabel: string, fieldValue: string) {
        await expect(this.page
            .locator(`//*[normalize-space()="${fieldLabel}"]/../..//td[normalize-space()="${fieldValue}"]`)).toBeVisible();
    }
}
