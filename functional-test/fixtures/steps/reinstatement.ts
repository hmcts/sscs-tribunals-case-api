import { BaseStep } from "./base";
import { expect, Page } from '@playwright/test';
import { credentials } from "../../config/config";
import task from '../../pages/content/review.reinstatement.request.task_en.json';
import amendInterlocReviewStateData from "../../pages/content/amend.interloc.review.state_en.json";
import dateUtilsComponent from '../../utils/DateUtilsComponent';
import { SendToAdmin } from "./send.to.admin";
import { VoidCase } from "./void.case";

const actionFurtherEvidenceTestdata = require('../../pages/content/action.further.evidence_en.json');
const issueDirectionTestdata = require('../../pages/content/issue.direction_en.json');


export class Reinstatement extends BaseStep {

    readonly page: Page;

    constructor(page){
        super(page);
        this.page = page;
    }

    async requestAndGrantAnReinstatement(caseId: string) {
        
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent(actionFurtherEvidenceTestdata.eventName);
        await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
            actionFurtherEvidenceTestdata.sender, 
            actionFurtherEvidenceTestdata.reinstatementDocType, 
            actionFurtherEvidenceTestdata.testfileone
        );
        await this.eventNameAndDescriptionPage.verifyPageContent(actionFurtherEvidenceTestdata.eventName);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifydueDates('Reinstatement Registered');
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('Reinstatement Outcome', 'In progress');
        await this.verifyHistoryTabDetails('With FTA', 'Issue further evidence');
        await this.historyTab.verifyPageContentByKeyValue('Interlocutory review state', 'Review by Judge');

        await this.loginUserWithCaseId(credentials.judge, true, caseId);
        await this.homePage.chooseEvent(issueDirectionTestdata.eventNameCaptor);

        await this.issueDirectionPage.submitIssueDirection(
            issueDirectionTestdata.preHearingType,
            issueDirectionTestdata.grantReinstatementOption, 
            issueDirectionTestdata.docTitle
        );
        await this.eventNameAndDescriptionPage.verifyPageContent(issueDirectionTestdata.eventNameCaptor);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifydueDates('Reinstatement Registered');
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('Reinstatement Outcome', 'Granted');
        
