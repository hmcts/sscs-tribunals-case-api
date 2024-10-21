import {Page} from '@playwright/test';
import {WebAction} from '../common/web.action';
import addNoteTestData from "./content/add.note_en.json";


let webActions: WebAction;

export class AddNote {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webActions.verifyPageLabel('.govuk-caption-l', 'Add a note'); //Captor Text
        //await webActions.verifyPageLabel('h1', casereference+": Bloggs"); //Captor Text
        await webActions.verifyPageLabel('.govuk-heading-l', 'Add a case note'); //Heading Text
        await webActions.verifyPageLabel('.form-label', 'Enter note'); //Field Label
    }

    async inputData(): Promise<void> {
        await webActions.inputField('#tempNoteDetail', addNoteTestData.noteSummaryValue);
    }

    async confirmSubmission(): Promise<void> {
        await webActions.clickButton('Submit');
    }

}
