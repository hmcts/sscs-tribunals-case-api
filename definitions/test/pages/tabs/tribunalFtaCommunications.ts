import { expect, Page } from '@playwright/test';
import { WebAction } from '../../common/web.action';

let webActions: WebAction;

export class TribunalFtaCommunications {
    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyRequestFromTribunalExists() {
        const requestFromTribunal = await this.page.locator("//span[text()='Requests from Tribunal 1']//ancestor::dl/..");
        await expect(requestFromTribunal).toBeVisible();
    }

    async verifyReplyFromFtaExists() {
        const replyFromFta = await this.page.locator("//span[text()='Reply']//ancestor::dl/..");
    }

    async verifyReplyHasBeenReviewed() {
        const replyReviewed = "//span[text()='reply has been reviewed']//ancestor::dl/..";
        await expect(this.page.locator(replyReviewed)).toBeVisible();
        await webActions.verifyPageLabel(replyReviewed, "Yes");
    }

}