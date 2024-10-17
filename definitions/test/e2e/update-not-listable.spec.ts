import {test} from "../lib/steps.factory";



test.describe("Update Not listable tests", {tag: '@nightly-pipeline'}, async() => {

    //Happy Path Test
    test("Not listable test - Set case to Not listable", async ({updateNotListableSteps}) => {
        test.slow();
        await updateNotListableSteps.performNotListableEvent();
        });

    //Test for Error Messages
    test("Not listable test - Not listable error message test",  async({updateNotListableSteps})=> {
        test.slow();
        await updateNotListableSteps.verifyNotListableErrorMessages()
    })

    test("Update Not listable test - Directions fulfilled case ready to list",async({updateNotListableSteps})=> {
        test.slow();
        await updateNotListableSteps.performUpdateNotListableDirectionFulfilled();
    })

    test("Update Not Listable test - Directions not fulfilled - New Due Date Assigned", async({updateNotListableSteps}) =>{
        test.slow();
        await updateNotListableSteps.performUpdateNotListableDirectionNotFulfilledNewDueDate();
    })

    test("Update Not listable test - Directions not fulfilled - Case moves to ready to list",async({updateNotListableSteps})=> {
        test.slow();
        await updateNotListableSteps.performUpdateNotListableDirectionNotFulfilledReadyToList();
    })

    test("Update Not listable test - Directions not fulfilled - Case moves to With FTA",async({updateNotListableSteps})=> {
        test.slow();
        await updateNotListableSteps.performUpdateNotListableDirectionNotFulfilledWithFTA();
    })

    test("Update Not listable test - Directions not fulfilled - Interlocutory Review (TCW)",async({updateNotListableSteps})=> {
        test.slow();
        await updateNotListableSteps.performUpdateNotListableDirectionNotFulfilledTCW()
    })

    test("Update Not listable test - Directions not fulfilled - Interlocutory Review (Judge)", async({updateNotListableSteps})=> {
        test.slow();
        await updateNotListableSteps.performUpdateNotListableDirectionNotFulfilledJudge()
    })

});