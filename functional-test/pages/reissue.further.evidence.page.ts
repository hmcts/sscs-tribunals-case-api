import { expect, Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import reissueFurtherEvidenceData from "./content/reissue.further.evidence_en.json";


let webAction: WebAction;

export class ReissueFurtherEvidencePage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContentReissueEvent() {
        await webAction.verifyPageLabel('.govuk-caption-l', reissueFurtherEvidenceData.reissueFurtherEvidenceHeadingReissue); //Above heading Text
        await webAction.isLinkClickable('Cancel');
    }

    async verifyPageContentActionEvent() {
        await webAction.verifyPageLabel('.govuk-caption-l', reissueFurtherEvidenceData.reissueFurtherEvidenceHeadingAction); //Above heading Text
        await webAction.isLinkClickable('Cancel');
    }

    async applyActionFurtherEvidence() {
        // Filling Action further evidence page
        await this.page.locator('#furtherEvidenceAction').selectOption('2: otherDocumentManual');
        await this.page.locator('#originalSender').selectOption('1: appellant');
        await this.page.getByRole('button', { name: 'Add New' }).click(); 
        await this.page.locator('#scannedDocuments_0_fileName').fill(reissueFurtherEvidenceData.reissueFurtherEvidenceFileName);

        await webAction.clickButton("Submit");
        await webAction.clickButton("Submit"); //second Submit to complete the event
    }

    async applyReissueFurtherEvidence() {
        // Filling Reissue further evidence page
        await this.page.locator('#reissueFurtherEvidenceDocument').selectOption('1: http://dm-store-aat.service.core-compute-aat.in');
        await this.page.locator('#resendToAppellant_Yes').click();
        await this.page.locator('#resendToRepresentative_No').click();
        await this.page.locator('#resendToDwp_No').click();
    
        await webAction.clickButton("Submit");
        await webAction.clickButton("Submit"); //second Submit to complete the event
}

    async cancelEvent(): Promise<void> {
        await webAction.clickLink("Cancel");
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickSubmitButton();
    }
}