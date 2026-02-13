import { Page } from '@playwright/test';
import { HomePage } from '../pages/common/homePage';
import { UploadResponsePage } from '../pages/upload.response.page';
import { Hearings } from '../pages/tabs/hearings';
import { ActionFurtherEvidencePage } from '../pages/action.further.evidence.page';
import { EventNameEventDescriptionPage } from '../pages/common/event.name.event.description';
import { Summary } from '../pages/tabs/summary';


const actionFurtherEvidenceTestdata = require('../pages/content/action.further.evidence_en.json');

export class StepsHelper {
  readonly page: Page;
  public homePage: HomePage;
  public uploadResponsePage: UploadResponsePage;
  public hearingsTab: Hearings;
  public actionFurtherEvidencePage: ActionFurtherEvidencePage;
  public eventNameAndDescriptionPage: EventNameEventDescriptionPage;
  public summaryTab: Summary;

  constructor(page: Page) {
    this.page = page;
    this.homePage = new HomePage(this.page);
    this.uploadResponsePage = new UploadResponsePage(this.page);
    this.hearingsTab = new Hearings(this.page);
    this.actionFurtherEvidencePage = new ActionFurtherEvidencePage(this.page);
    this.eventNameAndDescriptionPage = new EventNameEventDescriptionPage(this.page);
    this.summaryTab = new Summary(this.page);
  }

  async uploadResponseHelper(
    issueCodeData: string,
    assistOption: string,
    phmeFlag?: boolean,
    ucbFlag?: boolean,
    avFlag?: boolean
  ) {
    await this.homePage.chooseEvent('Upload response');
    await this.homePage.delay(4000);

    await this.uploadResponsePage.verifyPageContent();
    await this.uploadResponsePage.uploadDocs();
    if (phmeFlag) await this.uploadResponsePage.uploadPHEDocs();
    if (avFlag) await this.uploadResponsePage.uploadAVDocs();
    if (ucbFlag) await this.uploadResponsePage.uploadUCBDocs();
    await this.uploadResponsePage.selectIssueCode(issueCodeData);
    await this.uploadResponsePage.chooseAssistOption(assistOption);
    await this.uploadResponsePage.continueSubmission(assistOption);
  }

  async verifyHearingHelper() {
    await new Promise((f) => setTimeout(f, 5000));
    await this.homePage.navigateToTab('Hearings');
    await this.hearingsTab.verifyHearingStatusSummary(false);
  }

  async setCaseAsUrgentHelper() {

    // await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
    await this.homePage.chooseEvent(actionFurtherEvidenceTestdata.eventName);
    await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
      actionFurtherEvidenceTestdata.sender,
      actionFurtherEvidenceTestdata.urgentDocType,
      actionFurtherEvidenceTestdata.testfileone
    );
    await this.eventNameAndDescriptionPage.verifyPageContent(
      actionFurtherEvidenceTestdata.eventName
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();

    await this.summaryTab.verifyPageContentByKeyValue('Urgent case', 'Yes');
    await this.homePage.signOut();
  }
}
