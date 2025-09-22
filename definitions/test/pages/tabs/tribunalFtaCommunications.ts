import { Page } from '@playwright/test';
import { WebAction } from '../../common/web.action';

let webActions: WebAction;

export class TribunalFtaCommunications {
    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyRequestFromTribunalExists() {
        const requestFromTribunal = "//span[text()='Requests from Tribunal 1']//ancestor::dl/..";
        await webActions.verifyElementVisibility(requestFromTribunal);
    }

    async verifyRequestFromFTAExists() {
        const requestFromFTA = "//span[text()='Requests from FTA 1']//ancestor::dl/..";
        await webActions.verifyElementVisibility(requestFromFTA);
    }

    async verifyReplyExists() {
        const reply = "//span[text()='Reply']//ancestor::dl/.."
        await webActions.verifyElementVisibility(reply);
    }

    async verifyReplyHasBeenReviewed(tribsVerifyReply: boolean) {
        let replyReviewed;
        if(tribsVerifyReply) {
            replyReviewed = "//*[normalize-space()='Has the reply been reviewed by the Tribunal?']/..//span//span[normalize-space()='Yes']";
        } else {
            replyReviewed = "//*[normalize-space()='Has the reply been reviewed by the FTA?']/..//span//span[normalize-space()='Yes']";
        }
        
        await webActions.verifyElementVisibility(replyReviewed);
    }
}