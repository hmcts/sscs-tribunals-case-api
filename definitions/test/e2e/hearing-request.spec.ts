import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';
import { credentials } from '../config/config';

let caseId: string;

test.describe(
  'Create a new hearing for an List assist case',
  { tag: '@nightly-pipeline' },
  () => {
    test.beforeEach(
      'Check HMC environment',
      async ({ uploadResponseSteps, request }) => {
        await uploadResponseSteps.checkHmcEnvironment(request);
      }
    );

    test.afterEach(
      'Cancel the hearings after test run',
      async({ hearingSteps }, testInfo) => {
        // eslint-disable-next-line @typescript-eslint/no-unused-expressions
        (testInfo.title.includes("#executeTearDown")) && await hearingSteps.cancelHearingForCleanUp();
      }
    )

    test(
      'Trigger a new hearing & cancellation for DLA case',
      { tag: '@aat-regression' },
      async ({ uploadResponseSteps, hearingSteps }) => {
        caseId = await createCaseBasedOnCaseType('DLASANDL');
        await uploadResponseSteps.performUploadResponse(caseId, 'dla');
        await hearingSteps.verifyHearingIsTriggered(caseId, 'dla');
        await hearingSteps.verifyManualHearingCancellation();
      }
    );

    test('Trigger a new hearing for UC case #executeTearDown', async ({
      uploadResponseSteps,
      hearingSteps
    }) => {
      caseId = await createCaseBasedOnCaseType('UCSANDL');
      await uploadResponseSteps.performUploadResponseOnAUniversalCredit(caseId);
      await hearingSteps.verifyHearingIsTriggeredForUCCase(false);
    });

    test.skip('Trigger a new direction hearing via issue direction notice for UC case #executeTearDown', async ({
      uploadResponseSteps,
      hearingSteps,
      issueDirectionsNoticeSteps
    }) => {
      caseId = await createCaseBasedOnCaseType('UCSANDL');
      await issueDirectionsNoticeSteps.loginUserWithCaseId(
        credentials.hmrcSuperUser,
        false,
        caseId
      );
      await issueDirectionsNoticeSteps.performIssueDirectionNoticeDirectionHearing();
      await uploadResponseSteps.performUploadResponseOnAUniversalCredit(
        caseId,
        false
      );
      await hearingSteps.verifyHearingIsTriggeredForUCCase(true);
    });

    test.skip('Trigger a new direction hearing via update listing reqs for UC case #executeTearDown', async ({
      uploadResponseSteps,
      hearingSteps
    }) => {
      caseId = await createCaseBasedOnCaseType('UCSANDL');
      await hearingSteps.loginUserWithCaseId(
        credentials.hmrcSuperUser,
        false,
        caseId
      );
      await hearingSteps.updateHearingToDirectionViaEvent();
      await uploadResponseSteps.performUploadResponseOnAUniversalCredit(
        caseId,
        false
      );
      await hearingSteps.verifyHearingIsTriggeredForUCCase(true);
    });

    test('Trigger a new hearing for PIP case', async ({
      uploadResponseSteps,
      hearingSteps,
      voidCaseSteps
    }) => {
      caseId = await createCaseBasedOnCaseType('PIPREPSANDL');
      await uploadResponseSteps.performUploadResponse(caseId, 'pip');
      await hearingSteps.verifyHearingIsTriggered(caseId, 'pip');
      await voidCaseSteps.performVoidCase(caseId, false);
      await hearingSteps.verifyAutoHearingCancellation();
    });

    test('Manually Update an hearing for DLA case #executeTearDown', async ({
      uploadResponseSteps,
      hearingSteps
    }) => {
      caseId = await createCaseBasedOnCaseType('DLASANDL');
      await uploadResponseSteps.performUploadResponse(caseId, 'dla');
      await hearingSteps.verifyHearingIsTriggered(caseId, 'dla');
      await hearingSteps.updateHearingLengthManually();
      await hearingSteps.verifyUpdatedHearingStatus();
    });

    test('Auto Update an hearing for DLA case #executeTearDown', async ({
      uploadResponseSteps,
      hearingSteps
    }) => {
      test.slow();
      caseId = await createCaseBasedOnCaseType('DLASANDL');
      await uploadResponseSteps.performUploadResponse(caseId, 'dla');
      await hearingSteps.verifyHearingIsTriggered(caseId, 'dla');
      await hearingSteps.setAutolist(true);
      await hearingSteps.updateHearingViaEvent();
      try {
        await hearingSteps.verifyUpdatedHearingStatusViaEvent();
      } catch {
        await hearingSteps.updateListingRequirementsNoChange();
        await hearingSteps.verifyUpdatedHearingStatusViaEvent();
      }
    });
  }
);
