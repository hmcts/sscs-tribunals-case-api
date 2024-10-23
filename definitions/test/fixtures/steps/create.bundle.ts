import {expect, Page} from '@playwright/test';
import {BaseStep} from './base';
import {credentials} from "../../config/config";
import {StepsHelper} from "../../helpers/stepsHelper";

const bundleTestData = require('../../pages/content/create.a.bundle_en.json')
const uploadResponseTestdata = require('../../pages/content/upload.response_en.json');

export class CreateBundle extends BaseStep {

    private static caseId: string;
    readonly page: Page;
    protected stepsHelper: StepsHelper;

    constructor(page: Page) {
        super(page);
        this.page = page;
        this.stepsHelper = new StepsHelper(this.page);
    }

    async performUploadBundleResponse(caseId) {
        let bundleDate = new Date();
        let formattedDate = bundleDate.toISOString().split('T')[0].split('-').reverse().join('-');

        await this.loginUserWithCaseId(credentials.amSuperUser, false, caseId);
        await this.stepsHelper.uploadResponseHelper(uploadResponseTestdata.pipIssueCode, 'Yes');
        await this.checkYourAnswersPage.confirmSubmission();
        await this.homePage.chooseEvent("Create a bundle");
        await this.createBundlePage.verifyPageContent();
        await this.createBundlePage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();

        await this.homePage.delay(15000);
        await this.homePage.reloadPage();
        await this.homePage.navigateToTab("Bundles");
        await this.bundlesTab.verifyBundlesTabContentByKeyValueForASpan(`${bundleTestData.stitchStatusLabel}`, `${bundleTestData.stitchStatusDone}`);
        await this.bundlesTab.verifyBundlesTabContentByKeyValueForASpanRegEx(`${bundleTestData.stitchDocLabel}`, `\\d+-${bundleTestData.stitchVal}\\.pdf`);

        await this.bundlesTab.verifyTableElementByIndex(bundleTestData.folderName, `${bundleTestData.folderNameVal}`, 0);
        await this.bundlesTab.verifyTableElementByIndex(bundleTestData.docName, `${bundleTestData.folderOneDocVal} ${formattedDate}`, 0);
        await this.bundlesTab.verifyTableElementByIndex(bundleTestData.sourceDoc, `${bundleTestData.folderOneSourceVal} ${formattedDate}.pdf`, 0);
        await this.bundlesTab.verifyTableElementByIndex(bundleTestData.docName, `${bundleTestData.folderTwoDocVal} ${formattedDate}`, 1);
        await this.bundlesTab.verifyTableElementByIndex(bundleTestData.sourceDoc, `${bundleTestData.folderTwoSourceVal} ${formattedDate}.pdf`, 1);
        await this.bundlesTab.verifyTableElementByIndex(bundleTestData.sourceDoc, `${bundleTestData.folderTwoSourceVal} ${formattedDate}.pdf`, 1);
        await this.bundlesTab.verifyTableElementByIndex(bundleTestData.folderName, `${bundleTestData.bundleFolderTwoVal}`, 0);

        await this.bundlesTab.verifyBundlesTabContentByKeyValueForASpan(`${bundleTestData.configUsed}`, `${bundleTestData.configUsedDefaultVal}`);
        await this.bundlesTab.verifyBundlesTabContentByKeyValueForASpan(`${bundleTestData.amendBundle}`, `${bundleTestData.amendBundleDefaultVal}`);
        await this.homePage.clickSignOut();

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.navigateToTab("History");
        await this.verifyHistoryTabDetails("Response received", "Stitching bundle complete");
    }

    async triggerBundleForConfidentialCase() {
        let bundleDate = new Date();
        let formattedDate = bundleDate.toISOString().split('T')[0].split('-').reverse().join('-');

        await this.homePage.chooseEvent("Create a bundle");
        await this.createBundlePage.verifyPageContent();
        await this.createBundlePage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();

        await this.homePage.delay(15000);
        await this.homePage.reloadPage();
        await this.homePage.navigateToTab("Bundles");
        await this.bundlesTab.verifyBundlesTabContentByKeyValueForASpan(`${bundleTestData.stitchStatusLabel}`, `${bundleTestData.stitchStatusDone}`);
        await this.bundlesTab.verifyBundlesTabContentByKeyValueForASpanRegEx(`${bundleTestData.stitchDocLabel}`, `\\d+-${bundleTestData.stitchVal}\\.pdf`);

        await this.bundlesTab.verifyTableElementByIndex(bundleTestData.folderName, `${bundleTestData.folderNameVal}`, 0);
        await this.bundlesTab.verifyTableElementByIndex(bundleTestData.docName, `${bundleTestData.folderOneDocVal} ${formattedDate}`, 0);
        await this.bundlesTab.verifyTableElementByIndex(bundleTestData.sourceDoc, `${bundleTestData.folderOneSourceVal} ${formattedDate}.pdf`, 0);
        await this.bundlesTab.verifyTableElementByIndex(bundleTestData.docName, `${bundleTestData.folderTwoDocVal} ${formattedDate}`, 1);
        await this.bundlesTab.verifyTableElementByIndex(bundleTestData.sourceDoc, `${bundleTestData.folderTwoSourceVal} ${formattedDate}.pdf`, 1);
        await this.bundlesTab.verifyTableElementByIndex(bundleTestData.folderName, `${bundleTestData.bundleFolderTwoVal}`, 0);

        await this.bundlesTab.verifyBundlesTabContentByKeyValueForASpan(`${bundleTestData.configUsed}`, `${bundleTestData.configUsedDefaultVal}`);
        await this.bundlesTab.verifyBundlesTabContentByKeyValueForASpan(`${bundleTestData.amendBundle}`, `${bundleTestData.amendBundleDefaultVal}`);

        //Edited bundle
        await this.bundlesTab.verifyEditedBundlesTabContentByKeyValueForASpan(`${bundleTestData.stitchStatusLabel}`, `${bundleTestData.stitchStatusDone}`, 1);
        await this.bundlesTab.verifyBundlesTabContentByKeyValueForASpan(`${bundleTestData.configUsed}`, `${bundleTestData.configUsedEditedVal}`);
    }
}
