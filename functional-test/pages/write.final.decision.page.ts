import {Page} from '@playwright/test';
import {WebAction} from '../common/web.action';
import writeFinalDecisionData from "./content/write.final.decision_en.json";
import DateUtilsComponent from "../utils/DateUtilsComponent";
import { triggerAsyncId } from 'async_hooks';

let webActions: WebAction;

export class WriteFinalDecisionPages {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webActions = new WebAction(this.page);
    }

    async verifyPageContentTypeOfAppealPage(dailyLivingFlag = false) {
        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.typeOfAppealPageHeading);
        await webActions.verifyPageLabel('#writeFinalDecisionGenerateNotice legend > .form-label', writeFinalDecisionData.generateNoticeLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionGenerateNotice_Yes\']', writeFinalDecisionData.yesLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionGenerateNotice_No\']', writeFinalDecisionData.noLabel);

        if (dailyLivingFlag === true) {
            await webActions.verifyPageLabel('#writeFinalDecisionIsDescriptorFlow legend > .form-label', writeFinalDecisionData.isThisAwardAboutDailyLivingLabel);
            await webActions.verifyPageLabel('[for=\'writeFinalDecisionIsDescriptorFlow_Yes\']', writeFinalDecisionData.yesLabel);
            await webActions.verifyPageLabel('[for=\'writeFinalDecisionIsDescriptorFlow_No\']', writeFinalDecisionData.noLabel);
        }
    }

    async inputTypeOfAppealPageData(awardDailyLiving = false, generateNotice = true, appealType = "PIP") {
        switch (appealType) {
            case "PIP": {
                if (awardDailyLiving === true) {
                    await webActions.clickElementById("#writeFinalDecisionIsDescriptorFlow_Yes");
                } else {
                    await webActions.clickElementById("#writeFinalDecisionIsDescriptorFlow_No");
                }
                if (generateNotice === true) {
                    await webActions.clickElementById("[for='writeFinalDecisionGenerateNotice_Yes']");
                } else {
                    await webActions.clickElementById("[for='writeFinalDecisionGenerateNotice_No']");
                }
                break;
            }
            case "TAX CREDIT"   : {
                if (generateNotice === true) {
                    await webActions.clickElementById("[for='writeFinalDecisionGenerateNotice_Yes']");
                } else {
                    await webActions.clickElementById("[for='writeFinalDecisionGenerateNotice_No']");
                }
                break;
            }
            case "UNIVERSAL CREDIT": {
                if (generateNotice === true) {
                    await webActions.clickElementById("[for='writeFinalDecisionGenerateNotice_Yes']");
                } else {
                    await webActions.clickElementById("[for='writeFinalDecisionGenerateNotice_No']");
                }
                break;
            }
            case "ESA": {
                if (generateNotice === true) {
                    await webActions.clickElementById("[for='writeFinalDecisionGenerateNotice_Yes']");
                } else {
                    await webActions.clickElementById("[for='writeFinalDecisionGenerateNotice_No']");
                }
                break;
            }
            default: {
                //statements;
                break;
            }
        }

    }

    async verifyPageContentAllowedOrRefusedPage() {
        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.allowedRefusedPageHeading);
        await webActions.verifyPageLabel('span.form-label', writeFinalDecisionData.isTheAppealLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionAllowedOrRefused-allowed\']', writeFinalDecisionData.allowedLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionAllowedOrRefused-refused\']', writeFinalDecisionData.refusedLabel);

    }

    async verifyPageContentTypeOfHearingPage(otherPartyRequiredFlag = false) {
        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.typeOfHearingPageHeading);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionTypeOfHearing\'] > .form-label', writeFinalDecisionData.whatTypeOfHearingWasHeldLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionTypeOfHearing-faceToFace\']', writeFinalDecisionData.faceToFaceLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionTypeOfHearing-telephone\']', writeFinalDecisionData.telephoneLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionTypeOfHearing-video\']', writeFinalDecisionData.videoLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionTypeOfHearing-paper\']', writeFinalDecisionData.paperLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionTypeOfHearing-triage\']', writeFinalDecisionData.triageLabel);
        await webActions.clickElementById("#writeFinalDecisionTypeOfHearing-faceToFace");
        await webActions.verifyPageLabel('#writeFinalDecisionPresentingOfficerAttendedQuestion legend > .form-label', writeFinalDecisionData.didAPresentingOfficerLabel);
        await webActions.verifyPageLabel('#writeFinalDecisionAppellantAttendedQuestion legend > .form-label', writeFinalDecisionData.didTheAppellantAttendTheHearing);
        await webActions.verifyPageLabel('.error-spacing', writeFinalDecisionData.otherPartySectionHeading);
        if (otherPartyRequiredFlag === true) {
            await webActions.verifyPageLabel('.case-field__label', writeFinalDecisionData.otherPartyNameLabel);
            await webActions.verifyPageLabel("[_nghost-ng-c2096613220][_ngcontent-ng-c3895825752] legend > .form-label", writeFinalDecisionData.otherPartyAttendTheHearingLabel);
        }

    }

    async inputTypeOfHearingPageData(otherPartyInputRequired = false) {
        await webActions.clickElementById("#writeFinalDecisionPresentingOfficerAttendedQuestion_Yes");
        await webActions.clickElementById("#writeFinalDecisionAppellantAttendedQuestion_No");
        if (otherPartyInputRequired === true) {
            await webActions.clickElementById("[_nghost-ng-c2096613220][_ngcontent-ng-c3895825752] div:nth-of-type(2) > .form-control");
        }
    }

    async verifyPageContentForPanelMembersPage(appealType = 'PIP') {
        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.panelMembersPageHeading);
        switch (appealType) {
            case 'PIP': case 'ESA': {
                await webActions.verifyPageLabel('[for=\'writeFinalDecisionDisabilityQualifiedPanelMemberName\'] > .form-label', writeFinalDecisionData.nameOfDisabilityQualifiedPanelMemberDQPMLabel);
                await webActions.verifyPageLabel('[for=\'writeFinalDecisionMedicallyQualifiedPanelMemberName\'] > .form-label', writeFinalDecisionData.nameOfMedicallyQualifiedPanelMemberMQPMLabel);

            } case 'UNIVERSAL CREDIT' : {
                await webActions.verifyPageLabel('[for=\'writeFinalDecisionMedicallyQualifiedPanelMemberName\'] > .form-label', writeFinalDecisionData.nameOfMedicallyQualifiedPanelMemberMQPMLabel);
                await webActions.verifyPageLabel('[for=\'writeFinalDecisionOtherPanelMemberName\'] > .form-label', writeFinalDecisionData.otherLabel);
            }
            default : {
                break;
            }
        }
    }

    async inputPageContentForPanelMembersPageData(appealType = 'PIP') {
        switch (appealType) {
            case 'PIP': case 'ESA': {
                await webActions.typeField("#writeFinalDecisionDisabilityQualifiedPanelMemberName", writeFinalDecisionData.nameOfDisabilityQualifiedPanelMemberInput);
                await webActions.typeField("#writeFinalDecisionMedicallyQualifiedPanelMemberName", writeFinalDecisionData.nameOfMedicallyQualifiedPanelMemberInput);
            }
            case 'UNIVERSAL CREDIT' : {
                await webActions.typeField("#writeFinalDecisionMedicallyQualifiedPanelMemberName", writeFinalDecisionData.nameOfMedicallyQualifiedPanelMemberInput);
                await webActions.typeField("#writeFinalDecisionOtherPanelMemberName", writeFinalDecisionData.otherPanelMemberInput);
            }
            default : {
                break;
            }
        }
    }

    async verifyPageContentForDecisionDatePage() {
        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.decisionDatePageHeading);
        await webActions.verifyPageLabel('span.form-label', writeFinalDecisionData.dateOfFTADecisionLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionDateOfDecision-day\']', writeFinalDecisionData.dayLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionDateOfDecision-month\']', writeFinalDecisionData.monthLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionDateOfDecision-year\']', writeFinalDecisionData.yearLabel);
    }

    async inputTypePageContentForDecisionPageData() {
        await webActions.typeField("#writeFinalDecisionDateOfDecision-day", "11");
        await webActions.typeField("#writeFinalDecisionDateOfDecision-month", "07");
        await webActions.typeField("#writeFinalDecisionDateOfDecision-year", "2024");
    }

    async verifyPageContentForBundleSectionReferencePage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.bundleSectionPreferenceHeading);
        await webActions.verifyPageLabel('.form-label', writeFinalDecisionData.whatIsTheLastPageInTheTribunalBundleLabel);
        await webActions.verifyPageLabel('.form-hint', writeFinalDecisionData.whatIsTheLastPageInTheTribunalBundleGuidanceText);
    }

    async inputPageContentForBundleSectionReferencePageData() {
        await webActions.inputField("#writeFinalDecisionPageSectionReference", writeFinalDecisionData.lastPageInTheTribunalBundleInput);
    }

    async verifyPageContentForSummaryOutcomePage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.summaryOfOutcomePageHeading);
        await webActions.verifyPageLabel('.form-label', writeFinalDecisionData.summaryOfOutcomeLabel);
    }

    async inputPageContentForSummaryOutcomePageData() {
        await webActions.inputField("#writeFinalDecisionDetailsOfDecision", writeFinalDecisionData.summaryOfOutcomeInput)
    }

    async verifyPageContentForReasonForDecisionPage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.reasonsForDecisionPageHeading);
        await webActions.clickButton("Add new");
        await webActions.delay(1000);
    }

    async inputPageContentForReasonForDecisionPageData() {
        await webActions.inputField("#writeFinalDecisionReasons_value", writeFinalDecisionData.reasonsForDecisionInput);
        await webActions.delay(3000);
    }

    async verifyPageContentForAnythingElseDecisionPage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.anythingElsePageHeading);
        await webActions.verifyPageLabel('.form-label', writeFinalDecisionData.anythingElseLabel);
    }

    async inputPageContentForAnythingElsePageData() {
        await webActions.inputField("#writeFinalDecisionAnythingElse", writeFinalDecisionData.anythingElseInput)
    }

    async verifyPageContentForPreviewDecisionNoticePage(writeFinalDecisionEventFlag = true) {
        if (writeFinalDecisionEventFlag) {
            await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        } else {
            await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.issueFinalDecisionEventNameCaptor);
        }
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.previewDecisionNoticePageHeading);
        await webActions.verifyPageLabel('.form-label', writeFinalDecisionData.previewDecisionNoticeLabel);
        await webActions.verifyPageLabel('.form-hint', writeFinalDecisionData.previewDecisionNoticeGuidanceText);
    }

    async inputPageContentForPreviewDecisionNoticePageData() {
        await webActions.uploadFileUsingAFileChooser("#writeFinalDecisionPreviewDocument", "testfile1.pdf");
        await new Promise(f => setTimeout(f, 5000));
    }

    async verifyPageContentForCheckYourAnswersPage() {
        //await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor); // No Captor on this Page.
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.checkYourAnswersPageHeading);
        await webActions.verifyPageLabel('.heading-h2', writeFinalDecisionData.checkYourAnswersSectionHeading);
        await webActions.verifyPageLabel('.check-your-answers > [_ngcontent-ng-c645309043] > .text-16', writeFinalDecisionData.checkYourInformationCarefullyLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(3) > .valign-top > .text-16', writeFinalDecisionData.isThisAwardAboutDailyLivingLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(3) > .form-cell .text-16', writeFinalDecisionData.noLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(4) > .valign-top > .text-16', writeFinalDecisionData.generateNoticeLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(4) > .form-cell .text-16', writeFinalDecisionData.yesLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(5) > .valign-top > .text-16', writeFinalDecisionData.isTheAppealLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(5) > .form-cell .text-16', writeFinalDecisionData.allowedLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(6) > .valign-top > .text-16', writeFinalDecisionData.whatTypeOfHearingWasHeldLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(7) > .valign-top > .text-16', writeFinalDecisionData.didAPresentingOfficerLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(8) > .valign-top > .text-16', writeFinalDecisionData.didTheAppellantAttendTheHearing);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(9) > .valign-top > .text-16', writeFinalDecisionData.checkYourAnswersNameOfDisabilityQualifiedPanelMemberDQPMLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(10) > .valign-top > .text-16', writeFinalDecisionData.checkYourAnswersNameOfMedicallyQualifiedPanelMemberMQPMLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(11) > .valign-top > .text-16', writeFinalDecisionData.dateOfFTADecisionLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(12) > .valign-top > .text-16', writeFinalDecisionData.whatIsTheLastPageInTheTribunalBundleLabel);
        //await webActions.verifyPageLabel('.form-table tr:nth-of-type(14) > .valign-top > .text-16', writeFinalDecisionData.checkYourAnswersShowTheFinalDecisionOutcomeLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(14) > .valign-top > .text-16', writeFinalDecisionData.summaryOfOutcomeLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(15) > .valign-top > .text-16', writeFinalDecisionData.reasonsForDecisionLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(16) > .valign-top > .text-16', writeFinalDecisionData.checkYourAnswersAnythingElse);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(17) > .valign-top > .text-16', writeFinalDecisionData.previewDecisionNoticeLabel);
    }

    async verifyAndInputPageContentForTypeOfAwardPage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.typeOfAwardPageHeading);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionDailyLivingQuestion\'] > .form-label', writeFinalDecisionData.whatAreYouConsideringAwardingForDailyLivingLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionDailyLivingQuestion-notConsidered\']', writeFinalDecisionData.notConsideredLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionDailyLivingQuestion-standardRate\']', writeFinalDecisionData.standardRateLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionDailyLivingQuestion-enhancedRate\']', writeFinalDecisionData.enhancedRateLabel);
        await webActions.clickElementById("#pipWriteFinalDecisionDailyLivingQuestion-standardRate");

        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionComparedToDWPDailyLivingQuestion\'] > .form-label', writeFinalDecisionData.howWouldThisNewAwardCompareToTheOriginalFTAAwardForDailyLiving);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionComparedToDWPDailyLivingQuestion-higher\']', writeFinalDecisionData.higherLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionComparedToDWPDailyLivingQuestion-same\']', writeFinalDecisionData.sameLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionComparedToDWPDailyLivingQuestion-lower\']', writeFinalDecisionData.lowerLabel);
        await webActions.clickElementById("#pipWriteFinalDecisionComparedToDWPDailyLivingQuestion-same");

        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionMobilityQuestion\'] > .form-label', writeFinalDecisionData.whatAreYouConsideringAwardingForMobilityLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionMobilityQuestion-notConsidered\']', writeFinalDecisionData.notConsideredLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionMobilityQuestion-standardRate\']', writeFinalDecisionData.standardRateLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionMobilityQuestion-enhancedRate\']', writeFinalDecisionData.enhancedRateLabel);
        await webActions.clickElementById("#pipWriteFinalDecisionMobilityQuestion-enhancedRate");

        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionComparedToDWPMobilityQuestion\'] > .form-label', writeFinalDecisionData.howWouldThisNewAwardCompareToTheOriginalFTAAwardForMobility);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionComparedToDWPMobilityQuestion-higher\']', writeFinalDecisionData.higherLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionComparedToDWPMobilityQuestion-same\']', writeFinalDecisionData.sameLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionComparedToDWPMobilityQuestion-lower\']', writeFinalDecisionData.lowerLabel);
        await webActions.clickElementById("#pipWriteFinalDecisionComparedToDWPMobilityQuestion-higher");

    }

    async verifyAndInputPageContentForAwardDatesPage() {
        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.awardDatesPageHeading);
        await webActions.verifyPageLabel('#writeFinalDecisionStartDate legend > .form-label', writeFinalDecisionData.startDateLabel);
        await webActions.verifyPageLabel("[for='writeFinalDecisionStartDate-day']", writeFinalDecisionData.dayLabel);
        await webActions.verifyPageLabel("[for='writeFinalDecisionStartDate-month']", writeFinalDecisionData.monthLabel);
        await webActions.verifyPageLabel("[for='writeFinalDecisionStartDate-year']", writeFinalDecisionData.yearLabel);
        await webActions.verifyPageLabel("[for='writeFinalDecisionEndDateType'] > .form-label", writeFinalDecisionData.doesThisAwardHaveAnEndDateLabel);
        await webActions.verifyPageLabel("[for='writeFinalDecisionEndDateType-setEndDate']", writeFinalDecisionData.setEndDateLabel);
        await webActions.verifyPageLabel("[for='writeFinalDecisionEndDateType-indefinite']", writeFinalDecisionData.indefiniteAwardLabel);
        await webActions.verifyPageLabel("[for='writeFinalDecisionEndDateType-na']", writeFinalDecisionData.notApplicableNoAwardLabel);
        await webActions.clickElementById("#writeFinalDecisionEndDateType-setEndDate");
        await webActions.verifyPageLabel('#writeFinalDecisionEndDate legend > .form-label', writeFinalDecisionData.endDateLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionEndDate-day\']', writeFinalDecisionData.dayLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionEndDate-month\']', writeFinalDecisionData.monthLabel);
        await webActions.verifyPageLabel('[for=\'writeFinalDecisionEndDate-year\']', writeFinalDecisionData.yearLabel);
    }

    async inputPageContentForAwardDatesPageData() {
        await webActions.typeField("#writeFinalDecisionStartDate-day", '01');
        await webActions.typeField("#writeFinalDecisionStartDate-month", '07');
        await webActions.typeField("#writeFinalDecisionStartDate-year", '2024');

        await webActions.typeField("#writeFinalDecisionEndDate-day", '30');
        await webActions.typeField("#writeFinalDecisionEndDate-month", '07');
        await webActions.typeField("#writeFinalDecisionEndDate-year", '2024');
    }

    async verifyPageContentForSelectActivitiesPage() {
        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.selectActivitiesPageHeading);
        await webActions.verifyPageLabel('#pipWriteFinalDecisionDailyLivingActivitiesQuestion legend > .form-label', writeFinalDecisionData.dailyActivitiesLabel);
        await webActions.verifyPageLabel('#pipWriteFinalDecisionDailyLivingActivitiesQuestion .form-hint', writeFinalDecisionData.selectAllThatApplyGuidanceText);
        await webActions.verifyPageLabel("[for='pipWriteFinalDecisionDailyLivingActivitiesQuestion-preparingFood']", writeFinalDecisionData.preparingFoodLabel);
        await webActions.verifyPageLabel("[for='pipWriteFinalDecisionDailyLivingActivitiesQuestion-takingNutrition']", writeFinalDecisionData.takingNutritionLabel);
        await webActions.verifyPageLabel("[for='pipWriteFinalDecisionDailyLivingActivitiesQuestion-managingTherapy']", writeFinalDecisionData.managingTherapyOrMonitoringHealthConditionLabel);
        await webActions.verifyPageLabel("[for='pipWriteFinalDecisionDailyLivingActivitiesQuestion-washingAndBathing']", writeFinalDecisionData.washingAndBathingLabel);
        await webActions.verifyPageLabel("[for='pipWriteFinalDecisionDailyLivingActivitiesQuestion-managingToiletNeeds']", writeFinalDecisionData.managingToiletNeedsOrIncontinenceLabel);
        await webActions.verifyPageLabel("[for='pipWriteFinalDecisionDailyLivingActivitiesQuestion-dressingAndUndressing']", writeFinalDecisionData.dressingAndUndressingLabel);
        await webActions.verifyPageLabel("[for='pipWriteFinalDecisionDailyLivingActivitiesQuestion-communicating']", writeFinalDecisionData.communicatingVerballyLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionDailyLivingActivitiesQuestion-readingUnderstanding\']', writeFinalDecisionData.readingAndUnderstandingSignsSymbolsAndWordsLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionDailyLivingActivitiesQuestion-engagingWithOthers\']', writeFinalDecisionData.engagingWithOtherPeopleF2FLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionDailyLivingActivitiesQuestion-budgetingDecisions\']', writeFinalDecisionData.makingBudgetingDecisionsLabel);
        await webActions.verifyPageLabel('#pipWriteFinalDecisionMobilityActivitiesQuestion legend > .form-label', writeFinalDecisionData.mobilityLabel);
        await webActions.verifyPageLabel('#pipWriteFinalDecisionMobilityActivitiesQuestion .form-hint', writeFinalDecisionData.selectAllThatApplyGuidanceText);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionMobilityActivitiesQuestion-planningAndFollowing\']', writeFinalDecisionData.planningAndFollowingJourneysLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionMobilityActivitiesQuestion-movingAround\']', writeFinalDecisionData.movingAroundLabel);
    }

    async inputPageContentForSelectActivitiesPageData() {
        await webActions.clickElementById("#pipWriteFinalDecisionDailyLivingActivitiesQuestion-takingNutrition");
        await webActions.clickElementById("#pipWriteFinalDecisionMobilityActivitiesQuestion-movingAround");
    }

    async verifyPageContentForDailyLivingNutrition() {
        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.dailyLivingTakingNutritionPageHeading);
        await webActions.verifyPageLabel('span.form-label', writeFinalDecisionData.takingNutritionLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionTakingNutritionQuestion-takingNutrition2a\']', writeFinalDecisionData.canTakeNutritionLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionTakingNutritionQuestion-takingNutrition2b\']', writeFinalDecisionData.needsEitherLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionTakingNutritionQuestion-takingNutrition2c\']', writeFinalDecisionData.needsATherapeuticLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionTakingNutritionQuestion-takingNutrition2d\']', writeFinalDecisionData.needsPromptingLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionTakingNutritionQuestion-takingNutrition2e\']', writeFinalDecisionData.needsAssistanceLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionTakingNutritionQuestion-takingNutrition2f\']', writeFinalDecisionData.cannotConveyFoodAndDrinkLabel);
    }

    async inputPageContentForDailyLivingNutritionPageData() {
        await webActions.clickElementById("div:nth-of-type(6) > .form-control");
    }

    async verifyPageContentForMovingAround() {
        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.mobilityActivitiesPageHeading);
        await webActions.verifyPageLabel('span.form-label', writeFinalDecisionData.movingAroundLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionMovingAroundQuestion-movingAround12a\']', writeFinalDecisionData.canStandAndThenMoveMoreThan200mLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionMovingAroundQuestion-movingAround12b\']', writeFinalDecisionData.canStandAndThenMoveMoreThan50mLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionMovingAroundQuestion-movingAround12c\']', writeFinalDecisionData.canStandAndThenMoveMoreThan20mLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionMovingAroundQuestion-movingAround12d\']', writeFinalDecisionData.canStandAndThenMoveWithAid20mTo50mLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionMovingAroundQuestion-movingAround12e\']', writeFinalDecisionData.canStandAndThenMoveWithAid1mTo20mLabel);
        await webActions.verifyPageLabel('[for=\'pipWriteFinalDecisionMovingAroundQuestion-movingAround12f\']', writeFinalDecisionData.canStandAndThenMoveEitherAidedOrUnaidedLabel);
    }

    async verifyPageContentForCheckYourAnswersPageForAwardsCriteria() {

        //await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor); // No Captor on this Page.
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.checkYourAnswersPageHeading);
        await webActions.verifyPageLabel('.heading-h2', writeFinalDecisionData.checkYourAnswersSectionHeading);
        await webActions.verifyPageLabel('.check-your-answers > [_ngcontent-ng-c645309043] > .text-16', writeFinalDecisionData.checkYourInformationCarefullyLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(3) > .valign-top > .text-16', writeFinalDecisionData.isThisAwardAboutDailyLivingLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(3) > .form-cell .text-16', writeFinalDecisionData.yesLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(4) > .valign-top > .text-16', writeFinalDecisionData.generateNoticeLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(4) > .form-cell .text-16', writeFinalDecisionData.yesLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(5) > .valign-top > .text-16', writeFinalDecisionData.whatTypeOfHearingWasHeldLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(5) > .form-cell .text-16', writeFinalDecisionData.faceToFaceLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(6) > .valign-top > .text-16', writeFinalDecisionData.didAPresentingOfficerLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(6) > .form-cell .text-16', writeFinalDecisionData.yesLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(7) > .valign-top > .text-16', writeFinalDecisionData.didTheAppellantAttendTheHearing);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(7) > .form-cell .text-16', writeFinalDecisionData.noLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(8) > .valign-top > .text-16', writeFinalDecisionData.whatAreYouConsideringAwardingForDailyLivingLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(8) > .form-cell .text-16', writeFinalDecisionData.standardRateLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(9) > .valign-top > .text-16', writeFinalDecisionData.howWouldThisNewAwardCompareToTheOriginalFTAAwardForDailyLiving);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(9) > .form-cell .text-16', writeFinalDecisionData.sameLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(10) > .valign-top > .text-16', writeFinalDecisionData.whatAreYouConsideringAwardingForMobilityLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(10) > .form-cell .text-16', writeFinalDecisionData.enhancedRateLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(11) > .valign-top > .text-16', writeFinalDecisionData.howWouldThisNewAwardCompareToTheOriginalFTAAwardForMobility);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(11) > .form-cell .text-16', writeFinalDecisionData.higherLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(12) > .valign-top > .text-16', writeFinalDecisionData.startDateLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(13) > .valign-top > .text-16', writeFinalDecisionData.doesThisAwardHaveAnEndDateLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(13) > .form-cell .text-16', writeFinalDecisionData.setEndDateLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(14) > .valign-top > .text-16', writeFinalDecisionData.endDateLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(15) > .valign-top > .text-16', writeFinalDecisionData.nameOfDisabilityQualifiedPanelMemberDQPMWithoutOptionalLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(15) > .form-cell .text-16', writeFinalDecisionData.nameOfDisabilityQualifiedPanelMemberInput);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(16) > .valign-top > .text-16', writeFinalDecisionData.nameOfMedicallyQualifiedPanelMemberMQPMWithoutOptionalLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(16) > .form-cell .text-16', writeFinalDecisionData.nameOfMedicallyQualifiedPanelMemberInput);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(17) > .valign-top > .text-16', writeFinalDecisionData.dateOfFTADecisionLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(18) > .valign-top > .text-16', writeFinalDecisionData.dailyLivingLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(18) > .form-cell .text-16', writeFinalDecisionData.takingNutritionLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(19) > .valign-top > .text-16', writeFinalDecisionData.mobilityNonOptionalLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(19) > .form-cell .text-16', writeFinalDecisionData.movingAroundLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(20) > .valign-top > .text-16', writeFinalDecisionData.takingNutritionLabel);
        //await webActions.verifyPageLabel('.form-table tr:nth-of-type(20) > .form-cell .text-16', writeFinalDecisionData.takingNutritionLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(21) > .valign-top > .text-16', writeFinalDecisionData.movingAroundLabel);
        //await webActions.verifyPageLabel('.form-table tr:nth-of-type(21) > .form-cell .text-16', writeFinalDecisionData.takingNutritionLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(22) > .valign-top > .text-16', writeFinalDecisionData.whatIsTheLastPageInTheTribunalBundleLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(22) > .form-cell .text-16', writeFinalDecisionData.lastPageInTheTribunalBundleInput);

        await webActions.verifyPageLabel('tr:nth-of-type(23) > .valign-top > .text-16', writeFinalDecisionData.reasonsForDecisionLabel);
        await webActions.verifyPageLabel('tr:nth-of-type(23) > td:nth-of-type(1) span:nth-of-type(1)', writeFinalDecisionData.reasonsForDecisionInput);

        await webActions.verifyPageLabel('tr:nth-of-type(24) > .case-field-label', writeFinalDecisionData.anythingElsePageHeading);
        await webActions.verifyPageLabel('tr:nth-of-type(24) [_ngcontent-ng-c142448239]', writeFinalDecisionData.anythingElseInput);

        await webActions.verifyPageLabel('tr:nth-of-type(25) > .valign-top > .text-16', writeFinalDecisionData.previewDecisionNoticeLabel);
    }


    async verifyPageContentForCheckYourAnswersPageForNoNoticeGenerated() {
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.checkYourAnswersPageHeading);
        await webActions.verifyPageLabel('.heading-h2', writeFinalDecisionData.checkYourAnswersSectionHeading);
        await webActions.verifyPageLabel('.check-your-answers > [_ngcontent-ng-c645309043] > .text-16', writeFinalDecisionData.checkYourInformationCarefullyLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(3) > .valign-top > .text-16', writeFinalDecisionData.generateNoticeLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(3) > .form-cell .text-16', writeFinalDecisionData.noLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(4) > .valign-top > .text-16', writeFinalDecisionData.isTheAppealLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(4) > .form-cell .text-16', writeFinalDecisionData.refusedLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(5) > .valign-top > .text-16', writeFinalDecisionData.previewDecisionNoticeLabel);

    }

    async verifyPageContentForCheckYourAnswersPageForUCCaseWithScheduleAndReasses() {

        //await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor); // No Captor on this Page.
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.checkYourAnswersPageHeading);
        await webActions.verifyPageLabel('.heading-h2', writeFinalDecisionData.checkYourAnswersSectionHeading);
        await webActions.verifyPageLabel('.check-your-answers > [_ngcontent-ng-c645309043] > .text-16', writeFinalDecisionData.checkYourInformationCarefullyLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(3) > .valign-top > .text-16', writeFinalDecisionData.generateNoticeLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(3) > .form-cell .text-16', writeFinalDecisionData.yesLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(4) > .valign-top > .text-16', writeFinalDecisionData.isTheAppealLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(4) > .form-cell .text-16', writeFinalDecisionData.allowedLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(5) > .valign-top > .text-16', writeFinalDecisionData.whatTypeOfHearingWasHeldLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(5) > .form-cell .text-16', writeFinalDecisionData.faceToFaceLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(6) > .valign-top > .text-16', writeFinalDecisionData.didAPresentingOfficerLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(6) > .form-cell .text-16', writeFinalDecisionData.yesLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(7) > .valign-top > .text-16', writeFinalDecisionData.didTheAppellantAttendTheHearing);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(7) > .form-cell .text-16', writeFinalDecisionData.noLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(8) > .valign-top > .text-16', writeFinalDecisionData.nameOfMedicallyQualifiedPanelMemberMQPMWithoutOptionalLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(8) > .form-cell .text-16', writeFinalDecisionData.nameOfMedicallyQualifiedPanelMemberInput);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(9) > .valign-top > .text-16', writeFinalDecisionData.otherWithoutOptionalLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(9) > .form-cell .text-16', writeFinalDecisionData.otherPanelMemberInput);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(10) > .valign-top > .text-16', writeFinalDecisionData.dateOfFTADecisionLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(12) > .valign-top > .text-16', writeFinalDecisionData.isThisAWCAAppeal);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(12) > .form-cell .text-16', writeFinalDecisionData.yesLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(13) > .valign-top > .text-16', writeFinalDecisionData.isThisASupportGroupOnlyAppealLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(13) > .form-cell .text-16', writeFinalDecisionData.yesLabel);

        //Commented out as The return value was 'Show Schedule 7 Activities Apply?'
        //await webActions.verifyPageLabel('.form-table tr:nth-of-type(15) > .valign-top > .text-16', writeFinalDecisionData.doSchedule7ActivitiesApply);
        //await webActions.verifyPageLabel('.form-table tr:nth-of-type(15) > .form-cell .text-16', writeFinalDecisionData.theSchedule7ActivitiesSelectedBelowApply);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(16) > .valign-top > .text-16', writeFinalDecisionData.schedule7ActivitiesLabel);
        await webActions.verifyPageLabel('tr:nth-of-type(16) tr:nth-of-type(1) .text-16', writeFinalDecisionData.manualDexterityLabel);
        await webActions.verifyPageLabel('tr:nth-of-type(16) tr:nth-of-type(2) .text-16', writeFinalDecisionData.initiatingAndCompletingLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(17) > .valign-top > .text-16', writeFinalDecisionData.whatIsTheLastPageInTheTribunalBundleLabel);
        await webActions.verifyPageLabel('tr:nth-of-type(17) > .form-cell .text-16', writeFinalDecisionData.lastPageInTheTribunalBundleInput);

        await webActions.verifyPageLabel('tr:nth-of-type(19) > .valign-top > .text-16', writeFinalDecisionData.whenShouldFTAReAssessTheAwardLabel);
        await webActions.verifyPageLabel('tr:nth-of-type(19) > .form-cell .text-16', writeFinalDecisionData.reassessWithin3MonthsLabel);

        await webActions.verifyPageLabel('tr:nth-of-type(20) > .valign-top > .text-16', writeFinalDecisionData.reasonsForDecisionLabel);
        await webActions.verifyPageLabel('tr:nth-of-type(20) > td:nth-of-type(1) span:nth-of-type(1)', writeFinalDecisionData.reasonsForDecisionInput);

        await webActions.verifyPageLabel('tr:nth-of-type(20) > .valign-top > .text-16', writeFinalDecisionData.reasonsForDecisionLabel);
        await webActions.verifyPageLabel('tr:nth-of-type(20) > td:nth-of-type(1) span:nth-of-type(1)', writeFinalDecisionData.reasonsForDecisionInput);

        await webActions.verifyPageLabel('tr:nth-of-type(21) > .valign-top > .text-16', writeFinalDecisionData.checkYourAnswersAnythingElse);
        await webActions.verifyPageLabel('tr:nth-of-type(21) > .form-cell span', writeFinalDecisionData.anythingElseInput);

        await webActions.verifyPageLabel('tr:nth-of-type(22) > .valign-top > .text-16', writeFinalDecisionData.previewDecisionNoticeLabel);
    }

        async inputPageContentForMovingAroundPageData() {
        await webActions.clickElementById("div:nth-of-type(6) > .form-control");
    }

    async verifyPageContentForWorkCapabilityAssessmentPage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.workCapabilitiesAssessmentPageHeading);
        await webActions.verifyPageLabel('#wcaAppeal legend > .form-label', writeFinalDecisionData.isThisAWCAAppeal);
        await webActions.verifyPageLabel('[for=\'wcaAppeal_Yes\']', writeFinalDecisionData.yesLabel);
        await webActions.verifyPageLabel('[for=\'wcaAppeal_No\']', writeFinalDecisionData.noLabel);
    }

    async inputAndVerifyPageContentForWorkCapabilityAssessmentPageData(supportGroup:boolean) {
        await webActions.clickElementById("#wcaAppeal_Yes");
        await webActions.verifyPageLabel('#supportGroupOnlyAppeal legend > .form-label', writeFinalDecisionData.isThisASupportGroupOnlyAppealLabel);
        await webActions.verifyPageLabel('[for=\'supportGroupOnlyAppeal_Yes\']', writeFinalDecisionData.yesLabel);
        await webActions.verifyPageLabel('[for=\'supportGroupOnlyAppeal_No\']', writeFinalDecisionData.noLabel);
        (supportGroup) ? await webActions.clickElementById("#supportGroupOnlyAppeal_Yes") : await webActions.clickElementById("#supportGroupOnlyAppeal_No");
    }

    async verifyPageContentForSchedule7ActivitiesPage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.schedule7ActivitiesPageHeading);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesApply\'] > .form-label', writeFinalDecisionData.doSchedule7ActivitiesApply);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesApply-No\']', writeFinalDecisionData.noneApply);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesApply-Yes\']', writeFinalDecisionData.theSchedule7ActivitiesSelectedBelowApply);
    }

    async inputAndVerifyPageContentForSchedule7ActivitiesPageData() {
        await webActions.clickElementById("ccd-write-fixed-radio-list-field div:nth-of-type(2) > .form-control");
        await webActions.verifyPageLabel('ccd-write-multi-select-list-field legend > .form-label', writeFinalDecisionData.schedule7ActivitiesLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7MobilisingUnaided\']', writeFinalDecisionData.mobilisingUnaidedLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7SittingPositions\']', writeFinalDecisionData.transferringFromOneSeatedPositionLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7Reaching\']', writeFinalDecisionData.reachingCannotRaiseEitherArmLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7PickingUp\']', writeFinalDecisionData.pickingUpMovingAndTransferringLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7ManualDexterity\']', writeFinalDecisionData.manualDexterityLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7ManualDexterity\']', writeFinalDecisionData.manualDexterityLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7MakingSelfUnderstood\']', writeFinalDecisionData.makingSelfUnderstoodLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7Communication\']', writeFinalDecisionData.understandingCommunicationByLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7LossOfControl\']', writeFinalDecisionData.absenceOrLossLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7LearningTasks\']', writeFinalDecisionData.learningTasksLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7PersonalAction\']', writeFinalDecisionData.initiatingAndCompletingLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7CopingWithChange\']', writeFinalDecisionData.copingWithChangeLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7SocialEngagement\']', writeFinalDecisionData.copingWithSocialEngagementLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7AppropriatenessOfBehaviour\']', writeFinalDecisionData.appropriatenessOfBehaviourLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7ConveyingFoodOrDrink\']', writeFinalDecisionData.conveyingFoodOrDrinkLabel);
        await webActions.verifyPageLabel('[for=\'ucWriteFinalDecisionSchedule7ActivitiesQuestion-schedule7ChewingOrSwallowing\']', writeFinalDecisionData.chewingOrSwallowingFoodLabel);
        await webActions.clickElementById("[value='schedule7ManualDexterity']");
        await webActions.clickElementById("[value='schedule7PersonalAction']");
    }

    async verifyPageContentForReassessTheAwardPage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.whenShouldDWPReassessPageHeading);
        await webActions.verifyPageLabel('span.form-label', writeFinalDecisionData.whenShouldFTAReAssessTheAwardLabel);
        await webActions.verifyPageLabel('[for=\'dwpReassessTheAward-noRecommendation\']', writeFinalDecisionData.noRecommendationLabel);
        await webActions.verifyPageLabel('[for=\'dwpReassessTheAward-reassess3\']', writeFinalDecisionData.reassessWithin3MonthsLabel);
        await webActions.verifyPageLabel('[for=\'dwpReassessTheAward-reassess6\']', writeFinalDecisionData.reassessWithin6MonthsLabel);
        await webActions.verifyPageLabel('[for=\'dwpReassessTheAward-reassess12\']', writeFinalDecisionData.reassessWithin12MonthsLabel);
        await webActions.verifyPageLabel('[for=\'dwpReassessTheAward-reassess18\']', writeFinalDecisionData.reassessWithin18MonthsLabel);
        await webActions.verifyPageLabel('[for=\'dwpReassessTheAward-reassess24\']', writeFinalDecisionData.reassessWithin24MonthsLabel);
        await webActions.verifyPageLabel('[for=\'dwpReassessTheAward-doNotReassess3\']', writeFinalDecisionData.doNotReassessWithin3MonthsLabel);
        await webActions.verifyPageLabel('[for=\'dwpReassessTheAward-doNotReassess6\']', writeFinalDecisionData.doNotReassessWithin6MonthsLabel);
        await webActions.verifyPageLabel('[for=\'dwpReassessTheAward-doNotReassess12\']', writeFinalDecisionData.doNotReassessWithin12MonthsLabel);
        await webActions.verifyPageLabel('[for=\'dwpReassessTheAward-doNotReassess18\']', writeFinalDecisionData.doNotReassessWithin18MonthsLabel);
        await webActions.verifyPageLabel('[for=\'dwpReassessTheAward-doNotReassess24\']', writeFinalDecisionData.doNotReassessWithin24MonthsLabel);
        await webActions.verifyPageLabel('[for=\'dwpReassessTheAward-doNotReassess\']', writeFinalDecisionData.doNotReassessLabel);
    }

    async verifyPageContentForSchedule2ActivitiesPage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.schedule2ActivitiesPageHeading);
        await webActions.verifyPageLabel('[id="esaWriteFinalDecisionPhysicalDisabilitiesQuestion"] > fieldset > legend > .form-label', writeFinalDecisionData.schedule2ActivitiesLabel);
        await webActions.verifyPageLabel('[id="esaWriteFinalDecisionMentalAssessmentQuestion"] > fieldset > legend > .form-label', writeFinalDecisionData.schedule2ActivitiesPart2Label);
    }

    async inputAndVerifyPageContentForSchedule2ActivitiesPageData(option1: string, option2: string) {
        // await webActions.clickElementById("ccd-write-fixed-radio-list-field div:nth-of-type(2) > .form-control");
        // await webActions.verifyPageLabel('ccd-write-multi-select-list-field legend > .form-label', writeFinalDecisionData.schedule7ActivitiesLabel);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionPhysicalDisabilitiesQuestion-mobilisingUnaided\']', writeFinalDecisionData.esaMobilisingUnaidedLabel);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionPhysicalDisabilitiesQuestion-standingAndSitting\']', writeFinalDecisionData.esaTransferringFromOneSeatedPositionLabel);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionPhysicalDisabilitiesQuestion-reaching\']', writeFinalDecisionData.esaReachingCannotRaiseEitherArmLabel);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionPhysicalDisabilitiesQuestion-pickingUp\']', writeFinalDecisionData.esaPickingUpMovingAndTransferringLabel);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionPhysicalDisabilitiesQuestion-manualDexterity\']', writeFinalDecisionData.esaManualDexterityLabel);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionPhysicalDisabilitiesQuestion-makingSelfUnderstood\']', writeFinalDecisionData.esaMakingSelfUnderstoodLabel);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionPhysicalDisabilitiesQuestion-communication\']', writeFinalDecisionData.esaUnderstandingCommunicationByLabel);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionPhysicalDisabilitiesQuestion-navigation\']', writeFinalDecisionData.esaNavigation);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionPhysicalDisabilitiesQuestion-lossOfControl\']', writeFinalDecisionData.esaAbsenceOrLossLabel);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionPhysicalDisabilitiesQuestion-consciousness\']', writeFinalDecisionData.esaConsciousness);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionMentalAssessmentQuestion-learningTasks\']', writeFinalDecisionData.esaLearning);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionMentalAssessmentQuestion-awarenessOfHazards\']', writeFinalDecisionData.esaAwareness);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionMentalAssessmentQuestion-personalAction\']', writeFinalDecisionData.esaInitiation);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionMentalAssessmentQuestion-copingWithChange\']', writeFinalDecisionData.esaCoping);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionMentalAssessmentQuestion-gettingAbout\']', writeFinalDecisionData.esaGettingAbout);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionMentalAssessmentQuestion-socialEngagement\']', writeFinalDecisionData.esaSocialEngagement);
        await webActions.verifyPageLabel('[for=\'esaWriteFinalDecisionMentalAssessmentQuestion-appropriatenessOfBehaviour\']', writeFinalDecisionData.esaAppropriateness);
        await webActions.clickElementById(`#esaWriteFinalDecisionPhysicalDisabilitiesQuestion-${option1}`);
        await webActions.clickElementById(`#esaWriteFinalDecisionMentalAssessmentQuestion-${option2}`);
    }

    async verifyPageContentForCheckYourAnswersPageForESACaseWithScheduleAndReasses(appealPermission: string) {

        //await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor); // No Captor on this Page.
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.checkYourAnswersPageHeading);
        await webActions.verifyPageLabel('.heading-h2', writeFinalDecisionData.checkYourAnswersSectionHeading);
        await webActions.verifyPageLabel('.check-your-answers > [_ngcontent-ng-c645309043] > .text-16', writeFinalDecisionData.checkYourInformationCarefullyLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(3) > .valign-top > .text-16', writeFinalDecisionData.generateNoticeLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(3) > .form-cell .text-16', writeFinalDecisionData.yesLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(4) > .valign-top > .text-16', writeFinalDecisionData.isTheAppealLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(4) > .form-cell .text-16', appealPermission);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(5) > .valign-top > .text-16', writeFinalDecisionData.whatTypeOfHearingWasHeldLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(5) > .form-cell .text-16', writeFinalDecisionData.faceToFaceLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(6) > .valign-top > .text-16', writeFinalDecisionData.didAPresentingOfficerLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(6) > .form-cell .text-16', writeFinalDecisionData.yesLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(7) > .valign-top > .text-16', writeFinalDecisionData.didTheAppellantAttendTheHearing);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(7) > .form-cell .text-16', writeFinalDecisionData.noLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(12) > .valign-top > .text-16', writeFinalDecisionData.isThisAWCAAppeal);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(12) > .form-cell .text-16', writeFinalDecisionData.yesLabel);

        await webActions.verifyPageLabel('.form-table tr:nth-of-type(13) > .valign-top > .text-16', writeFinalDecisionData.isThisASupportGroupOnlyAppealLabel);
        await webActions.verifyPageLabel('.form-table tr:nth-of-type(13) > .form-cell .text-16', writeFinalDecisionData.noLabel);

        // await webActions.verifyPageLabel('.form-table tr:nth-of-type(14) > .form-cell .text-16', phyDisabilities);
        // await webActions.verifyPageLabel('.form-table tr:nth-of-type(15) > .form-cell .text-16', menDisabilities);
    }

    async verifyPageContentForConsciousnessTheAwardPage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.esaPhysicalConsciousnessTitle);
    }

    async verifyPageContentForReachingTheAwardPage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.esaReachingTitle);
    }

    async inputAndVerifyPageContentForSchedule2ConsciousnessPageData() {
        await webActions.clickElementById("#esaWriteFinalDecisionConsciousnessQuestion-consciousness10c");
    }

    async inputAndVerifyPageContentForSchedule2ReachingPageData() {
        await webActions.clickElementById("#esaWriteFinalDecisionReachingQuestion-reaching3b");
    }

    async verifyPageContentForCopingTheAwardPage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.esaCopingWithChangeTitle);
    }

    async verifyPageContentForCognitiveTheAwardPage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.esaCognitiveTitle);
    }

    async inputAndVerifyPageContentForSchedule2CognitivePageData() {
        await webActions.clickElementById("#esaWriteFinalDecisionLearningTasksQuestion-learningTasks11b");
    }


    async inputAndVerifyPageContentForSchedule2CopingPageData() {
        await webActions.clickElementById("#esaWriteFinalDecisionCopingWithChangeQuestion-copingWithChange14d");
    }

    async verifyPageContentAndInputForRegulationPage() {

        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.esaRegulationTitle);
        await webActions.clickElementById('#doesRegulation29Apply_No');
    }

    async inputAndVerifyPageContentForSchedule3Activities() {
        await webActions.verifyPageLabel('.govuk-caption-l', writeFinalDecisionData.eventNameCaptor);
        await webActions.verifyPageLabel('h1.govuk-heading-l', writeFinalDecisionData.esaSchedule3ActivitiesPageHeading);
        await webActions.clickElementById('#esaWriteFinalDecisionSchedule3ActivitiesApply-No');
        await webActions.clickElementById('#doesRegulation35Apply_No');
    }

    async inputPageContentForReassessTheAwardPage() {
        await webActions.clickElementById("#dwpReassessTheAward div:nth-of-type(2) > .form-control");
    }

    async chooseAllowedOrRefused(optionVal: string) {
        await webActions.clickElementById(optionVal);
    }

    async submitContinueBtn(): Promise<void> {
        await webActions.clickButton("Continue");
    }

    async verifyDocumentTitle(expText: string) {
        await webActions.verifyTextVisibility(expText);
    }

    async confirmSubmission(): Promise<void> {
        await this.page.waitForTimeout(3000);
        await webActions.clickSubmitButton();
        await this.page.waitForTimeout(3000);
    }
}
