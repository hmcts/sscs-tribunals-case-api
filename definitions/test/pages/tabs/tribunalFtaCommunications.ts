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

    async verifyReplyExists() {
        const reply = "//span[text()='Reply']//ancestor::dl/.."
        await webActions.verifyElementVisibility(reply);
        
    }

    async verifyReplyHasBeenReviewed() {
        const replyReviewed = "//span[contains(text(),'reviewed')]/../following-sibling::td/span";
        await webActions.verifyElementVisibility(replyReviewed);
        await webActions.verifyPageLabel(replyReviewed, "Yes");
    }

}