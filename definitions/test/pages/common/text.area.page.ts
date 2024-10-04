import { Page } from '@playwright/test';
import { WebAction } from '../../common/web.action';

let webActions: WebAction;

export class TextAreaPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContent(captionValue: string, headingValue:string, textAreaLabel: string) {

        //.govuk-caption-l
        await webActions.verifyPageLabel('.govuk-caption-l', captionValue); //Caption Text
        await webActions.verifyPageLabel('.govuk-heading-l', headingValue); //Heading Text
        await webActions.verifyPageLabel('.form-label', textAreaLabel); //Field Label
    }

    async inputData(enterNoteText:string): Promise<void> {
        await webActions.inputField('#tempNoteDetail', enterNoteText);
    }

    async confirmSubmission(): Promise<void> {
        await webActions.clickButton('Submit');
    }

}
