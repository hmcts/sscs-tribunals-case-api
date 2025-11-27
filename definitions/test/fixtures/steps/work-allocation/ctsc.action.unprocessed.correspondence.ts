import { Page, expect } from '@playwright/test';
import { BaseStep } from '../base';
import { credentials } from '../../../config/config';
import task from '../../../pages/content/action.unprocessed.correspondence.task_en.json';
import actionFurtherEvidence from '../../../pages/content/action.further.evidence_en.json';


export class CtscActionUnprocessedCorrespondence extends BaseStep {
  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  async createActionUnprocessedCorrespondenceTask(caseId: string) {
    await this.loginUserWithCaseId(credentials.amSuperUser, false, caseId);
    await this.homePage.chooseEvent('Upload document FE');
    await this.uploadDocumentFurtherEvidencePage.verifyPageContent();
    await this.uploadDocumentFurtherEvidencePage.clickAddNew();
    await this.uploadDocumentFurtherEvidencePage.selectDocumenType('Other evidence');
    await this.uploadDocumentFurtherEvidencePage.inputFilename('Unprocessed Correspondence Test Auto File');
    await this.uploadDocumentFurtherEvidencePage.uploadFurtherEvidenceDoc('testfile1.pdf');
    await this.uploadDocumentFurtherEvidencePage.confirmSubmission();
    await this.uploadDocumentFurtherEvidencePage.confirmSubmission();
    await this.homePage.signOut();
  }

  async createMultipleActionUnprocessedCorrespondenceTasks(caseId: string, numberOfTasksToBeCreated = 3) {
    await this.loginUserWithCaseId(credentials.amSuperUser, false, caseId);
    for (let i = 0; i < numberOfTasksToBeCreated; i++) {
      await this.homePage.chooseEvent('Upload document FE');
      await this.uploadDocumentFurtherEvidencePage.verifyPageContent();
      await this.uploadDocumentFurtherEvidencePage.clickAddNew();
      await this.uploadDocumentFurtherEvidencePage.selectDocumenType('Other evidence');
      await this.uploadDocumentFurtherEvidencePage.inputFilename(`Unprocessed Correspondence Test Auto File ${i + 1}`);
      await this.uploadDocumentFurtherEvidencePage.uploadFurtherEvidenceDoc('testfile1.pdf');
      await this.uploadDocumentFurtherEvidencePage.confirmSubmission();
      await this.uploadDocumentFurtherEvidencePage.confirmSubmission();
    }
    await this.homePage.signOut();
  }

  async verifyCtscAdminWithoutCaseAllocatorRoleCanViewActionUnprocessedCorrespondenceTask(caseId: string) {
    await this.loginUserWithCaseId(credentials.amCtscAdminNwLiverpool, true, caseId);
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.verifyPriortiy(task.name, task.priority);
    await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
    await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptions);
  }

  async assignTaskToSelf(caseId: string) {
    await this.loginUserWithCaseId(credentials.amCtscTeamLeaderNwLiverpool, false, caseId);
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.selfAssignTask(task.name);
    await this.tasksTab.verifyTaskIsAssigned(task.name);
    await this.homePage.navigateToMyWork();
    await this.myWorkPage.verifyTaskAssignedToMe();
  }

  async completeActionUnprocessedCorrespondenceTask(caseId: string) {
    await this.loginPage.goToCase(caseId)
    await this.homePage.navigateToTab('Tasks');
    await this.tasksTab.clickNextStepLink(task.actionFurtherEvidence.link);
    await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
      actionFurtherEvidence.sender, 
      actionFurtherEvidence.otherDocType,
      actionFurtherEvidence.testfileone
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await this.verifyUnprocessedCorrespondenceTabNotVisible();
    
  }

  async verifyActionUnprocessedCorrespondenceTaskCompleted(caseId: string) {
    await this.homePage.navigateToTab('Tasks');
    await this.homePage.navigateToMyWork();
    await this.myWorkPage.verifyNoAssignedTasks();
  }

  async verifyCtscAdminWithCaseAllocatorRoleCanViewAndAssignActionUnprocessedCorrespondence(caseId: string) {
    await this.loginUserWithCaseId(credentials.amCtscTeamLeaderNwLiverpool, true, caseId);
    await this.homePage.navigateToTab('Tasks')
    await this.tasksTab.verifyTaskIsDisplayed(task.name);
    await this.tasksTab.verifyPriortiy(task.name, task.priority);
    await this.tasksTab.verifyPageContentByKeyValue(task.name, 'Assigned to', task.assignedToWhenNotAssigned);
    await this.tasksTab.verifyManageOptions(task.name, task.unassignedManageOptionsForCaseAllocator);
    await this.tasksTab.assignTaskToCtscUser(task.name, credentials.amCtscAdminNwLiverpool.email);
    await this.signOut();
    await this.loginUserWithCaseId(credentials.amCtscAdminNwLiverpool, false, caseId);
    await this.homePage.navigateToMyWork();
    await this.myWorkPage.verifyTaskAssignedToMe("Joe Bloggs", "Personal Independence Payment", "CARDIFF", "CTSC - Action Unprocessed Correspondence", "high");
  }

  async verifyUnprocessedCorrespondenceTabNotVisible() {
     await expect(this.homePage.unprocessedCorrespondenceTab).toBeHidden();
      }
}
