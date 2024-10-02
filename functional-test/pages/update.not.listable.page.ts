import {WebAction} from "../common/web.action";
import {Page} from "@playwright/test";
import updateNotListableData from "./content/update.not.listable_en.json";

let webActions: WebAction;

export class UpdateNotListablePage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }
    async verifyPageContent() {
        await webActions.verifyPageLabel('.govuk-caption-l', updateNotListableData.updateNotListableEventCaption); //Caption Text
        await webActions.verifyPageLabel('.govuk-heading-l', updateNotListableData.updateNotListableEventHeading); //Heading Text
        await webActions.verifyTextVisibility("Have the requirements in the direction been fulfilled?");
    }

    async requirementsFulfilled(){
        await this.page.waitForTimeout(3000);
        await this.page.click('#updateNotListableDirectionsFulfilled_Yes');
        await this.page.getByText('Continue').click();

    }

    async requirementsNotFulfilled(){
        await this.page.waitForTimeout(3000);
        await this.page.click('#updateNotListableDirectionsFulfilled_No');
        await this.page.getByText('Continue').click();
    }

    async interlocutoryReviewStateNotRequired(){
        await this.page.waitForTimeout(3000);
        await this.page.click('#updateNotListableInterlocReview_No');
        await this.page.getByText('Continue').click();
    }

    async interlocutoryReviewRequiredTCW(){
        await this.page.waitForTimeout(3000);
        await this.page.click('#updateNotListableInterlocReview_Yes');
        await webActions.chooseOptionByLabel('#updateNotListableWhoReviewsCase',"A TCW");
        await this.page.getByText('Continue').click();
    }


    async interlocutoryReviewRequiredJudge(){
        await this.page.waitForTimeout(3000);
        await this.page.click('#updateNotListableInterlocReview_Yes');
        await webActions.chooseOptionByLabel('#updateNotListableWhoReviewsCase',"A Judge");
        await this.page.getByText('Continue').click();
    }

    async noNewDueDateRequired(){
        await this.page.waitForTimeout(3000);
        await this.page.click('#updateNotListableSetNewDueDate_No');
        await this.page.getByText('Continue').click();
    }

    async newDueDateRequired(){
        await this.page.waitForTimeout(3000);
        await this.page.click('#updateNotListableSetNewDueDate_Yes');
        await this.page.waitForTimeout(3000);
        await webActions.inputField('#updateNotListableDueDate-day', '13');
        await webActions.inputField('#updateNotListableDueDate-month', '06');
        await webActions.inputField('#updateNotListableDueDate-year', '2025');
        await this.page.waitForTimeout(3000);
        await this.page.click('#updateNotListableSetNewDueDate_Yes');
        await this.page.waitForTimeout(3000);
        await this.page.getByText('Continue').click();
    }

    async noNewDueDateMoveCaseToWithFTA(){
        await this.page.waitForTimeout(3000);
        await this.page.click('#updateNotListableSetNewDueDate_No');
        await this.page.getByText('Continue').click();
        await this.page.waitForTimeout(3000);
        await this.page.click('#updateNotListableWhereShouldCaseMoveTo-withDwp');
        await this.page.getByText('Continue').click();
    }

    async moveCaseToReadyToList(){
        await this.page.waitForTimeout(3000);
        await this.page.click('#updateNotListableWhereShouldCaseMoveTo-readyToList')
        await this.page.getByText('Continue').click();
    }



    async confirmSubmission(): Promise<void> {
        await this.page.waitForTimeout(3000);
        await this.page.getByText('Submit').click();
    }

    async continueEvent(): Promise<void> {
        await this.page.waitForTimeout(3000);
        await this.page.getByText('Continue').click();
    }

    }