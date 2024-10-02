import {Page} from '@playwright/test';
import {HomePage} from '../pages/common/homePage';
import {UploadResponsePage} from '../pages/upload.response.page';

export class StepsHelper {

    readonly page: Page;
    public homePage: HomePage;
    public uploadResponsePage: UploadResponsePage;

    constructor(page: Page) {
        this.page = page;
        this.homePage = new HomePage(this.page);
        this.uploadResponsePage = new UploadResponsePage(this.page);
    }

    async uploadResponseHelper(issueCodeData: string, assistOption: string, phmeFlag?: boolean, ucbFlag?: boolean, avFlag?: boolean) {
        await this.homePage.chooseEvent('Upload response');
        await this.homePage.delay(4000);

        await this.uploadResponsePage.verifyPageContent();
        await this.uploadResponsePage.uploadDocs();
        if(phmeFlag) await this.uploadResponsePage.uploadPHEDocs();
        if(avFlag) await this.uploadResponsePage.uploadAVDocs();
        if(ucbFlag) await this.uploadResponsePage.uploadUCBDocs();
        await this.uploadResponsePage.selectIssueCode(issueCodeData);
        await this.uploadResponsePage.chooseAssistOption(assistOption);
        await this.uploadResponsePage.continueSubmission();
    }
}
