import { expect, Page } from '@playwright/test';
import { BaseStep } from './base';
import { credentials } from "../../config/config";
import languagePreferenceData from '../../pages/content/update.langauge.preference_en.json';
import eventTestData from '../../pages/content/event.name.event.description_en.json';

export class UpdateLanguagePreference extends BaseStep {
    
  readonly page : Page;

   constructor(page: Page) {
       super(page);
       this.page = page;
   }

    async performUpdateLanguagePreference(caseId :string, loginRequired :boolean = true) {

        if(loginRequired) {
            await this.loginUserWithCaseId(credentials.amCaseWorker, false, caseId);
            await this.homePage.reloadPage(); 
        }

        await this.homePage.chooseEvent(languagePreferenceData.eventName);

        await this.updateLanguagePreferencePage.verifyPageContent();
        await this.updateLanguagePreferencePage.chooseWelshLanguage(languagePreferenceData.welshlanguagePreferenceYesLabel);
        await this.updateLanguagePreferencePage.confirmSubmission();

        await this.updateLanguagePreferencePage.verifyCYAPageContent();
        await this.eventNameAndDescriptionPage.inputData(eventTestData.eventSummaryInput, eventTestData.eventDescriptionInput);
        await this.eventNameAndDescriptionPage.confirmSubmission();

        await expect(this.homePage.summaryTab).toBeVisible();
        await this.homePage.delay(3000);

        await this.homePage.navigateToTab('History');
        await this.historyTab.verifyEventCompleted(languagePreferenceData.captionText);

        await this.homePage.navigateToTab('Welsh');
        await this.welshTab.verifyPageContentByKeyValue(languagePreferenceData.languagePreferenceQuestionLabelWithinWelshTab, 
            languagePreferenceData.welshlanguagePreferenceYesLabel);
    }
}
