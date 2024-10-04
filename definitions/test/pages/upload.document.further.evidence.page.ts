import { Page, expect } from '@playwright/test';
import { WebAction } from '../common/web.action';
import uploadDocumentFeData  from './content/upload.document.further.evidence_en.json';

let webActions: WebAction;

export class UploadDocumentFurtherEvidencePage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async clickAddNew() {
        await webActions.clickButton('Add new');;
    }

    async verifyPageContent() {
        await webActions.verifyPageLabel('h1.govuk-heading-l', uploadDocumentFeData.pageHeading); //Page heading
        await webActions.verifyPageLabel('div#draftSscsFurtherEvidenceDocument h2.heading-h2', uploadDocumentFeData.sectionHeading); //Section heading
    }

    async selectDocumenType(documentType: string) {
        await expect(this.page.locator('select#draftSscsFurtherEvidenceDocument_0_documentType')).toBeVisible();
        await webActions.chooseOptionByLabel('select#draftSscsFurtherEvidenceDocument_0_documentType', documentType);
    }

    async inputFilename(fileName: string){
        await webActions.inputField('input#draftSscsFurtherEvidenceDocument_0_documentFileName', fileName);
    }

    async uploadFurtherEvidenceDoc(fileName: string): Promise<void> {
        await webActions.uploadFileUsingAFileChooser('input#draftSscsFurtherEvidenceDocument_0_documentLink', fileName);
        await expect(this.page.locator(`div#draftSscsFurtherEvidenceDocument span:has-text('Uploading...')`)).toBeVisible();
        await expect(this.page.locator(`div#draftSscsFurtherEvidenceDocument button:has-text('Cancel upload')`)).toBeDisabled();
        await this.delay(5000);
    }

    async confirmSubmission(): Promise<void> {
        await webActions.clickButton('Submit');
    }

    async delay(ms: number) {
        return new Promise( resolve => setTimeout(resolve, ms) );
    }
}
