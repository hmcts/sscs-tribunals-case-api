import { Page, expect } from '@playwright/test';
import requestInfo from "../../pages/content/request.info.from.party_en.json";
import eventTestData from "../../pages/content/event.name.event.description_en.json";
import { BaseStep } from './base';
import { credentials } from '../../config/config';
import task from '../../pages/content/review.information.requested.task_en.json';
import { VoidCase } from './void.case';
import { InformationReceived } from './information.received';







export class RequestInfoFromParty extends BaseStep {

    readonly page: Page;

    constructor(page: Page) {
        super(page);
        this.page = page;
    }

    async performRequestInfoFromPartyEvent(caseId: string, loginRequired: boolean = true) {
        if (loginRequired) {
            await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        }

        await this.homePage.chooseEvent('Request info from party');

        await this.completeRequestInfoFromParty();        
    }

    async completeRequestInfoFromParty() {
        await this.requestInfoFromPartyPage.verifyPageContent();
        await this.requestInfoFromPartyPage.chooseRequestInfoFromCaseParty();
        await this.requestInfoFromPartyPage.selectPartyToRequestInfoFrom(requestInfo.requestInfoPartySelectionDropdownValue);
        await this.page.getByRole('button', { name: 'Add new' }).click();

        await this.requestInfoFromPartyPage.inputRequestDetails(requestInfo.requestInfoDetailsToRequestInput);
        await this.requestInfoFromPartyPage.inputDateOfRequest();
        await this.requestInfoFromPartyPage.chooseResponseRequired();
        await this.requestInfoFromPartyPage.inputDueDate();
        await this.page.getByRole('button', { name: 'Continue' }).click();

        await expect(this.page.locator('form.check-your-answers h2.heading-h2')).toHaveText('Check your answers');
        await this.eventNameAndDescriptionPage.verifyPageContent(requestInfo.requestInfoCaption);
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput,
            eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);
        await this.verifyHistoryTabDetails('Information requested', requestInfo.requestInfoCaption, eventTestData.eventDescriptionInput);
    }

    async verifyCtscAdminWithoutCaseAllocatorRoleCanViewReviewInformationRequestedTask(caseId: string) {

        // Verify CTSC Admin can view the unassigned Review Information Requested task
        await this.performRequestInfoFromPartyEvent(caseId);
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);
    }

    async verifyCtscAdminWithCaseAllocatorRoleCanViewAndAssignReviewInformationRequestedTask(caseId: string) {

        /* Login as CTSC Administrator with case allocator role and view the 
           unassigned Review Information Requested task and assign it to another CTSC Admin */
        await this.loginUserWithCaseId(credentials.amCaseWorkerWithCaseAllocatorRole, false, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPriortiy(task.name, task.priority);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptionsForCaseAllocator);
        await this.tasksTab.assignTaskToCtscUser(task.name, credentials.amCaseWorker.email);
    }

    async verifyCtscAdminAsAnAssignedUserForReviewInformationRequestedTaskCanViewAndCompleteTheTask(caseId: string) {

        // Login as CTSC Administrator and view the unassigned Review Information Requested task
        await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedTo);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptions);
        await this.tasksTab.verifyNextStepsOptions(task.name, task.nextStepsOptions);

        // Select information received next step and complete the event
        await this.tasksTab.clickNextStepLink(task.informationReceived.link);

        let informationReceived = new InformationReceived(this.page)
        await informationReceived.performInformationReceivedEvent();

        // Verify task is removed from the tasks list within Tasks tab
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }

    async verifyReviewInformationRequestedTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId: string) {

        // Verify CTSC Admin with case allocator role can view the unassigned Review Information Requested task
        await this.loginUserWithCaseId(credentials.amCaseWorkerWithCaseAllocatorRole, false, caseId);
        await this.homePage.navigateToTab('Tasks')
        await this.tasksTab.verifyTaskIsDisplayed(task.name);
        await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptionsForCaseAllocator);

        // CTSC Administrator with case allocator role assigns task to another CTSC user
        await this.tasksTab.assignTaskToCtscUser(task.name, credentials.amCaseWorker.email);
        await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedTo);
        await this.tasksTab.verifyManageOptions(task.name, task.assignedManageOptionsForCaseAllocator);

        // CTSC Administrator voids the case
        let voidCase = new VoidCase(this.page);
        await voidCase.performVoidCase(caseId, false);

        // Verify task is removed from the tasks list within Tasks tab
        await this.homePage.navigateToTab('Tasks');
        await this.tasksTab.verifyTaskIsHidden(task.name);
    }
}