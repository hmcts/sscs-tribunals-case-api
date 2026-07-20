import { expect, Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from '../../config/config';
import createCaseBasedOnCaseType from '../../api/client/sscs/factory/appeal.type.factory';
import issueDirectionTestdata from '../../pages/content/issue.direction_en.json';
import eventTestData from '../../pages/content/event.name.event.description_en.json';
import actionFurtherEvidenceTestdata from '../../pages/content/action.further.evidence_en.json';
import sendToInterLocPreValidData from '../../pages/content/send.to.interloc_en.json';

export class IssueDirectionsNotice extends BaseStep {
  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  private async waitForAwaitOtherPartyData() {
    for (let attempt = 0; attempt < 8; attempt++) {
      await this.homePage.navigateToTab('Summary');
      const summaryState = await this.page
        .locator('#summaryState')
        .textContent()
        .catch(() => '');

      if (summaryState?.includes('Await Other Party Data')) {
        return;
      }

      await this.homePage.delay(5000);
      await this.homePage.reloadPage();
    }

    await this.homePage.navigateToTab('Summary');
    await this.summaryTab.verifyPageSectionByKeyValue(
      'Appeal status',
      'Await Other Party Data'
    );
  }

  private async waitForRequestOtherPartyDataHistory() {
    for (let attempt = 0; attempt < 8; attempt++) {
      await this.homePage.navigateToTab('History');
      const historyEvent = await this.page
        .getByRole('link', { name: 'Request other party data' })
        .first()
        .isVisible()
        .catch(() => false);

      if (historyEvent) {
        await this.historyTab.verifyPageContentByKeyValue(
          'End state',
          'Await Other Party Data'
        );
        return;
      }

      await this.homePage.delay(5000);
      await this.homePage.reloadPage();
    }

    await this.homePage.navigateToTab('History');
    await this.historyTab.verifyEventCompleted('Request other party data');
    await this.historyTab.verifyPageContentByKeyValue(
      'End state',
      'Await Other Party Data'
    );
  }

  async validateChildSupportInterlocReviewPreValidAppeal(caseId: string, user) {
    await this.loginUserWithCaseId(user, true, caseId);
    await expect(this.page.locator('#summaryState')).toContainText(
      'Interlocutory Review - Pre-Valid'
    );
    await this.homePage.chooseEvent('Issue directions notice');

    await this.issueDirectionPage.selectHearingOption(
      issueDirectionTestdata.preHearingType
    );
    await this.issueDirectionPage.selectDirectionType(
      issueDirectionTestdata.appealToProceedDirectionType
    );
    await this.issueDirectionPage.chooseRecipients('#confidentialityType-general');
    await this.issueDirectionPage.chooseNoticeType('#generateNotice_Yes');
    await this.issueDirectionPage.enterNoticeContent(true);
    await this.issueDirectionPage.confirmSubmission();
    await this.issueDirectionPage.confirmSubmission();
    await this.issueDirectionPage.confirmSubmission();

    await this.waitForAwaitOtherPartyData();
    await this.waitForRequestOtherPartyDataHistory();
  }

  async performIssueDirectionNoticeIncompleteApplicationPreHearingAppealToProceed() {
    let pipCaseId = await createCaseBasedOnCaseType('PIPINCOMPLETE');
    await new Promise((f) => setTimeout(f, 10000)); //Delay required for the Case to be ready
    /*let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            pipCaseId.trim(), 'dwpUploadResponse', 'dwp');*/

    /*logger.info('The value of the response writer : '+credentials.amCaseWorker.email)
        let caseWorkerToken: string = await accessToken(credentials.amCaseWorker);
        let serviceTokenForCaseWorker: string = await getSSCSServiceToken();
        let caseWorkerId: string = await accessId(credentials.amCaseWorker);
        await new Promise(f => setTimeout(f, 20000)); //Delay required for the Case to be ready
        await performEventOnCaseForActionFurtherEvidence(caseWorkerToken.trim(),
            serviceTokenForCaseWorker.trim(),caseWorkerId.trim(),'SSCS','Benefit',
            taxCreditCaseId.trim(), 'uploadDocumentFurtherEvidence');*/

    //This block would also act as a Test for the Send to interloc - pre-valid Event. SSCSSI-228
    await this.loginUserWithCaseId(credentials.amCaseWorker, false, pipCaseId);
    await this.homePage.chooseEvent('Send to interloc - pre-valid');
    await this.sendToInterlocPrevalidPage.verifyPageContentForTheInterlocReferralPage();
    await this.sendToInterlocPrevalidPage.inputReasonForReferral();
    await this.sendToInterlocPrevalidPage.submitContinueBtn();
    await this.textAreaPage.verifyPageContent(
      sendToInterLocPreValidData.sendToInterLocPreValidCaption,
      sendToInterLocPreValidData.appealNotepad,
      sendToInterLocPreValidData.enterNoteOptionalLabel
    );
    await this.textAreaPage.inputData(
      sendToInterLocPreValidData.appealNotepadInput
    );
    await this.sendToInterlocPrevalidPage.confirmSubmission();
    await this.eventNameAndDescriptionPage.verifyPageContent(
      sendToInterLocPreValidData.sendToInterLocPreValidCaption,
      false
    );
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await this.verifyHistoryTabDetails('Send to interloc - pre-valid');
    await this.homePage.signOut();

    await this.loginUserWithCaseId(credentials.judge, false, pipCaseId);
    await this.homePage.chooseEvent('Issue directions notice');

    await this.issueDirectionPage.verifyPageContent();
    await this.issueDirectionPage.populatePreHearingAppealToProceed(
      issueDirectionTestdata.preHearingType,
      'Appeal to Proceed',
      issueDirectionTestdata.docTitle
    );

    await this.eventNameAndDescriptionPage.verifyPageContent(
      'Issue directions notice',
      true,
      'Direction type'
    );
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await this.verifyHistoryTabDetails('Issue directions notice');
    // await performAppealDormantOnCase(pipCaseId);
  }

  async performIssueDirectionNoticePreHearingAppealToProceed() {
    let taxCreditCaseId = await createCaseBasedOnCaseType('TAX CREDIT');
    await new Promise((f) => setTimeout(f, 30000)); //Delay required for the Case to be ready
    /* let responseWriterToken: string = await accessToken(credentials.hmrcUser);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.hmrcUser);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            taxCreditCaseId.trim(), 'dwpUploadResponse', 'hmrc'); */

    /*logger.info('The value of the response writer : '+credentials.amCaseWorker.email)
        let caseWorkerToken: string = await accessToken(credentials.amCaseWorker);
        let serviceTokenForCaseWorker: string = await getSSCSServiceToken();
        let caseWorkerId: string = await accessId(credentials.amCaseWorker);
        await new Promise(f => setTimeout(f, 20000)); //Delay required for the Case to be ready
        await performEventOnCaseForActionFurtherEvidence(caseWorkerToken.trim(),
            serviceTokenForCaseWorker.trim(),caseWorkerId.trim(),'SSCS','Benefit',
            taxCreditCaseId.trim(), 'uploadDocumentFurtherEvidence');*/

    await this.loginUserWithCaseId(
      credentials.amCaseWorker,
      false,
      taxCreditCaseId
    );
    await this.homePage.chooseEvent(
      actionFurtherEvidenceTestdata.eventNameCaptor
    );
    await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
      actionFurtherEvidenceTestdata.sender,
      actionFurtherEvidenceTestdata.other,
      actionFurtherEvidenceTestdata.testfileone
    );
    await this.eventNameAndDescriptionPage.verifyPageContent(
      actionFurtherEvidenceTestdata.eventName
    );
    await this.eventNameAndDescriptionPage.confirmAndSignOut();

    await this.loginUserWithCaseId(credentials.judge, false, taxCreditCaseId);
    await this.homePage.chooseEvent('Issue directions notice');

    await this.issueDirectionPage.verifyPageContent();
    await this.issueDirectionPage.populatePreHearingAppealToProceed(
      issueDirectionTestdata.preHearingType,
      'Appeal to Proceed',
      issueDirectionTestdata.docTitle
    );

    await this.eventNameAndDescriptionPage.verifyPageContent(
      'Issue directions notice',
      true,
      'Direction type'
    );
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await this.verifyHistoryTabDetails('Issue directions notice');
    // await performAppealDormantOnCase(taxCreditCaseId);
  }

  async performIssueDirectionNoticePostHearingESAAppealToProceed() {
    //let esaCaseId = await createCaseBasedOnCaseType('ESA');
    let esaCaseId = await createCaseBasedOnCaseType('PIP');
    await new Promise((f) => setTimeout(f, 30000)); //Delay required for the Case to be ready
    /*let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            esaCaseId.trim(), 'dwpUploadResponse', 'dwp');*/

    /*logger.info('The value of the response writer : '+credentials.amCaseWorker.email)
        let caseWorkerToken: string = await accessToken(credentials.amCaseWorker);
        let serviceTokenForCaseWorker: string = await getSSCSServiceToken();
        let caseWorkerId: string = await accessId(credentials.amCaseWorker);
        await new Promise(f => setTimeout(f, 20000)); //Delay required for the Case to be ready
        await performEventOnCaseForActionFurtherEvidence(caseWorkerToken.trim(),
            serviceTokenForCaseWorker.trim(),caseWorkerId.trim(),'SSCS','Benefit',
            taxCreditCaseId.trim(), 'uploadDocumentFurtherEvidence');*/

    await this.loginUserWithCaseId(credentials.amCaseWorker, false, esaCaseId);
    await this.homePage.chooseEvent(
      actionFurtherEvidenceTestdata.eventNameCaptor
    );
    await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
      actionFurtherEvidenceTestdata.sender,
      actionFurtherEvidenceTestdata.other,
      actionFurtherEvidenceTestdata.testfileone
    );
    await this.eventNameAndDescriptionPage.verifyPageContent(
      actionFurtherEvidenceTestdata.eventName
    );
    await this.eventNameAndDescriptionPage.confirmAndSignOut();

    await this.loginUserWithCaseId(credentials.judge, false, esaCaseId);
    await this.homePage.chooseEvent('Issue directions notice');

    await this.issueDirectionPage.verifyPageContent();
    await this.issueDirectionPage.populatePostHearingESAAppealToProceed(
      issueDirectionTestdata.postHearingType,
      'Appeal to Proceed',
      issueDirectionTestdata.docTitle
    );

    await this.eventNameAndDescriptionPage.verifyPageContent(
      'Issue directions notice',
      true,
      'Direction type'
    );
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await this.verifyHistoryTabDetails('Issue directions notice');
    // await performAppealDormantOnCase(esaCaseId);
  }

  async performIssueDirectionNoticePostHearingDLAAppealToProceed() {
    //let pipCaseId = await createCaseBasedOnCaseType('DLASANDL');
    let pipCaseId = await createCaseBasedOnCaseType('PIP');
    await new Promise((f) => setTimeout(f, 30000)); //Delay required for the Case to be ready
    /*let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            pipCaseId.trim(), 'dwpUploadResponse', 'dwp');*/

    /*logger.info('The value of the response writer : '+credentials.amCaseWorker.email)
        let caseWorkerToken: string = await accessToken(credentials.amCaseWorker);
        let serviceTokenForCaseWorker: string = await getSSCSServiceToken();
        let caseWorkerId: string = await accessId(credentials.amCaseWorker);
        await new Promise(f => setTimeout(f, 20000)); //Delay required for the Case to be ready
        await performEventOnCaseForActionFurtherEvidence(caseWorkerToken.trim(),
            serviceTokenForCaseWorker.trim(),caseWorkerId.trim(),'SSCS','Benefit',
            taxCreditCaseId.trim(), 'uploadDocumentFurtherEvidence');*/

    await this.loginUserWithCaseId(credentials.amCaseWorker, false, pipCaseId);
    await this.homePage.chooseEvent(
      actionFurtherEvidenceTestdata.eventNameCaptor
    );
    await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
      actionFurtherEvidenceTestdata.sender,
      actionFurtherEvidenceTestdata.other,
      actionFurtherEvidenceTestdata.testfileone
    );
    await this.eventNameAndDescriptionPage.verifyPageContent(
      actionFurtherEvidenceTestdata.eventName
    );
    await this.eventNameAndDescriptionPage.confirmAndSignOut();

    await this.loginUserWithCaseId(credentials.judge, false, pipCaseId);
    await this.homePage.chooseEvent('Issue directions notice');

    await this.issueDirectionPage.verifyPageContent();
    await this.issueDirectionPage.populatePostHearingDLAProvideInformation(
      issueDirectionTestdata.postHearingType,
      'Provide information',
      issueDirectionTestdata.docTitle
    );
    await this.eventNameAndDescriptionPage.verifyPageContent(
      'Issue directions notice',
      true,
      'Direction type'
    );
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await this.verifyHistoryTabDetails('Issue directions notice');
  }

  async performIssueDirectionErrorMessages() {
    //let pipCaseId = await createCaseBasedOnCaseType('DLASANDL');
    let pipCaseId = await createCaseBasedOnCaseType('PIP');
    await new Promise((f) => setTimeout(f, 30000)); //Delay required for the Case to be ready
    /*let responseWriterToken: string = await accessToken(credentials.dwpResponseWriter);
        let serviceToken: string = await getSSCSServiceToken();
        let responseWriterId: string = await accessId(credentials.dwpResponseWriter);
        await performEventOnCaseWithUploadResponse(responseWriterToken.trim(),
            serviceToken.trim(), responseWriterId.trim(),
            'SSCS', 'Benefit',
            pipCaseId.trim(), 'dwpUploadResponse', 'dwp');*/

    /*logger.info('The value of the response writer : '+credentials.amCaseWorker.email)
        let caseWorkerToken: string = await accessToken(credentials.amCaseWorker);
        let serviceTokenForCaseWorker: string = await getSSCSServiceToken();
        let caseWorkerId: string = await accessId(credentials.amCaseWorker);
        await new Promise(f => setTimeout(f, 20000)); //Delay required for the Case to be ready
        await performEventOnCaseForActionFurtherEvidence(caseWorkerToken.trim(),
            serviceTokenForCaseWorker.trim(),caseWorkerId.trim(),'SSCS','Benefit',
            taxCreditCaseId.trim(), 'uploadDocumentFurtherEvidence');*/

    await this.loginUserWithCaseId(credentials.amCaseWorker, false, pipCaseId);
    await this.homePage.chooseEvent(
      actionFurtherEvidenceTestdata.eventNameCaptor
    );
    await this.actionFurtherEvidencePage.submitActionFurtherEvidence(
      actionFurtherEvidenceTestdata.sender,
      actionFurtherEvidenceTestdata.other,
      actionFurtherEvidenceTestdata.testfileone
    );
    await this.eventNameAndDescriptionPage.verifyPageContent(
      actionFurtherEvidenceTestdata.eventName
    );
    await this.eventNameAndDescriptionPage.confirmAndSignOut();

    await this.loginUserWithCaseId(credentials.judge, false, pipCaseId);
    await this.homePage.chooseEvent('Issue directions notice');
    await this.issueDirectionPage.verifyPageContent();
    await this.issueDirectionPage.confirmSubmission();

    await this.issueDirectionPage.verifyErrorMsg(true, false, false);
    await this.issueDirectionPage.chooseRecipients(
      '#confidentialityType-confidential'
    );
    await this.issueDirectionPage.waitForSpecificRecipientOptions();
    await this.issueDirectionPage.confirmSubmission();
    await this.issueDirectionPage.verifyErrorMsg(false, true, false);

    await this.issueDirectionPage.chooseNoticeType('#generateNotice_Yes');
    await this.issueDirectionPage.confirmSubmission();
    await this.issueDirectionPage.verifyErrorMsg(false, false, true);
  }

  async performIssueDirectionNoticeDirectionHearing() {
    await this.homePage.chooseEvent('Issue directions notice');

    await this.issueDirectionPage.verifyPageContent();
    await this.issueDirectionPage.submitIssueDirectionDirectionHearing(
      issueDirectionTestdata.preHearingType,
      'Provide information',
      issueDirectionTestdata.docTitle,
      true
    );
    await this.eventNameAndDescriptionPage.verifyCyaPageContent(
      'Issue directions notice',
      [
        'Direction type',
        'Do you want to select next hearing type i.e. Substantive or Directions hearing',
        'Next hearing type'
      ],
      ['Provide information', 'Yes', 'Direction']
    );
    await this.eventNameAndDescriptionPage.inputData(
      eventTestData.eventSummaryInput,
      eventTestData.eventDescriptionInput
    );
    await this.eventNameAndDescriptionPage.confirmSubmission();
    await this.verifyHistoryTabDetails('Issue directions notice');
  }
}