        await this.verifyHistoryTabDetails('With FTA', 'Issue directions notice');
        await this.historyTab.verifyPageContentByKeyValue('Interlocutory review state', 'Awaiting Admin Action');
    }

    async requestAndRefuseAnReinstatement(caseId: string) {
        
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.reloadPage();
        await this.homePage.chooseEvent(actionFurtherEvidenceTestdata.eventName);
        await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
            actionFurtherEvidenceTestdata.sender, 
            actionFurtherEvidenceTestdata.reinstatementDocType, 
            actionFurtherEvidenceTestdata.testfileone
        );

        await this.eventNameAndDescriptionPage.verifyPageContent(actionFurtherEvidenceTestdata.eventName);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifydueDates('Reinstatement Registered');
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('Reinstatement Outcome', 'In progress');
        await this.verifyHistoryTabDetails('With FTA', 'Issue further evidence');
        await this.historyTab.verifyPageContentByKeyValue('Interlocutory review state', 'Review by Judge');

        await this.loginUserWithCaseId(credentials.judge, true, caseId);
        await this.homePage.chooseEvent(issueDirectionTestdata.eventNameCaptor);

        await this.issueDirectionPage.submitIssueDirection(
            issueDirectionTestdata.preHearingType,
            issueDirectionTestdata.refuseReinstatementOption, 
            issueDirectionTestdata.docTitle
        );
        await this.eventNameAndDescriptionPage.verifyPageContent(issueDirectionTestdata.eventNameCaptor);
        await this.eventNameAndDescriptionPage.confirmSubmission();
        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        await this.homePage.navigateToTab("Appeal Details");
        await this.appealDetailsTab.verifydueDates('Reinstatement Registered');
        await this.appealDetailsTab.verifyAppealDetailsPageContentByKeyValue('Reinstatement Outcome', 'Refused');
        await this.verifyHistoryTabDetails('With FTA', 'Issue directions notice');
    }

    async verifySalariedJudgeCanViewTheUnassignedReviewReinstatementRequestTask(caseId: string): Promise<void> {
         
        // Verify Review Reinstatement Request - Judge task is displayed to the Salaried Judge
         await this.loginUserWithCaseId(credentials.salariedJudge, false, caseId);
         await this.homePage.navigateToTab('Tasks');
         await this.tasksTab.verifyTaskIsDisplayed(task.name);
         await this.tasksTab.verifyPageContentByKeyValue(task.name, 
            'Task created', dateUtilsComponent.formatDateToSpecifiedDateFormat(new Date()));
         await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
         await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);
    }

    async verifySalariedJudgeCanCompleteTheUnassignedReviewReinstatementRequestTask(caseId: string): Promise<void> {

        // Verify Salaried Judge self assigns the task
        await this.loginUserWithCaseId(credentials.salariedJudge, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.selfAssignTask(task.name);
        
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToSalariedJudge);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForSalariedJudge);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Select Send to Admin next step and complete the event
        await this.tasksTab.clickNextStepLink(task.sendToAdmin.link);

        let sendToAdmin = new SendToAdmin(this.page);
        await sendToAdmin.comepleteSendToAdmin();

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifySalariedJudgeCanReassignTheReviewReinstatementRequestTaskToTcw(caseId: string): Promise<void> {

        // Verify Salaried Judge self assigns the task
        await this.loginUserWithCaseId(credentials.salariedJudge, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.selfAssignTask(task.name);
        
        await this.tasksTab.reassignTaskToTcwUser(task.name, credentials.amTribunalCaseWorker.email);
    }

    async verifyFeePaidJudgeCanViewTheUnassignedReviewReinstatementRequestTask(caseId: string): Promise<void> {
        // Verify Review Reinstatement Request - Judge task is displayed to the Fee-Paid Judge
         await this.loginUserWithCaseId(credentials.feePaidJudge, false, caseId);
         await this.homePage.navigateToTab('Tasks');
         await this.tasksTab.verifyTaskIsDisplayed(task.name);
         await this.tasksTab.verifyPageContentByKeyValue(task.name, 
            'Task created', dateUtilsComponent.formatDateToSpecifiedDateFormat(new Date()));
         await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
         await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);
    }

    async verifyFeePaidJudgeCanCompleteTheUnassignedReviewReinstatementRequestTask(caseId: string): Promise<void> {

        // Fee-Paid Judge self assigns the task
        await this.loginUserWithCaseId(credentials.feePaidJudge, true, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.selfAssignTask(task.name);
        
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToFeePaidJudge);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForFeePaidJudge);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Select Send to Admin next step and complete the event
        await this.tasksTab.clickNextStepLink(task.sendToAdmin.link);

        let sendToAdmin = new SendToAdmin(this.page);
        await sendToAdmin.comepleteSendToAdmin();

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifyTcwCanCompleteTheAssignedReviewReinstatementRequestTask(caseId: string): Promise<void> {

        // Verify TCW can see the assigned Review Reinstatement Request - Judge task
        await this.loginUserWithCaseId(credentials.amTribunalCaseWorker, false, caseId);
        await this.homePage.navigateToTab('Tasks');
        
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToTCW);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForTCW);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Select Amend Interloc review state next step and complete the event
        await this.tasksTab.clickNextStepLink(task.amendInterlocReviewState.link);

        await this.amendInterlocReviewStatePage.verifyPageContent();
        await this.amendInterlocReviewStatePage.selectReviewState(amendInterlocReviewStateData.interlocReviewStateSelectValue);
        await this.amendInterlocReviewStatePage.confirmSelection();
        await this.amendInterlocReviewStatePage.confirmSubmission();

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifySalariedJudgeCanCompleteTheReviewReinstatementRequestTaskManually(caseId: string): Promise<void> {

        // Verify Salaried Judge self assigns the task
        await this.loginUserWithCaseId(credentials.salariedJudge, false, caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.selfAssignTask(task.name);
        
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToSalariedJudge);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForSalariedJudge);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Judge selects to mark the task as done
        await this.tasksTab.markTheTaskAsDone(task.name);

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifySalariedJudgeCanViewAndSelfAssignTheReviewReinstatementRequestTask(caseId: string): Promise<void> {

        // Verify Review Reinstatement Request - Judge task is displayed to the Salaried Judge
        await this.verifySalariedJudgeCanViewTheUnassignedReviewReinstatementRequestTask(caseId);
        await this.tasksTab.selfAssignTask(task.name);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToSalariedJudge);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForSalariedJudge);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);
    }

    async verifyReviewReinstatementRequestTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId: string): Promise<void> {

        // CTSC Admin voids the case
        let voidCase = new VoidCase(this.page);
        await voidCase.performVoidCase(caseId);

        // Verify task is removed from the tasks list within Tasks tab
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }
}
