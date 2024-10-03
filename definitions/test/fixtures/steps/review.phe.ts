import { BaseStep } from "./base";
import { Page, expect } from '@playwright/test';
import { credentials } from "../../config/config";
import task from '../../pages/content/review.phe.request.task_en.json';
import dateUtilsComponent from '../../utils/DateUtilsComponent';
import { VoidCase } from "./void.case";

const reviewPHETestdata = require('../../pages/content/review.phe_en.json');
const bundleTestData = require('../../pages/content/create.a.bundle_en.json');


export class ReviewPHE extends BaseStep {

    readonly page: Page;

    constructor(page){
        
        super(page);
        this.page = page;
    }

    async grantAnPHERequest(caseId: string) {
        
        await this.loginUserWithCaseId(credentials.judge, false, caseId);
        await this.homePage.chooseEvent(reviewPHETestdata.eventNameCaptor);

        await this.reviewPHEPage.verifyPageContent();
        await this.reviewPHEPage.selectGrantPermission();
        await this.reviewPHEPage.confirmSubmission();

        await this.eventNameAndDescriptionPage.verifyPageContent(reviewPHETestdata.eventNameCaptor);
        await this.eventNameAndDescriptionPage.confirmSubmission();

       
        await this.summaryTab.verifyPresenceOfTitle(reviewPHETestdata.pheGrantedText);
        await this.verifyHistoryTabDetails(reviewPHETestdata.eventNameCaptor);
        await this.historyTab.verifyPresenceOfTitle(reviewPHETestdata.pheGrantedText);
        await this.homePage.clickSignOut();

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('FTA State', 'PHE granted');
        await this.verifyBundleForPHE();
    }

    async verifyBundleForPHE(){
        
        await this.homePage.chooseEvent("Create a bundle");
        await this.createBundlePage.verifyPageContent();
        await this.createBundlePage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();

        await this.homePage.delay(15000);
        await this.homePage.reloadPage();
        await this.homePage.navigateToTab("Bundles");
        await this.bundlesTab.verifyBundlesTabContentByKeyValueForASpan(`${bundleTestData.stitchStatusLabel}`, `${bundleTestData.stitchStatusDone}`);
        await this.bundlesTab.verifyBundlesTabContentByKeyValueForASpanRegEx(`${bundleTestData.stitchDocLabel}`, `\\d+-${bundleTestData.stitchVal}\\.pdf`);
        await this.bundlesTab.verifyBundlesTabContentByKeyValueForASpan(`${bundleTestData.configUsed}`, `${bundleTestData.configUsedDefaultVal}`);
        await this.bundlesTab.verifyBundlesTabContentByKeyValueForASpan(`${bundleTestData.configUsed}`, `${bundleTestData.configUsedEditedVal}`);
    }

    async refuseAnPHERequest(caseId: string) {

        await this.homePage.clickSignOut();
        await this.loginUserWithCaseId(credentials.judge, false, caseId);
        await this.homePage.chooseEvent(reviewPHETestdata.eventNameCaptor);

        await this.reviewPHEPage.verifyPageContent();
        await this.reviewPHEPage.selectRefusePermission();
        await this.reviewPHEPage.confirmSubmission();

        await this.eventNameAndDescriptionPage.verifyPageContent(reviewPHETestdata.eventNameCaptor);
        await this.eventNameAndDescriptionPage.confirmSubmission();

       
        await this.summaryTab.verifyTitleNotPresent(reviewPHETestdata.pheGrantedText);
        await this.verifyHistoryTabDetails(reviewPHETestdata.eventNameCaptor);
        await this.homePage.clickSignOut();

        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('FTA State', 'PHE refused');
    }

