import _ from "lodash";
import { credentials } from "../../config/config";
import { test } from "../../lib/steps.factory";
import ibcData from "../../pages/content/iba/iba.casedata_en.json";


test.describe.configure({ mode: 'serial' });

test.describe("Create IBA Paper Scottish case appeal and perform few events", { tag: '@ibc' }, async () => {

  let ibcCaseId: string;

  test("Create IBA Paper case", async ({ createUpdateToCaseDataSteps, }) => {
    test.slow();
    const createData = ibcData.createCaseData;
    createData.hearingType = 'Paper';
    await createUpdateToCaseDataSteps.loginUserWithoutCaseId(credentials.amCaseWorker, false);

    ibcCaseId = await createUpdateToCaseDataSteps.createIBCcase(createData);
    await createUpdateToCaseDataSteps.verifyAppealDetailsTab(createData);
    await createUpdateToCaseDataSteps.verifySummaryTab(createData);
  });

  test("Update to case data for IBA Paper case", async ({ createUpdateToCaseDataSteps, }) => {
    test.slow();
    const updateData = _.merge(ibcData.createCaseData, ibcData.updateCaseData);
    updateData.hearingOptions.isAttending = 'No';
    await createUpdateToCaseDataSteps.loginUserWithCaseId(credentials.amCaseWorker, false, ibcCaseId);
    await createUpdateToCaseDataSteps.updateToCaseDataEvent(ibcCaseId, updateData);
    await createUpdateToCaseDataSteps.verifyAppealDetailsTab(updateData);
    await createUpdateToCaseDataSteps.verifySummaryTab(updateData);
  });

  test("Generate appeal PDF for IBA Paper case", async ({ generateAppealPdfSteps, }) => {
    test.slow();
    const appellantDetails = ibcData.createCaseData.appellantDetails;
    await generateAppealPdfSteps.loginUserWithCaseId(credentials.amCaseWorker, false, ibcCaseId);
    await generateAppealPdfSteps.generateAppealPdfEvent();
    await generateAppealPdfSteps.verifyDocumentsTab(
      `${appellantDetails.nameDetails.lastName}_${appellantDetails.identityDetails.ibcaReferenceNumber}.pdf`
    );
  });

  test("Update other party data for IBA Paper case", async ({ updateOtherPartyDataSteps, }) => {
    test.slow();
    await updateOtherPartyDataSteps.loginUserWithCaseId(credentials.amCaseWorker, false, ibcCaseId);
    await updateOtherPartyDataSteps.performUpdateOtherPartyDataIBC(ibcCaseId);
    await updateOtherPartyDataSteps.verifyOtherPartyDetailsIBC();
  });

});
