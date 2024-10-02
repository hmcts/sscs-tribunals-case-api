import { Page } from '@playwright/test';
import { WebAction } from '../common/web.action';
import languagePreferenceData from './content/update.langauge.preference_en.json';

let webAction: WebAction;

export class UpdateLanguagePreferencePage {

    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        webAction = new WebAction(this.page);
    }

    async verifyPageContent() {
        await webAction.verifyPageLabel('span.govuk-caption-l', languagePreferenceData.captionText); //Caption text
        await webAction.verifyPageLabel('h1.govuk-heading-l', languagePreferenceData.heading); //Heading text
        await webAction.verifyPageLabel('legend span.form-label', languagePreferenceData.languagePreferenceQuestionLabel); //Heading text
    }

    async chooseWelshLanguage(yesOrNo: string): Promise<void> {
        await this.page.locator(`label[for='languagePreferenceWelsh_${yesOrNo}']`).click();
    }

    async verifyCYAPageContent(): Promise<void> {
        await webAction.verifyPageLabel('h1.govuk-heading-l', languagePreferenceData.captionText); //Heading text
        await webAction.verifyPageLabel('form.check-your-answers h2.heading-h2', languagePreferenceData.checkYourAnswersHeading);//Check your answers text
    }

    async confirmSubmission(): Promise<void> {
        await webAction.clickButton('Submit');
    }

    async cancelEvent(): Promise<void> {
        await webAction.clickLink("Cancel");
    }
}