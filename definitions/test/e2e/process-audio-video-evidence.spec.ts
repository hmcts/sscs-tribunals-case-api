import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";

let caseId : string;

test.describe("Process audio/video evidence test", {tag: "@nightly-pipeline"}, async() => {

    test.beforeEach("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });
    
    test("Grant - Audio/video evidence uploaded by DWP", async ({ uploadResponseSteps, processAVEvidenceSteps }) => {
        await uploadResponseSteps.performUploadResponseWithAVEvidenceOnAPIP(caseId);
        await processAVEvidenceSteps.acceptEvidenceUploadedByDWP(caseId);
    });

    test("Grant - Audio/video evidence uploaded by CTSC", {tag: "@preview-regression"}, async ({ uploadDocumentFurtherEvidenceSteps, processAVEvidenceSteps }) => {
        await uploadDocumentFurtherEvidenceSteps.performUploadDocumentFurtherEvidence(caseId, true);
        await processAVEvidenceSteps.acceptEvidenceUploadedByCTSC(caseId);
    });
    
    test("Refuse - Audio/video evidence uploaded by DWP", async ({ uploadResponseSteps, processAVEvidenceSteps }) => {

        await uploadResponseSteps.performUploadResponseWithAVEvidenceOnAPIP(caseId);
        await processAVEvidenceSteps.excludeEvidenceUploadedByDWP(caseId);
    });

    test("Refuse - Audio/video evidence uploaded by CTSC", async ({ uploadDocumentFurtherEvidenceSteps, processAVEvidenceSteps }) => {

        await uploadDocumentFurtherEvidenceSteps.performUploadDocumentFurtherEvidence(caseId, true);
        await processAVEvidenceSteps.excludeEvidenceUploadedByCTSC(caseId);
    });
    

     test.afterAll("Case has to be set to Dormant",async () => {
        await performAppealDormantOnCase(caseId);
     });
});

test.describe.serial('WA - Manual Process Audio/Video Evidence task completion test', {tag: '@work-allocation'}, async() => {

    test.beforeAll("Case has to be Created",async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("As a TCW view and complete Process Audio/Video Evidence manually", async ({ uploadDocumentFurtherEvidenceSteps, processAVEvidenceSteps }) => {
        
        test.slow();
        await uploadDocumentFurtherEvidenceSteps.performUploadDocumentFurtherEvidence(caseId, true); 
        await processAVEvidenceSteps.verifyTcwWithCaseAllocatorRoleCanViewAndAssignProcessAudioVideoEvidenceTask(caseId);
        await processAVEvidenceSteps.verifyTcwCanMarkProcessAudioVideoEvidenceTaskCanViewAndCompleteTheTaskMarkAsDone(caseId);
    });

    test.afterAll("Case has to be set to Dormant",async () => {
         await performAppealDormantOnCase(caseId);
    });
});

test.describe.serial('WA - Process Audio/Video Evidence task iniation and completion tests', {tag: '@work-allocation'}, async() => {

    test.beforeAll("Case has to be Created",async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Perform upload document further evidence and assign to the task to TCW", async ({ uploadDocumentFurtherEvidenceSteps, processAVEvidenceSteps }) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.performUploadDocumentFurtherEvidence(caseId, true);
        await processAVEvidenceSteps.verifyTcwWithCaseAllocatorRoleCanViewAndAssignProcessAudioVideoEvidenceTask(caseId);
    });

    test("As a TCW, complete Process Audio/Video Evidence task", async ({ processAVEvidenceSteps }) => {

        test.slow();
        await processAVEvidenceSteps.verifyTcwAsAnAssignedUserForProcessAudioVideoEvidenceTaskCanViewAndCompleteTheTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant",async () => {
         await performAppealDormantOnCase(caseId);
    });
});

test.describe.serial('WA - Process Audio/Video Evidence task automatic cancellation when case is void', {tag: '@work-allocation'}, async() => {

    test.beforeAll("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Grant - Audio/video evidence uploaded by CTSC", async ({ uploadDocumentFurtherEvidenceSteps }) => {
        await uploadDocumentFurtherEvidenceSteps.performUploadDocumentFurtherEvidence(caseId, true);
    });

    test("Process Audio/Video Evidence task is cancelled automatically when case is void", async ({ processAVEvidenceSteps }) => {
        await processAVEvidenceSteps.verifyProcessAudioVideoEvidenceTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
});
