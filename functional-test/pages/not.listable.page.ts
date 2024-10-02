import {WebAction} from "../common/web.action";
import {Page} from "@playwright/test";
import updatenotListableData from "./content/update.not.listable_en.json";
import eventTestData from "./content/event.name.event.description_en.json";

let webActions: WebAction;

export class NotListablePage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webActions.verifyPageLabel('.govuk-caption-l', updatenotListableData.notListableEventCaption); //Caption Text
        await webActions.verifyPageLabel('.govuk-heading-l', updatenotListableData.notListableEventHeading); //Heading Text
    }

    async enterNotListableProvideReason(){
        await webActions.inputField('#notListableProvideReasons',updatenotListableData.notListableProvideReasons);
    }


    async enterValidDirectionDueDate() {
        await webActions.typeField('#notListableDueDate-day', '21');
        await webActions.typeField('#notListableDueDate-month', '12');
        await webActions.typeField('#notListableDueDate-year', '2025');
    }

    async enterInvalidDirectionDueDate() {
        await webActions.typeField('#notListableDueDate-day', '01');
        await webActions.typeField('#notListableDueDate-month', '02');
        await webActions.typeField('#notListableDueDate-year', '2024');
    }

    async verifyPastDueDateErrorMessage(){
       await this.page.getByText(updatenotListableData.notListablePastDueDataErrorMessage).isVisible();
    }

    async verifyNotListableReasonError(){
        await webActions.verifyPageLabel('.validation-error', updatenotListableData.notListableReasonsErrorMessage);
    }

    async verifyCheckYourAnswersPage(){
        await webActions.verifyTextVisibility('Test Reason(s) for the case being not listable is required');
        await webActions.verifyTextVisibility('21 Dec 2025');
    }

    async insertEventSummaryAndDescription(){
        await webActions.inputField('#field-trigger-summary',eventTestData.eventSummaryInput)
        await webActions.inputField('#field-trigger-description',eventTestData.eventDescriptionInput)
    }

    async confirmSubmission(): Promise<void> {
        await this.page.waitForTimeout(3000);
        await this.page.getByText('Submit').dblclick();
        await this.page.waitForLoadState("domcontentloaded");
    }

    async continueEvent(): Promise<void> {
        await this.page.waitForTimeout(3000);
        await this.page.getByText('Continue').click();
    }

}