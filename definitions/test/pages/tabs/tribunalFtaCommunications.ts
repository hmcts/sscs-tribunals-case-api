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

    async verifyRequestFromFTAExists() {
        const requestFromTribunal = await this.page.locator("//span[text()='Requests from FTA 1']//ancestor::dl/..");
        await expect(requestFromTribunal).toBeVisible();
    }

    async verifyReplyExists() {
        const replyFromFta = await this.page.locator("//span[text()='Reply']//ancestor::dl/..");
    }

    async verifyReplyHasBeenReviewed(tribsVerifyReply: boolean) {
        let replyReviewed;
        if(tribsVerifyReply) {
            replyReviewed = "//*[normalize-space()='Has the reply been reviewed by the Tribunal?']/..//span//span[normalize-space()='Yes']";
        } else {
            replyReviewed = "//*[normalize-space()='Has the reply been reviewed by the FTA?']/..//span//span[normalize-space()='Yes']";
        }
        
        await expect(this.page.locator(replyReviewed)).toBeVisible();
        await webActions.verifyPageLabel(replyReviewed, "Yes");
    }

}