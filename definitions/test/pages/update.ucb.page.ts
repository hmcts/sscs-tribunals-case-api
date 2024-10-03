import {Page} from '@playwright/test';
import {WebAction} from '../common/web.action';
import addNoteTestData from "./content/add.note_en.json";


let webActions: WebAction;

export class UpdateUCBPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webActions.verifyPageLabel('.govuk-caption-l', 'Update UCB flag'); //Captor Text
    }

    async updateUCBToNo() {
        await webActions.clickElementById('#dwpUCB_No');
        await webActions.clickSubmitButton();
    }

    async confirmSubmission(): Promise<void> {
        await webActions.clickButton('Submit');
    }

}
