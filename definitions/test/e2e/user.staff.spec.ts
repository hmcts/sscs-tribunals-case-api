import {test} from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";

let caseId: string;

test("Test for an SSCS Staff Admin to Add a User to the Account",
    {tag: '@wip'},
    async ({userStaffSteps}) => {
    await userStaffSteps.performAddUserStaff();
});

