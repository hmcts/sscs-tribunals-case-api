import {expect, Page} from "@playwright/test";
import {StringUtilsComponent} from "../../utils/StringUtilsComponent";
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import linkCaseTestData from "../../pages/content/link.case_en.json";
import eventTestData from "../../pages/content/event.name.event.description_en.json"
import {credentials} from "../../config/config";
import {BaseStep} from "./base";

export class LinkCase extends BaseStep {

    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async linkCaseSuccessfully() {
        // Start by creating cases to be linked
        var firstPipCaseId = await createCaseBasedOnCaseType("PIP");
        var secondPipCaseId = await createCaseBasedOnCaseType("PIP");
        let hyphenatedSecondCaseId = StringUtilsComponent.formatClaimReferenceToAUIDisplayFormat(secondPipCaseId).replace(/\s/g, '-');

        // Event starts and content is verified
        await this.goToLinkACasePage(this.page, firstPipCaseId);
        await this.linkACasePage.verifyPageContent(firstPipCaseId);

        // Cases are linked and event is submitted
        await this.linkACasePage.linkCase(secondPipCaseId);
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        // Moving to Related Appeals tab and verifying that the cases are linked by looking for 'secondPipCaseId' on 'firstPipCaseId' case.
        await this.homePage.navigateToTab("Related Appeals");
        await expect(this.page.getByText(hyphenatedSecondCaseId)).toBeVisible();

        // Moving to History tab and verifying that 'Link a case' event has run and the end state is what is expected.
        await this.homePage.navigateToTab("History");
        await this.historyTab.verifyPageContentByKeyValue('End state', 'With FTA');
        await this.historyTab.verifyPageContentByKeyValue('Event', 'Link a case');
        await this.historyTab.verifyEventCompleted('Link a case');
    }

    async linkCaseToItself() {
        // Only need 1 case created as we will be linking the case using the same case id.
        const pipCaseId = await createCaseBasedOnCaseType("PIP");

        // Link a case event has been triggered and the case reference id for the is used for Link a case.
        await this.goToLinkACasePage(this.page, pipCaseId);
        await this.linkACasePage.linkCase(pipCaseId)
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        // Error has been thrown and now is being verified.
        await this.linkACasePage.verifyCaseCannotLinkToItself();
    }

    async linkNonExistingCase() {
        // Only need 1 case created as we will be linking the case using an invalid case id.
        const pipCaseId = await createCaseBasedOnCaseType("PIP");

        // Link a case event has been triggered and invalid case reference id is used for Link a case.
        await this.goToLinkACasePage(this.page, pipCaseId);
        await this.linkACasePage.linkCase(linkCaseTestData.linkNonExistentCase);

        // Error message has been thrown and is now being verified.
        await this.linkACasePage.verifyCannotLinkFakeCase(linkCaseTestData.linkNonExistentCase)
    }

    async removeLinkedCase(){
        // Similar to linkCaseSuccessfully event we start by linking 2 cases.
        var firstPipCaseId = await createCaseBasedOnCaseType("PIP");
        var secondPipCaseId = await createCaseBasedOnCaseType("PIP");
        let hyphenatedSecondCaseId = StringUtilsComponent.formatClaimReferenceToAUIDisplayFormat(secondPipCaseId).replace(/\s/g, '-');

        await this.goToLinkACasePage(this.page, firstPipCaseId);
        await this.linkACasePage.verifyPageContent(firstPipCaseId);

        await this.linkACasePage.linkCase(secondPipCaseId);
        await this.linkACasePage.confirmSubmission();

        await this.homePage.navigateToTab("Related Appeals");
        await expect(this.page.getByText(hyphenatedSecondCaseId)).toBeVisible();

        // Now that cases are linked, rerun the event to remove the link between the cases.
        await this.goToLinkACasePage(this.page, firstPipCaseId);

        //Clicking remove buttons once event has been triggered
        await this.linkACasePage.removeLink();

        // Submit the event to confirm removal of link between cases.
        await this.linkACasePage.confirmSubmission();
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await this.homePage.delay(3000);

        // Confirm that event has successfully run and is showing in History tab.
        await this.homePage.goToHomePage(firstPipCaseId);
        await this.homePage.delay(1000);
        await this.homePage.navigateToTab("History");
        await this.historyTab.verifyPageContentByKeyValue('End state', 'With FTA');
        await this.historyTab.verifyPageContentByKeyValue('Event', 'Link a case');
    }

   // Event created to trigger Link a case event from next steps dropdown menu:
    private async goToLinkACasePage(page: Page, caseId: string) {
        await this.loginUserWithCaseId(credentials.amCaseWorker, true, caseId);
        await this.homePage.chooseEvent("Link a case");
    }
}
