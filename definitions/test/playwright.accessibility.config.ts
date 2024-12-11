import { defineConfig, devices } from "@playwright/test";
import config from "./playwright.config";

export default defineConfig(
    config,
    {
        reporter: [["html", { open: 'never',
        outputDir: 'accessibility-report',
        host: '0.0.0.0',
        port: 9223,  printSteps: true}]]
    }
)
