import {expect, Page} from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

async function axeTest(page: Page, testInfo): Promise<void> {
    // accessibility testing function
    const accessibilityScanResults = await new AxeBuilder({page})
        .withTags([
            "wcag2a",
            "wcag2aa",
            "wcag21a",
            "wcag21aa",
            "wcag22a",
            "wcag22aa",
        ])
        .analyze();

    await testInfo.attach('accessibility-scan-results', {
        body: JSON.stringify(accessibilityScanResults, null, 2),
        contentType: 'application/json'
    });


    expect(accessibilityScanResults.violations).toEqual([]);
}

export default axeTest;
