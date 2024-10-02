import path from "path";
const fs = require('fs');
const yaml = require('js-yaml');

export const urls = {

  tribunalsApiUri: process.env.TEST_E2E_API_URI || '',
  xuiUrl: process.env.TEST_E2E_URL_WEB || '',
  idamUrl : process.env.IDAM_URL || 'https://idam-api.aat.platform.hmcts.net',
  s2sUrl : process.env.S2S_URL || 'http://rpe-service-auth-provider-aat.service.core-compute-aat.internal',
  ccdApiUrl : process.env.CCD_API_URL || 'http://ccd-data-store-api-aat.service.core-compute-aat.internal'
};

console.log(`CCD API URL: ${urls.ccdApiUrl}`);

const versionFilePath = path.resolve(__dirname, '../../benefit/VERSION.yaml');
export const environment = {
  name: process.env.ENVIRONMENT ?? '',
  hearingsEnabled: process.env.HEARINGS_ENABLED,
  aatDefVersion: yaml.load(fs.readFileSync(versionFilePath, 'utf8'))
}

export const credentials = {
  caseWorker: {
    email: process.env.TEST_CASEOFFICER_USERNAME,
    password: process.env.TEST_CASEOFFICER_PASSWORD,
  },
  amCaseWorker: {
    email: process.env.TEST_AM_CASEOFFICER_USERNAME,
    password: process.env.TEST_AM_CASEOFFICER_PASSWORD,
  },
  amCaseWorkerWithCaseAllocatorRole: {
    email: process.env.TEST_AM_CASEOFFICER_CASE_ALLOCATOR_USERNAME,
    password: process.env.TEST_AM_CASEOFFICER_CASE_ALLOCATOR_PASSWORD,
  },
  seniorCaseworker: {
    email: process.env.SENIOR_CASEWORKER_USERNAME,
    password:
      process.env.SENIOR_CASEWORKER_PASSWORD,
  },
  hearingCentreAdmin: {
    email:
      process.env.HEARING_CENTRE_ADMIN_USERNAME,
    password:
      process.env.HEARING_CENTRE_ADMIN_PASSWORD,
  },
  hearingCentreTeamLead: {
    email:
      process.env.HEARING_CENTRE_TEAM_LEAD_USERNAME,
    password:
      process.env.HEARING_CENTRE_TEAM_LEAD_PASSWORD,
  },
  judge: {
    email: process.env.TEST_JUDGE_USERNAME,
    password: process.env.TEST_JUDGE_PASSWORD,
  },
  seniorJudge: {
    email: process.env.SENIOR_JUDGE_USERNAME,
    password: process.env.SENIOR_JUDGE_PASSWORD,
  },
  salariedJudge: {
    email: process.env.TEST_SALARIED_JUDGE_USERNAME,
    password: process.env.TEST_SALARIED_JUDGE_PASSWORD
  },
  feePaidJudge: {
    email: process.env.TEST_FEE_PAID_JUDGE_USERNAME,
    password: process.env.TEST_FEE_PAID_JUDGE_PASSWORD
  },
  amTribunalCaseWorker: {
    email: process.env.TEST_AM_TRIBUNAL_CASEWORKER_USERNAME,
    password: process.env.TEST_AM_TRIBUNAL_CASEWORKER_PASSWORD,
  },
  respondent: {
    email: process.env.RESPONDENT_USERNAME,
    password: process.env.RESPONDENT_PASSWORD,
  },
  citizen: {
    email: process.env.CITIZEN_USERNAME,
    password: process.env.CITIZEN_PASSWORD,
  },
  superUser: {
    email: process.env.SUPER_USER_USERNAME,
    password: process.env.SUPER_USER_PASSWORD,
  },
  dwpResponseWriter: {
    email: process.env.TEST_DWP_USERNAME,
    password: process.env.TEST_DWP_PASSWORD,
  },
  hmrcUser: {
    email: process.env.TEST_HMRC_USERNAME,
    password: process.env.TEST_HMRC_PASSWORD,
  },
  amSuperUser: {
    email: process.env.TEST_AM_SUPERUSER_USERNAME,
    password: process.env.TEST_AM_SUPERUSER_PASSWORD,
  },
  amSeniorTribunalCaseWorkerWithCaseAllocatorRole: {
    email: process.env.TEST_AM_SENIOR_TRIBUNAL_CASEWORKER_CASE_ALLOCATOR_USERNAME,
    password: process.env.TEST_AM_SENIOR_TRIBUNAL_CASEWORKER_CASE_ALLOCATOR_PASSWORD,
  },
  amRegionalCenterAdmin: {
    email: process.env.TEST_AM_REGIONAL_CENTER_ADMIN_USERNAME,
    password: process.env.TEST_AM_REGIONAL_CENTER_ADMIN_PASSWORD,
  },
  amRegionalCenterTeamLeader: {
    email: process.env.TEST_AM_REGIONAL_CENTER_TEAM_LEADER_USERNAME,
    password: process.env.TEST_AM_REGIONAL_CENTER_TEAM_LEADER_PASSWORD,
  }
};

export const resources = {
  idamClientId : process.env.IDAM_OAUTH2_CLIENT_ID,
  idamClientSecret : process.env.IDAM_OAUTH2_CLIENT_SECRET,
  idamClientRedirect : process.env.IDAM_OAUTH2_REDIRECT_URL
}

export const paths = {

  testFile: path.resolve(__dirname, "../tests/fixtures/file/mockFile.txt"),
  testPdfFile: path.resolve(
      __dirname,
      "../tests/fixtures/file/mockFile.pdf",
  ),
  testWordFile: path.resolve(
      __dirname,
      "../tests/fixtures/file/mockFile.docx",
  ),
  testOdtFile: path.resolve(
      __dirname,
      "../tests/fixtures/file/mockFile.odt",
  ),
};

export const timeouts = {
  shortTimeout: 5000,
  mediumTimout: 60000,
  longTimeout: 120000,
  maxTimeout: 180000
};

