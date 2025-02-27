/* eslint-disable complexity */
import { request } from '@playwright/test';
import { urls } from '../../../../config/config';
import pipPayload from '../../../data/payload/create-appeal/pip_sya.json';
import pipPayloadWelsh from '../../../data/payload/create-appeal/pip_sya_welsh.json';
import ucPayload from '../../../data/payload/create-appeal/uc_sya.json';
import esaPayload from '../../../data/payload/create-appeal/esa_sya.json';
import childSupportPayload from '../../../data/payload/create-appeal/child_support_sya.json';
import taxCreditPayload from '../../../data/payload/create-appeal/tax_credit_sya.json';
import pipSandLPayload from '../../../data/payload/create-appeal/pip_sandl_sya.json';
import dlaSandLPayload from '../../../data/payload/create-appeal/dla_sandl_sya.json';
import ucSandLVideoPayload from '../../../data/payload/create-appeal//uc_sandl_video_sya.json';
import piprepFtoFSandLPayload from '../../../data/payload/create-appeal/pip_sandl_rep_ftof.json';
import piprepSandLPayload from '../../../data/payload/create-appeal/pip_sandl_rep.json';
import pipIncompleteAppealPayload from '../../../data/payload/create-appeal/pip_incomplete_appeal.json';
import pipNonCompliantAppealPayload from '../../../data/payload/create-appeal/pip_non_compliant_appeal.json';
import ibcPayload from '../../../data/payload/create-appeal/ibca_sya.json';
import { StringUtilsComponent } from '../../../../utils/StringUtilsComponent';

async function createCaseBasedOnCaseType(caseType: string) {
  let apiContext;
  let dataPayload;

  //Formulate API Context For Request,wrapping the Request Endpoint
  apiContext = await request.newContext({
    // All requests we send go to this API Endpoint.
    baseURL: urls.tribunalsApiUri
  });

  const payloadMap: { [key: string]: any } = {
    PIP: pipPayload,
    UC: ucPayload,
    ESA: esaPayload,
    CHILDSUPPORT: childSupportPayload,
    'TAX CREDIT': taxCreditPayload,
    PIPSANDL: pipSandLPayload,
    DLASANDL: dlaSandLPayload,
    UCSANDL: ucSandLVideoPayload,
    PIPREPINTERSANDL: piprepFtoFSandLPayload,
    PIPREPSANDL: piprepSandLPayload,
    PIPINCOMPLETE: pipIncompleteAppealPayload,
    PIPNONCOMPLIANT: pipNonCompliantAppealPayload,
    WELSHPIP: pipPayloadWelsh,
    IBC: ibcPayload,
  };

  dataPayload = payloadMap[caseType] || new Error('Unsupported case type');

  let caseTypeLower = caseType.toLowerCase();
  let apiUrl =
    caseTypeLower.includes('incomplete') ||
    caseTypeLower.includes('noncompliant')
      ? `${urls.tribunalsApiUri}/appeals`
      : `${urls.tribunalsApiUri}/api/appeals`;

  if (caseTypeLower.includes('noncompliant')) {
    dataPayload.appellant.nino = StringUtilsComponent.getRandomNINumber();
  }

  const response = await apiContext.post(apiUrl, {
    data: dataPayload
  });
  const respHeaders = response.headers();
  const locationUrl: string = respHeaders.location;
  return locationUrl.substring(locationUrl.lastIndexOf('/') + 1);
}

export default createCaseBasedOnCaseType;
