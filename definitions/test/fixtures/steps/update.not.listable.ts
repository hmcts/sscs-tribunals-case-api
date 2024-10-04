import {BaseStep} from "./base";
import {Page} from "@playwright/test";
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import {credentials} from "../../config/config";


export class UpdateNotListable extends BaseStep {

    readonly page: Page;


    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performNotListableEvent() {
        let pipCaseId = await createCaseBasedOnCaseType('PIP');
        //Trigger Not listable event:
        await this.goToNotListablePage(this.page, pipCaseId);
        await this.notListablePage.verifyPageContent(); //Verifying Heading and Caption for event
        //inserting data and verifying said data during the event
        await this.notListablePage.enterNotListableProvideReason();
        await this.notListablePage.continueEvent();

        await this.notListablePage.enterValidDirectionDueDate();
        await this.notListablePage.continueEvent();

        await this.notListablePage.confirmSubmission();
       // verifying that event has submitted successfully and details are showing in Summary and History Tabs
       await this.verifyHistoryTabDetails("Not listable","Not listable")
    }

   //Method to verify the error messaging for Not listable.
    async verifyNotListableErrorMessages(){
        let pipCaseId = await createCaseBasedOnCaseType('PIP');
        await this.goToNotListablePage(this.page, pipCaseId);
        await this.notListablePage.verifyPageContent();
        await this.notListablePage.continueEvent();
        await this.notListablePage.verifyNotListableReasonError();

        await this.notListablePage.enterNotListableProvideReason();
        await this.notListablePage.continueEvent();

        await this.notListablePage.enterInvalidDirectionDueDate();
        await this.updateNotListablePage.continueEvent();
        await this.notListablePage.verifyPastDueDateErrorMessage();
    }

    async performUpdateNotListableDirectionFulfilled(){
        await this.performNotListableEvent();
        await this.homePage.chooseEvent("Update not listable case");
        await this.updateNotListablePage.verifyPageContent();
        await this.updateNotListablePage.requirementsFulfilled()
        await this.updateNotListablePage.confirmSubmission()
        await this.verifyHistoryTabDetails("Ready to list", "Update not listable case");
    }

    async performUpdateNotListableDirectionNotFulfilledReadyToList(){

        await this.performNotListableEvent();
        await this.homePage.chooseEvent("Update not listable case");
        await this.updateNotListablePage.requirementsNotFulfilled();

        //Set interlocutory review option to No
        await this.updateNotListablePage.interlocutoryReviewStateNotRequired();

        //Selecting NO to updating Directions Due Date
        await this.updateNotListablePage.noNewDueDateRequired();

        //Moving case to ready to list and confirming event success
        await this.updateNotListablePage.moveCaseToReadyToList();
        await this.updateNotListablePage.confirmSubmission();

        await this.verifyHistoryTabDetails("Ready to list", "Update not listable case");
    }

    async performUpdateNotListableDirectionNotFulfilledWithFTA(){

        await this.performNotListableEvent();
        //Trigger Update not listable case event
        await this.homePage.chooseEvent("Update not listable case");
        await this.updateNotListablePage.requirementsNotFulfilled();

        //Set interlocutory review option to No
        await this.updateNotListablePage.interlocutoryReviewStateNotRequired()

        //No new due date required, Moving case to With FTA and confirming event success
        await this.updateNotListablePage.noNewDueDateMoveCaseToWithFTA();
        await this.updateNotListablePage.confirmSubmission();

        await this.verifyHistoryTabDetails("With FTA", "Update not listable case");
    }

    async performUpdateNotListableDirectionNotFulfilledNewDueDate(){

        await this.performNotListableEvent();
        //Trigger Update not listable case event
        await this.homePage.chooseEvent("Update not listable case");
        await this.updateNotListablePage.requirementsNotFulfilled();

        //Set interlocutory review option to No
        await this.updateNotListablePage.interlocutoryReviewStateNotRequired()

        //Selecting NO to updating Directions Due Date
        await this.updateNotListablePage.newDueDateRequired();

        await this.updateNotListablePage.confirmSubmission();

        await this.verifyHistoryTabDetails("Not listable", "Update not listable case");
    }

    async performUpdateNotListableDirectionNotFulfilledTCW(){
        await this.performNotListableEvent();
        //Trigger Update not listable case event
        await this.homePage.chooseEvent("Update not listable case");
        await this.updateNotListablePage.requirementsNotFulfilled();

        //Set interlocutory review option to Yes TCW to review.
        await this.updateNotListablePage.interlocutoryReviewRequiredTCW()

        await this.updateNotListablePage.confirmSubmission()

        await this.verifyHistoryTabDetails("Not listable", "Update not listable case");
    }

    async performUpdateNotListableDirectionNotFulfilledJudge(){
        await this.performNotListableEvent();
        //Trigger Update not listable case event
        await this.homePage.chooseEvent("Update not listable case");
        await this.updateNotListablePage.requirementsNotFulfilled();

        //Set interlocutory review option to Yes Judge to review.
        await this.updateNotListablePage.interlocutoryReviewRequiredJudge()

        await this.updateNotListablePage.confirmSubmission()

        await this.verifyHistoryTabDetails("Not listable", "Update not listable case");
    }

    private async goToNotListablePage(page: Page, caseId: string) {
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.chooseEvent("Not listable");
    }

    async performUpdateNotListableDirectionNotFulfilledAbateTCW(caseId: string){
        
        //Trigger Not listable event:
        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.chooseEvent("Not listable");
        await this.notListablePage.verifyPageContent(); //Verifying Heading and Caption for event

        //inserting data and verifying said data during the event
        await this.notListablePage.enterNotListableProvideReason();
        await this.notListablePage.continueEvent();

        await this.notListablePage.enterValidDirectionDueDate();
        await this.notListablePage.continueEvent();

        await this.notListablePage.confirmSubmission();
       
        // verifying that event has submitted successfully and details are showing in Summary and History Tabs
       await this.verifyHistoryTabDetails("Not listable","Not listable")

        //Trigger Update not listable case event
        await this.homePage.chooseEvent("Update not listable case");
        await this.updateNotListablePage.requirementsNotFulfilled();

        //Set interlocutory review option to Yes TCW to review.
        await this.updateNotListablePage.interlocutoryReviewRequiredTCW()

        await this.updateNotListablePage.confirmSubmission()

        await this.verifyHistoryTabDetails("Not listable", "Update not listable case");
    }

    async performUpdateNotListableDirectionNotFulfilledAbateJudge(caseId: string){
        
        //Trigger Not listable event:
        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.chooseEvent("Not listable");
        await this.notListablePage.verifyPageContent(); //Verifying Heading and Caption for event

        //inserting data and verifying said data during the event
        await this.notListablePage.enterNotListableProvideReason();
        await this.notListablePage.continueEvent();

        await this.notListablePage.enterValidDirectionDueDate();
        await this.notListablePage.continueEvent();

        await this.notListablePage.confirmSubmission();
       
        // verifying that event has submitted successfully and details are showing in Summary and History Tabs
       await this.verifyHistoryTabDetails("Not listable","Not listable")

        //Trigger Update not listable case event
        await this.homePage.chooseEvent("Update not listable case");
        await this.updateNotListablePage.requirementsNotFulfilled();

        //Set interlocutory review option to Yes Judge to review.
        await this.updateNotListablePage.interlocutoryReviewRequiredJudge()

        await this.updateNotListablePage.confirmSubmission()

        await this.verifyHistoryTabDetails("Not listable", "Update not listable case");
    }

}