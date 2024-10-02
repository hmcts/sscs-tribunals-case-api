import { Page } from '@playwright/test';
import eventTestData from "../../pages/content/event.name.event.description_en.json";
import appointeeDetailsData from "../../pages/content/appointee.details_en.json";
import { SendToJudgePage } from '../../pages/send.to.judge.page';
import { BaseStep } from './base';
import {credentials} from "../../config/config";
import {DeathOfAppellantPage} from "../../pages/death.of.appelant";
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../../api/client/sscs/appeal.event";

export class DeathOfAnAppelant extends BaseStep {

    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performDeathOfAnAppellantWithoutAnApointee() {

        let caseId = await createCaseBasedOnCaseType('PIP');
        await this.loginUserWithCaseId(credentials.dwpResponseWriter, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Death of appellant');

        let deathOfAppellantPage = new DeathOfAppellantPage(this.page);
        await deathOfAppellantPage.verifyPageContent();
        await deathOfAppellantPage.populateDeathOfAppellantPageData('No');
        await deathOfAppellantPage.confirmSubmission();

        //Params are passed to this page as this is a common page to be reused.
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        //Navigate to History Tab and Verify event is listed
        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('Date of appellant death','1 Jun 2003');
        await  deathOfAppellantPage.signOut();

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.verifyHistoryTabLink('Death of appellant');
        await performAppealDormantOnCase(caseId);
    }

    async performDeathOfAnAppellantWithAnAppointee() {

        let caseId = await createCaseBasedOnCaseType('UC');
        await this.loginUserWithCaseId(credentials.dwpResponseWriter, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Death of appellant');

        let deathOfAppellantPage = new DeathOfAppellantPage(this.page);
        await deathOfAppellantPage.verifyPageContent();
        await deathOfAppellantPage.populateDeathOfAppellantPageData('Yes');
        await deathOfAppellantPage.confirmSubmission();

        //Params are passed to this page as this is a common page to be reused.
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('Date of appellant death','1 Jun 2003');
        await this.appealDetailsTab.verifyAppealDetailsAppointeeDetails(appointeeDetailsData);
        await  deathOfAppellantPage.signOut();

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.verifyHistoryTabLink('Death of appellant');
        await performAppealDormantOnCase(caseId);
    }

    async performDeathOfAnAppellantNotValidErrorScenarios() {
        let caseId = await createCaseBasedOnCaseType('ESA');
        await this.loginUserWithCaseId(credentials.dwpResponseWriter, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Death of appellant');

        let deathOfAppellantPage = new DeathOfAppellantPage(this.page);
        await deathOfAppellantPage.populateDeathOfAppellantDateInvalidFormat('No');
        await deathOfAppellantPage.confirmSubmission();
        await deathOfAppellantPage.verifyDeathDateNotValidErrorMsg();
        //await deathOfAppellantPage.reloadPage();
       /* await deathOfAppellantPage.populateDeathOfAppellantDateInTheFuture('No');
        await deathOfAppellantPage.confirmSubmission();
        //The Error is shown in the next page
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await deathOfAppellantPage.verifyDeathDateNotValidErrorMsg();*/
        await  deathOfAppellantPage.signOut();
        await performAppealDormantOnCase(caseId);
    }

    async performDeathOfAnAppellantFutureDateErrorScenarios() {
        let caseId = await createCaseBasedOnCaseType('ESA');
        await this.loginUserWithCaseId(credentials.dwpResponseWriter, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent('Death of appellant');

        let deathOfAppellantPage = new DeathOfAppellantPage(this.page);
        await deathOfAppellantPage.populateDeathOfAppellantDateInTheFuture('No');
        await deathOfAppellantPage.confirmSubmission();
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await deathOfAppellantPage.verifyDeathDateNotBeIntheFutureErrorMsg();
        await  deathOfAppellantPage.signOut();
        await performAppealDormantOnCase(caseId);
    }
}