    async allocateCaseToInterlocutoryJudge(caseId: string) {

        // CTSC Admin with case allocator role allocates case to Interlocutory Judge
        await this.loginUserWithCaseId(credentials.amCaseWorkerWithCaseAllocatorRole, false, caseId);
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);
        await this.homePage.navigateToTab('Roles and access');
        await this.rolesAndAccessTab.allocateInterlocutoryJudge(credentials.salariedJudge.email);
        await this.homePage.clickSignOut();
    }

    async verifyInterlocutoryJudgeCanViewAndCompleteTheAutoAssignedReviewPHERequestTask(caseId: string): Promise<void> {

        // Interlocutory Judge verfies the auto assigned task details
        await this.loginUserWithCaseId(credentials.salariedJudge, false, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
         await this.tasksTab.verifyPageContentByKeyValue(task.name, 
            'Task created', dateUtilsComponent.formatDateToSpecifiedDateFormat(new Date()));
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToInterlocutaryJudge);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForInterlocutaryJudge);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Interlocutory Judge clicks Review PHE Request next step link and completes the event with Refusal
        await this.tasksTab.clickNextStepLink(task.reviewPheRequest.link);
        await this.completeReviewPheRequestWithRefusal();

        // Interlocutory Judge verifies task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifySalariedJudgeCanViewAndCompleteTheUnassignedReviewPHERequestTask(caseId: string): Promise<void> {

        // Verify Salaried Judge can view the unassigned task
        await this.loginUserWithCaseId(credentials.salariedJudge, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 
            'Task created', dateUtilsComponent.formatDateToSpecifiedDateFormat(new Date()));
         await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
         await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptionsForSalariedJudge);

        // Salaried Judge self assigns the task
        await this.tasksTab.selfAssignTask(task.name);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToSalariedJudge);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForSalariedJudge);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Salaried Judge clicks Review PHE Request next step link and completes the event with Grant
        await this.tasksTab.clickNextStepLink(task.reviewPheRequest.link);
        await this.completeReviewPheRequestWithGrant();

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifyFeePaidJudgeCanViewAndCompleteTheUnassignedReviewPHERequestTask(caseId: string): Promise<void> {

        // Fee Paid Judge self assigns the task
        await this.loginUserWithCaseId(credentials.feePaidJudge, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 
            'Task created', dateUtilsComponent.formatDateToSpecifiedDateFormat(new Date()));
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);

         // Fee-Paid Judge self assigns the task
        await this.tasksTab.selfAssignTask(task.name);        
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToFeePaidJudge);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForFeePaidJudge);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Fee-Paid Judge clicks Review PHE Request next step link and completes the event with Refusal
        await this.tasksTab.clickNextStepLink(task.reviewPheRequest.link);
        await this.completeReviewPheRequestWithRefusal();

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifySalariedJudgeCanViewAndSelfAssignTheReviewPHERequestTask(caseId: string): Promise<void> {

        // Verify Review PHE Request - Judge task is displayed to the Salaried Judge
        await this.loginUserWithCaseId(credentials.salariedJudge, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(task.name);

        // Salaried Judge self assigns the task
        await this.tasksTab.selfAssignTask(task.name);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToSalariedJudge);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForSalariedJudge);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);
    }

    async verifyReviewPHERequestTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId: string): Promise<void> {

        // CTSC Admin voids the case
        let voidCase = new VoidCase(this.page);
        await voidCase.performVoidCase(caseId);

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async completeReviewPheRequestWithGrant() {
        await this.reviewPHEPage.verifyPageContent();
        await this.reviewPHEPage.selectGrantPermission();
        await this.reviewPHEPage.confirmSubmission();

        await this.eventNameAndDescriptionPage.verifyPageContent(reviewPHETestdata.eventNameCaptor);
        await this.eventNameAndDescriptionPage.confirmSubmission();
       
        await this.summaryTab.verifyPresenceOfTitle(reviewPHETestdata.pheGrantedText);
        await this.verifyHistoryTabDetails(reviewPHETestdata.eventNameCaptor);
        await this.historyTab.verifyPresenceOfTitle(reviewPHETestdata.pheGrantedText);
    }

    async completeReviewPheRequestWithRefusal() {
        await this.reviewPHEPage.verifyPageContent();
        await this.reviewPHEPage.selectRefusePermission();
        await this.reviewPHEPage.confirmSubmission();

        await this.eventNameAndDescriptionPage.verifyPageContent(reviewPHETestdata.eventNameCaptor);
        await this.eventNameAndDescriptionPage.confirmSubmission();
       
        await this.summaryTab.verifyTitleNotPresent(reviewPHETestdata.pheGrantedText);
        await this.verifyHistoryTabDetails(reviewPHETestdata.eventNameCaptor);
    }
}