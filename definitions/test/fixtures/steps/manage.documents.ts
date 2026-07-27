import { BaseStep } from './base';
import { expect, Page } from '@playwright/test';
import dateUtilsComponent from '../../utils/DateUtilsComponent';

export class ManageDocuments extends BaseStep {
  readonly page: Page;

  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  async uploadDocumentToTab(tab: string, type: string, filename: string) {
    await this.triggerManageDocumentsEvent();
    await this.uploadRemoveOrMoveDocumentPage.verifyUploadRemoveOrMovePage();
    await this.uploadRemoveOrMoveDocumentPage.selectUploadRemoveDocument();
    await this.uploadToRemoveFromTabPage.verifyUploadRemovePage();
    if (tab === 'Documents') {
      await this.uploadToRemoveFromTabPage.selectDocumentsTab();
    } else if (tab === 'Tribunal Internal Documents') {
      await this.uploadToRemoveFromTabPage.selectInternalDocumentsTab();
    }
    await this.uploadToRemoveFromDocumentsPage.verifyUploadRemoveDocumentsPage();
    await this.uploadToRemoveFromDocumentsPage.addNewDocument(type, filename);
    await this.uploadToRemoveFromDocumentsPage.confirmSubmission();
    await this.eventNameAndDescriptionPage.confirmSubmission();
  }

  async removeDocumentFromTab(tab: string, filename: string) {
    await this.triggerManageDocumentsEvent();
    await this.uploadRemoveOrMoveDocumentPage.verifyUploadRemoveOrMovePage();
    await this.uploadRemoveOrMoveDocumentPage.selectUploadRemoveDocument();
    await this.uploadToRemoveFromTabPage.verifyUploadRemovePage();
    if (tab === 'Documents') {
      await this.uploadToRemoveFromTabPage.selectDocumentsTab();
    } else if (tab === 'Tribunal Internal Documents') {
      await this.uploadToRemoveFromTabPage.selectInternalDocumentsTab();
    }
    await this.uploadToRemoveFromDocumentsPage.verifyUploadRemoveDocumentsPage();
    await this.uploadToRemoveFromDocumentsPage.removeDocument(tab, filename);
    await this.uploadToRemoveFromDocumentsPage.confirmSubmission();
    await this.eventNameAndDescriptionPage.confirmSubmission();
  }

  async moveDocumentTo(to: string, filename: string) {
    await this.triggerManageDocumentsEvent();
    await this.uploadRemoveOrMoveDocumentPage.verifyUploadRemoveOrMovePage();
    await this.uploadRemoveOrMoveDocumentPage.selectMoveDocument();
    await this.moveToTabPage.verifyMovePage();
    if (to === 'Documents') {
      await this.moveToTabPage.selectDocumentsTab();
    } else if (to === 'Tribunal Internal Documents') {
      await this.moveToTabPage.selectInternalDocumentsTab();
    }
    await this.moveDocumentsPage.verifyMoveDocumentsPage();
    if (to === 'Documents') {
      await this.moveDocumentsPage.verifyIssuedQuestion();
      await this.moveDocumentsPage.verifyMoveToDocumentsChoices(filename);
      await this.moveDocumentsPage.selectInternalDocumentToMove(filename);
      await this.moveDocumentsPage.selectNotIssued();
    } else if (to === 'Tribunal Internal Documents') {
      await this.moveDocumentsPage.verifyMoveToInternalDocumentsChoices(
        filename
      );
      await this.moveDocumentsPage.selectDocumentToMove(filename);
    }
    await this.moveDocumentsPage.confirmSubmission();
    await this.eventNameAndDescriptionPage.confirmSubmission();
  }

  async moveInternalDocumentNoneFoundErrorCheck() {
    await this.triggerManageDocumentsEvent();
    await this.uploadRemoveOrMoveDocumentPage.verifyUploadRemoveOrMovePage();
    await this.uploadRemoveOrMoveDocumentPage.selectMoveDocument();
    await this.moveToTabPage.verifyMovePage();
    await this.moveToTabPage.selectDocumentsTab();
    await this.moveToTabPage.verifyErrorNoInternalDocuments();
    await this.moveToTabPage.page.getByRole('button', {name: 'Cancel', exact: true}).click();
  }

