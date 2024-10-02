import { Page } from '@playwright/test';
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import { StringUtilsComponent } from "../../utils/StringUtilsComponent";
import { BaseStep } from './base';
import {credentials} from "../../config/config";
const associateCaseTestData = require("../../pages/content/associate.case_en.json");

export class AssociateCase extends BaseStep {

    readonly page : Page;

   constructor(page: Page) {
       super(page);
       this.page = page;
   }

    async associateCaseSuccessfully() {

        var firstPipCaseId = await createCaseBasedOnCaseType("PIP");
        var secondPipCaseId = await createCaseBasedOnCaseType("PIP");
        let hyphenatedSecondCaseId = StringUtilsComponent.formatClaimReferenceToAUIDisplayFormat(secondPipCaseId).replace(/\s/g, '-');
        await this.goToAssociateCasePage(firstPipCaseId);

        await this.associateCasePage.associateCase(secondPipCaseId);
        await this.associateCasePage.confirmSubmission();

        await this.homePage.navigateToTab("Summary");
        await this.summaryTab.verifyPageContentLinkTextByKeyValue('Related appeal(s)', hyphenatedSecondCaseId);
        await this.verifyHistoryTabDetails('With FTA', 'Associate case');
    }

    async associateNonExistentCase() {

        var pipCaseId = await createCaseBasedOnCaseType("PIP");
        await this.goToAssociateCasePage(pipCaseId);

        await this.associateCasePage.associateCase(associateCaseTestData.associateCaseNonExistentCase);
        await this.associateCasePage.verifyInputErrorMessage(associateCaseTestData.associateCaseNonExistentCase);
        await this.associateCasePage.cancelEvent();

        await this.homePage.navigateToTab("Summary");
        await this.summaryTab.verifyFieldHiddenInPageContent('Related appeal(s)');
        await this.verifyHistoryTabDetails('With FTA');
    }

    async selfAssociateACase() {

        var pipCaseId = await createCaseBasedOnCaseType("PIP");
        await this.goToAssociateCasePage(pipCaseId);

        await this.associateCasePage.associateCase(pipCaseId);
        /* TODO: user is able to continue next page when same caseId is used to associate
           instead of displaying error message. Below method needs to be updated when the
           relevant bug is fixed.
        */
        await this.associateCasePage.verifyInputErrorMessage(associateCaseTestData.associateCaseNonExistentCase);
        await this.associateCasePage.cancelEvent();

        await this.homePage.navigateToTab("Summary");
        await this.summaryTab.verifyFieldHiddenInPageContent('Related appeal(s)');
        await this.verifyHistoryTabDetails('With FTA');
    }

    private async goToAssociateCasePage(caseId: string) {
        await this.loginUserWithCaseId(credentials.amCaseWorker,true, caseId);
        await this.homePage.chooseEvent("Associate case");
    }
}
