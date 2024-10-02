import { expect, Page } from '@playwright/test';


export class Welsh {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
    }

    async verifyPageContentByKeyValue(fieldLabel: string, fieldValue: string) {
        await expect(this.page
            .locator(`//th[normalize-space()="${fieldLabel}"]/../td[normalize-space()="${fieldValue}"]`)).toBeVisible();
    }
}
