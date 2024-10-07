import {credentials} from "../config/config";
import {Page} from "@playwright/test";

interface UserLoginInfo {
    username: string;
    password: string;
}

type IdamLogin = {
    fields: UserLoginInfo;
    submitButton: string;
    signInUser(
        page: Page,
        user: keyof typeof credentials,
        application: string,
    ): Promise<void>;
};

const idamLoginHelper: IdamLogin = {
    fields: {
        username: "#username",
        password: "#password",
    },
    submitButton: 'input[value="Sign in"]',

    async signInUser(
        page: Page,
        user: keyof typeof credentials,
        application: string,
    ): Promise<void> {
        if (!page.url().includes("idam-web-public.")) {
            await page.goto(application);
        }
        await page.waitForSelector(
            `#skiplinktarget:text("Sign in or create an account")`,
        );
        await page.fill(this.fields.username, credentials.caseWorker.email);
        await page.fill(this.fields.password, credentials.caseWorker.password);
        await page.click(this.submitButton);

    },
};

export default idamLoginHelper;
