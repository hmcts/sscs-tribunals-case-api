import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';


    let caseId: string;

test('Reissuing Further Evidence', { tag: '@nightly-pipeline' }, async ({ reissueFurtherEvidenceSteps }) => {
        test.slow();
        caseId = await createCaseBasedOnCaseType('PIP');
        await reissueFurtherEvidenceSteps.performUploadDocumentFurtherEvidenceForReissueEvent(caseId, false);
        await reissueFurtherEvidenceSteps.performActionEvidence(caseId);
        await reissueFurtherEvidenceSteps.performReissueFurtherEvidence(caseId);
    })