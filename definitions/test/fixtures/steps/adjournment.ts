import { BaseStep } from './base';
import { expect, Page } from '@playwright/test';
import { credentials } from '../../config/config';
import { StepsHelper } from '../../helpers/stepsHelper';

export class Adjournment extends BaseStep {
    readonly page: Page;
    protected stepsHelper: StepsHelper;
    
    constructor(page: Page) {
        super(page);
        this.page = page;
        this.stepsHelper = new StepsHelper(this.page);
    }

    async verifyHearingHelper(caseId: string) {
        await this.loginUserWithCaseId(credentials.hmrcSuperUser, false, caseId);
        await this.stepsHelper.verifyHearingHelper();
    }

    private async handleNotListableFlow() {
        await this.writeAdjournmentPage.selectDirectionToParties();
        await this.writeAdjournmentPage.submitContinueBtn();
        await this.writeAdjournmentPage.selectDirectionDueDates();
        await this.writeAdjournmentPage.submitContinueBtn();
    }

    async writeAdjournmentAndMoveToListing(caseId: string, options: {
        setToRTLFlag: boolean
    }) {
        await this.signOut();
        await this.loginUserWithCaseId(credentials.judge, false, caseId);
        await this.homePage.chooseEvent('Write adjournment notice');
        await this.writeAdjournmentPage.inputTypeOfAppealPageData(true, 'PIP');
        await this.writeAdjournmentPage.submitContinueBtn();

        await this.writeAdjournmentPage.selectPanelMemsNeeded();
        await this.writeAdjournmentPage.submitContinueBtn();

        //await this.writeAdjournmentPage.inputPanelMembers();
        await this.writeAdjournmentPage.submitContinueBtn();

        await this.writeAdjournmentPage.selectTypeOfHearingHeld();
        await this.writeAdjournmentPage.submitContinueBtn();

        await this.writeAdjournmentPage.selectToBeListed(options.setToRTLFlag);
        await this.writeAdjournmentPage.submitContinueBtn();

        if (!options.setToRTLFlag) await this.handleNotListableFlow();

        await this.writeAdjournmentPage.selectNextHearingType();
        await this.writeAdjournmentPage.submitContinueBtn();

        await this.writeAdjournmentPage.selectHearingVenue();
        await this.writeAdjournmentPage.submitContinueBtn();

        await this.writeAdjournmentPage.submitContinueBtn();

        await this.writeAdjournmentPage.selectStandardTimeSlot();
        await this.writeAdjournmentPage.submitContinueBtn();

        await this.writeAdjournmentPage.selectNoInterpreterRequired();
        await this.writeAdjournmentPage.submitContinueBtn();

        await this.writeAdjournmentPage.selectFirstAvailableDate();
        await this.writeAdjournmentPage.submitContinueBtn();

        await this.writeAdjournmentPage.addReasonForAdjournment('Test Adjournment Reason');
        await this.writeAdjournmentPage.submitContinueBtn();

        await this.writeAdjournmentPage.submitContinueBtn();
        await this.writeAdjournmentPage.submitContinueBtn();


        await this.writeAdjournmentPage.confirmSubmission();

        await this.verifyHistoryTabDetails('Write adjournment notice');
    }

    async performIssueAdjournmentNoticeForAnAppeal(options: {
        endState: string
    }) {
        await this.homePage.chooseEvent('Issue adjournment notice');
        await this.writeAdjournmentPage.verifyPageContentForPreviewDecisionNoticePage();
        await this.writeAdjournmentPage.confirmSubmission();
        await this.writeFinalDecisionPage.confirmSubmission();
        await this.verifyHistoryTabDetails('Issue adjournment notice');
        await this.verifyEndStateInHistoryTab(options.endState);
   }

     async verifyAdjournmentDecisionForAnAppeal() {
        await this.homePage.navigateToTab('Documents');
        await this.documentsTab.verifyPageContentByKeyValue(
        'Type',
        'Adjournment Notice'
        );
        await this.documentsTab.verifyPageContentByKeyValue('Bundle addition', 'A');
        await this.documentsTab.verifydueDates('Date added');
      }

      async verifyListingRequirements() {
        await this.homePage.navigateToTab('Listing Requirements');
        await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
            'Duration of the hearing',
            '60'
        );
        await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
            'Is an interpreter wanted?',
            'No'
        );
        await this.listingRequirementsTab.verifyContentByKeyValueForASpan(
            "Appellant's Hearing Channel",
            'Face To Face'
        );
        await this.signOut();
      }

}

