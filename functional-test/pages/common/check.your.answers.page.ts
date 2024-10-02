import { Page } from '@playwright/test';
import { WebAction } from '../../common/web.action';
const uploadResponseTestdata = require('../content/upload.response_en.json');
const eventTestData = require("../content/event.name.event.description_en.json");

let webActions: WebAction;

export class CheckYourAnswersPage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyCYAPageContent(headingValue: string, benefitCode?: string, issueCode?: string, caseType?: string) {

        await webActions.verifyPageLabel('.govuk-heading-l', headingValue); //Heading Text
        await webActions.verifyPageLabel('form.check-your-answers h2.heading-h2', eventTestData.eventSummarycheckYourAnswersHeading);//Check your answers Text.
        await webActions.verifyPageLabel('//self::ccd-read-document-field/a', 
            [uploadResponseTestdata.testfileone, uploadResponseTestdata.testfiletwo, uploadResponseTestdata.testfilethree]);
        if(caseType === 'UC'){
            await webActions.verifyPageLabel('//self::ccd-read-yes-no-field/span', 
               [uploadResponseTestdata.poAttendOption, uploadResponseTestdata.ucDisputeOption, uploadResponseTestdata.ucJointPartyOnCase]);
        } else {
            await webActions.verifyPageLabel('//self::ccd-read-fixed-list-field/span',
            [benefitCode, issueCode]);   
            await webActions.verifyPageLabel('//self::ccd-read-yes-no-field/span', [uploadResponseTestdata.poAttendOption]);
        }
    }

    async verifyCYAPageContentWithPHE(headingValue: string, benefitCode?: string, issueCode?: string) {

        await webActions.verifyPageLabel('.govuk-heading-l', headingValue); //Heading Text
        await webActions.verifyPageLabel('form.check-your-answers h2.heading-h2', eventTestData.eventSummarycheckYourAnswersHeading);//Check your answers Text.
        await webActions.verifyPageLabel('//self::ccd-read-fixed-list-field/span',
            ["Potentially harmful evidence", benefitCode, issueCode]);   
        await webActions.verifyPageLabel('//self::ccd-read-yes-no-field/span', [uploadResponseTestdata.poAttendOption]);
    }

    async verifyCYAPageContentWithUCB(headingValue: string, benefitCode?: string, issueCode?: string) {

        await webActions.verifyPageLabel('.govuk-heading-l', headingValue); //Heading Text
        await webActions.verifyPageLabel('form.check-your-answers h2.heading-h2', eventTestData.eventSummarycheckYourAnswersHeading);//Check your answers Text.
        await webActions.verifyPageLabel('//self::ccd-read-fixed-list-field/span',
            [benefitCode, issueCode]);   
        await webActions.verifyPageLabel('//self::ccd-read-yes-no-field/span', ["Yes", uploadResponseTestdata.poAttendOption]);
    }

    async confirmSubmission(): Promise<void> {
        await webActions.clickButton('Submit');
    }

    async verifyPHMEErrorMsg(): Promise<void>{
        await webActions.verifyElementVisibility('#errors');
        await webActions.verifyTextVisibility('You must upload an edited FTA response document');
        await webActions.verifyTextVisibility('You must upload an edited FTA evidence bundle');
    }

    async verifyIssueCodeErrorMsg(): Promise<void>{
        await webActions.verifyElementVisibility('#errors');
        await webActions.verifyTextVisibility('Issue code cannot be set to the default value of DD');
    }
}