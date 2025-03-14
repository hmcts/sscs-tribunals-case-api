import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';
import { credentials } from '../config/config';

let caseId: string;

test.describe(
  'Manage documents tests',
  { tag: '@test-pipeline' },
  async () => {
    test('Upload/remove documents from documents tab', async ({
      manageDocumentsSteps
    }) => {
      caseId = await createCaseBasedOnCaseType('PIPREPSANDL');
      await manageDocumentsSteps.fastLoginUserWithCaseId(
        credentials.caseWorker,
        caseId
      );
      let documentType = 'Other document';
      let fileName = 'testfile1.pdf';
      let tab = 'Documents';
      await manageDocumentsSteps.verifyFileNotInTab(
        tab,
        documentType,
        fileName
      );
      await manageDocumentsSteps.uploadDocumentToTab(
        tab,
        documentType,
        fileName
      );
      await manageDocumentsSteps.verifyFileInTab(tab, documentType, fileName);
      await manageDocumentsSteps.removeDocumentFromTab(tab, documentType);
      await manageDocumentsSteps.verifyFileNotInTab(
        tab,
        documentType,
        fileName
      );
    });

    test('Upload/remove documents from internal tribunal documents tab', async ({
      manageDocumentsSteps
    }) => {
      caseId = await createCaseBasedOnCaseType('PIPREPSANDL');
      await manageDocumentsSteps.fastLoginUserWithCaseId(
        credentials.caseWorker,
        caseId
      );
      let documentType = 'Other document';
      let fileName = 'testfile1.pdf';
      let tab = 'Tribunal Internal Documents';
      await manageDocumentsSteps.verifyInternalDocumentsTabHidden();
      await manageDocumentsSteps.uploadDocumentToTab(
        tab,
        documentType,
        fileName
      );
      await manageDocumentsSteps.verifyFileInTab(tab, documentType, fileName);
      await manageDocumentsSteps.signOut();
      await manageDocumentsSteps.fastLoginUserWithCaseId(
        credentials.dwpResponseWriter,
        caseId
      );
      await manageDocumentsSteps.verifyInternalDocumentsTabHidden();
      await manageDocumentsSteps.signOut();
      await manageDocumentsSteps.fastLoginUserWithCaseId(
        credentials.caseWorker,
        caseId
      );
      await manageDocumentsSteps.verifyFileInTab(tab, documentType, fileName);
      await manageDocumentsSteps.removeDocumentFromTab(tab, documentType);
      await manageDocumentsSteps.verifyInternalDocumentsTabHidden();
    });

    test('Move documents to and from internal tribunal documents', async ({
      manageDocumentsSteps
    }) => {
      caseId = await createCaseBasedOnCaseType('PIPREPSANDL');
      await manageDocumentsSteps.fastLoginUserWithCaseId(
        credentials.caseWorker,
        caseId
      );
      let documentType = 'SSCS1';
      let fileName = 'BloggsPIP_RFQ.pdf';
      await manageDocumentsSteps.verifyFileInTab(
        'Documents',
        documentType,
        fileName
      );
      await manageDocumentsSteps.moveDocumentTo(
        'Tribunal Internal Documents',
        fileName
      );
      await manageDocumentsSteps.verifyFileInTab(
        'Tribunal Internal Documents',
        documentType,
        fileName
      );
      await manageDocumentsSteps.moveDocumentTo('Documents', fileName);
      await manageDocumentsSteps.verifyFileInTab(
        'Documents',
        documentType,
        fileName
      );
      await manageDocumentsSteps.verifyInternalDocumentsTabHidden();
    });

    test('Move document from internal tribunal documents to documents with issue', async ({
      manageDocumentsSteps
    }) => {
      caseId = await createCaseBasedOnCaseType('PIPREPSANDL');
      await manageDocumentsSteps.fastLoginUserWithCaseId(
        credentials.caseWorker,
        caseId
      );
      let documentType = 'Other document';
      let fileName = 'testfile1.pdf';
      let tab = 'Tribunal Internal Documents';
      await manageDocumentsSteps.uploadDocumentToTab(
        tab,
        documentType,
        fileName
      );
      await manageDocumentsSteps.verifyFileInTab(tab, documentType, fileName);
      await manageDocumentsSteps.moveDocumentToDocumentsWithIssue(fileName);
      await manageDocumentsSteps.verifyIssuedFileInDocumentsTab(
        documentType,
        fileName
      );
    });
  }
);
