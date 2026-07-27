import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';

let webActions: WebAction;

export class CommunicateWithTribunalPage {
    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async replyToTribunalQuery(replyRequired = true) {
        await webActions.clickRadioButton('Reply to Tribunal Query');
        await webActions.clickButton('Continue');
        await webActions.chooseOptionByIndex('#tribunalRequestsDl', 1);
        await webActions.clickButton('Continue');
        await (replyRequired
            ? webActions.inputField('#commRequestResponseTextArea', 'Test details for Tribunal communication Reply')
            : webActions.checkAnCheckBox('No reply required'));
        await webActions.clickButton('Continue');
        await webActions.clickSubmitButton();
    }

    async replyToFTAQuery(replyRequired = true) {
        await webActions.clickRadioButton('Reply to FTA Query');
        await webActions.clickButton('Continue');
        await webActions.chooseOptionByIndex('#ftaRequestsDl', 1);
        await webActions.clickButton('Continue');
        await (replyRequired
            ? webActions.inputField('#commRequestResponseTextArea', 'Test details for FTA communication Reply')
            : webActions.checkAnCheckBox('No reply required'));
        await webActions.clickButton('Continue');
        await webActions.clickSubmitButton();
    }
}