  async moveDocumentNoneFoundErrorCheck() {
    await this.triggerManageDocumentsEvent();
    await this.uploadRemoveOrMoveDocumentPage.verifyUploadRemoveOrMovePage();
    await this.uploadRemoveOrMoveDocumentPage.selectMoveDocument();
    await this.moveToTabPage.verifyMovePage();
    await this.moveToTabPage.selectInternalDocumentsTab();
    await this.moveToTabPage.verifyErrorNoDocuments();
    await this.moveToTabPage.page.getByRole('button', {name: 'Cancel', exact: true}).click();
  }

  async moveDocumentNoneSelectedErrorChecks(filename: string) {
    await this.triggerManageDocumentsEvent();
    await this.uploadRemoveOrMoveDocumentPage.verifyUploadRemoveOrMovePage();
    await this.uploadRemoveOrMoveDocumentPage.selectMoveDocument();
    await this.moveToTabPage.verifyMovePage();
    await this.moveToTabPage.selectDocumentsTab();
    await this.moveDocumentsPage.verifyMoveDocumentsPage();
    await this.moveDocumentsPage.verifyIssuedQuestion();
    await this.moveDocumentsPage.verifyMoveToDocumentsChoices(filename);
    await this.moveDocumentsPage.verifyDocumentIssuedError();
    await this.moveDocumentsPage.selectNotIssued();
    await this.moveDocumentsPage.submit();
    await this.moveDocumentsPage.verifyNoDocumentsError();
  }

  async moveDocumentToDocumentsWithIssue(filename: string) {
    await this.triggerManageDocumentsEvent();
    await this.uploadRemoveOrMoveDocumentPage.verifyUploadRemoveOrMovePage();
    await this.uploadRemoveOrMoveDocumentPage.selectMoveDocument();
    await this.moveToTabPage.verifyMovePage();
    await this.moveToTabPage.selectDocumentsTab();
    await this.moveDocumentsPage.verifyMoveDocumentsPage();
    await this.moveDocumentsPage.verifyIssuedQuestion();
    await this.moveDocumentsPage.verifyMoveToDocumentsChoices(filename);
    await this.moveDocumentsPage.selectInternalDocumentToMove(filename);
    await this.moveDocumentsPage.selectIssued();
    await this.moveDocumentsPage.submit();
    await this.eventNameAndDescriptionPage.confirmSubmission();
  }

  async verifyFileNotInTab(tab: string, type: string, filename: string) {
    await this.homePage.navigateToTab(tab);
    await this.documentsTab.verifyPageContentNotPresentByKeyValue('Type', type);
    await this.documentsTab.verifyPageContentNotPresentByKeyValue(
      'Original document URL',
      filename
    );
  }

  async verifyFileInTab(tab: string, type: string, filename: string) {
    await this.homePage.navigateToTab(tab);
    await this.documentsTab.verifyPageContentByKeyValue('Type', type);
    await this.documentsTab.verifyPageContentByKeyValue(
      'Original document URL',
      filename
    );
  }

  async triggerManageDocumentsEvent() {
    await this.homePage.chooseEvent('Manage documents');
  }

  async verifyInternalDocumentsTabHidden() {
    await expect(this.homePage.internalDocumentsTab).toBeHidden();
  }

  async verifyIssuedFileInDocumentsTab(type: string, filename: string) {
    await this.homePage.navigateToTab('Documents');
    await this.documentsTab.verifyPageContentByKeyValue('Type', type);
    await this.documentsTab.verifyPageContentByKeyValue(
      'Original document URL',
      filename
    );
    let date = dateUtilsComponent.formatDateToSpecifiedDateNumberFormat(
      new Date()
    );
    await this.documentsTab.verifyPageContentByKeyValue(
      'File name',
      'Addition A - Other document issued on ' + date + '.pdf'
    );
    await this.documentsTab.verifyPageContentByKeyValue(
      'Evidence issued',
      'Yes'
    );
    await this.documentsTab.verifyPageContentByKeyValue('Bundle addition', 'A');
  }
}
