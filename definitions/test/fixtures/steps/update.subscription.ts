import { expect, Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from "../../config/config";
import UpdateSubscriptionTestData from "../../pages/content/update.subscription.data_en.json"
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import { StringUtilsComponent } from "../../utils/StringUtilsComponent";
import { time } from 'console';
const eventTestData = require("../../pages/content/event.name.event.description_en.json");

export class UpdateSubscription extends BaseStep {  //updated class

    readonly page: Page;


    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performUpdateSubscription(caseId: string) {   // added new method regarding subscription event
        // Creating case - PIP
        var PipCaseId = await createCaseBasedOnCaseType("PIP");

        // Starting event
        await this.goToUpdateSubscriptionPage(this.page, PipCaseId);
        await this.updateSubscriptionPage.verifyPageContent();

        // Filling fields and Submitting the event
        await this.updateSubscriptionPage.applySubscription();
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        // Verifying History tab + end state
        await this.verifyHistoryTabDetails("Update subscription");
        await this.historyTab.verifyPageContentByKeyValue('End state', 'With FTA');
        await this.historyTab.verifyPageContentByKeyValue('Event', 'Update subscription');
        await this.historyTab.verifyPageContentByKeyValue('Comment', 'Event Description for Automation Verification');

        // Navigate to Subscriptions tab + validations
        await this.homePage.navigateToTab("Subscriptions");
        await this.homePage.delay(1000);
        await this.subscriptionsTab.verifyPageContentByKeyValue;

    }

    // Event created to trigger Update subscription event from next steps dropdown menu:
    private async goToUpdateSubscriptionPage(page: Page, caseId: string) {
        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.chooseEvent("Update subscription");
    }
}