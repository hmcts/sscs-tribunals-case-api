import {BaseStep} from "./base";
import {expect, Page} from "@playwright/test";
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import axeTest from "../../helpers/accessibility";
import {credentials} from "../../config/config";
import {StepsHelper} from "../../helpers/stepsHelper";
import uploadDocumentFurtherEvidenceData from "../../pages/content/upload.document.further.evidence_en.json";
import eventTestData from "../../pages/content/event.name.event.description_en.json";
import bundleTestData from '../../pages/content/create.a.bundle_en.json';
import {StringUtilsComponent} from "../../utils/StringUtilsComponent";

// Accessibility Test Steps:
// Create a case
// Login as dwp user -> (scan login page)
// Upload FE -> (scan pages)
// Action further evidence -> (scan pages)
// Upload response -> (scan pages)
// Create bundle -> (scan page)
// Link a case -> (scan page)
// Scan remaining tabs

export class AccessibilitySteps extends BaseStep {

    readonly page: Page;
    protected stepsHelper: StepsHelper;



    constructor(page: Page) {
        super(page);
        this.page = page;
        this.stepsHelper = new StepsHelper(this.page);
    }


    async performAccessibilityTest(firstCaseId: string, secondCaseId: string)  {

        await createCaseBasedOnCaseType('PIP')
        await this.loginUserWithCaseId(credentials.superUser, false, firstCaseId);
        await axeTest(this.page)
        await this.homePage.chooseEvent("Upload document FE")

        await this.uploadDocumentFurtherEvidencePage.verifyPageContent();
        await axeTest(this.page)

        await this.uploadDocumentFurtherEvidencePage.clickAddNew();
        await this.uploadDocumentFurtherEvidencePage.selectDocumenType(uploadDocumentFurtherEvidenceData.documentType);
        await this.uploadDocumentFurtherEvidencePage.inputFilename(uploadDocumentFurtherEvidenceData.fileName);
        await this.uploadDocumentFurtherEvidencePage.uploadFurtherEvidenceDoc(uploadDocumentFurtherEvidenceData.testfileone);
        await this.uploadDocumentFurtherEvidencePage.confirmSubmission();

        await this.eventNameAndDescriptionPage.verifyPageContent(uploadDocumentFurtherEvidenceData.eventName);
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await axeTest(this.page)
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await this.homePage.navigateToTab("Unprocessed Correspondence")
        await axeTest(this.page)

        await this.homePage.chooseEvent('Upload response');
        await this.homePage.delay(1000);
        await axeTest(this.page)
        await this.uploadResponsePage.verifyPageContent();
        await this.uploadResponsePage.uploadDocs();
        await this.uploadResponsePage.selectIssueCode("CC");
        await this.homePage.delay(1000);
        await this.uploadResponsePage.chooseAssistOption("No");
        await this.homePage.delay(1000);
        await this.uploadResponsePage.continueSubmission();
        //await axeTest(this.page);
        await this.page.getByText("submit").click();

        await this.homePage.delay(1000);

        await this.homePage.navigateToTab("FTA Documents")
        await axeTest(this.page)

        let bundleDate = new Date();
        let formattedDate = bundleDate.toISOString().split('T')[0].split('-').reverse().join('-');

        await this.homePage.chooseEvent("Create a bundle");
        await this.createBundlePage.verifyPageContent();
        await axeTest(this.page);
        await this.createBundlePage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();

        await this.homePage.delay(10000);
        await this.homePage.reloadPage();
        await expect(this.homePage.summaryTab).toBeVisible();
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
        await axeTest(this.page);

        await this.homePage.chooseEvent("Link a case");

        await this.linkACasePage.verifyPageContent(firstCaseId);
        await axeTest(this.page)

        let hyphenatedSecondCaseId = StringUtilsComponent.formatClaimReferenceToAUIDisplayFormat(secondCaseId).replace(/\s/g, '-');

        await this.linkACasePage.linkCase(secondCaseId);
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        // Moving to Related Appeals tab and verifying that the cases are linked by looking for 'secondPipCaseId' on 'firstPipCaseId' case.
        await this.homePage.navigateToTab("Related Appeals");
        await expect(this.page.getByText(hyphenatedSecondCaseId)).toBeVisible();
        await axeTest(this.page)

        await this.homePage.goToHomePage(firstCaseId);
        await this.homePage.delay(1000);
        await this.homePage.navigateToTab("History");
        await this.historyTab.verifyPageContentByKeyValue('Event', 'Link a case');
        await axeTest(this.page)
        await this.homePage.navigateToTab("Appeal details")
        await axeTest(this.page)
        await this.homePage.navigateToTab("Subscriptions")
        await axeTest(this.page)
        await this.homePage.navigateToTab("Documents")
        await axeTest(this.page)
        await this.homePage.navigateToTab("Listing Requirements")
        await axeTest(this.page)
        await this.homePage.navigateToTab("CAM Fields")
        await axeTest(this.page)


    }

}