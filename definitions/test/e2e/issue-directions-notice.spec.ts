import {test} from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";

let caseId: string;

test.describe("Issue direction test", {tag: '@nightly-pipeline'}, async() => {

    test("Issue Direction Notice - Pre Hearing - Normal Tax Credit Application - Appeal to Proceed",
        {tag: '@preview-regression'},
        async ({issueDirectionsNoticeSteps}) => {
            test.slow();
            await issueDirectionsNoticeSteps.performIssueDirectionNoticePreHearingAppealToProceed();
        });

    test("Issue Direction Notice - Post Hearing - Employment Support Application - Provide Information",
        {tag: '@preview-regression'},
        async ({issueDirectionsNoticeSteps}) => {
            test.slow();
            await issueDirectionsNoticeSteps.performIssueDirectionNoticePostHearingESAAppealToProceed();
        });

    test("Issue Direction Notice - Post Hearing - Disability Living Allowance Application - Provide Information",
        async ({issueDirectionsNoticeSteps}) => {
            test.slow();
            await issueDirectionsNoticeSteps.performIssueDirectionNoticePostHearingDLAAppealToProceed();
        });

    test("Issue Direction Notice - Error Messages Test",
        {tag: '@preview-regression'},
        async ({issueDirectionsNoticeSteps}) => {
            test.slow();
            await issueDirectionsNoticeSteps.performIssueDirectionErrorMessages();
        });

    test("Issue Direction Notice - Invalid PIP Case - Pre Hearing - Appeal to Proceed",
        async ({issueDirectionsNoticeSteps}) => {
            test.slow();
            await issueDirectionsNoticeSteps.performIssueDirectionNoticeIncompleteApplicationPreHearingAppealToProceed();
        });
});


