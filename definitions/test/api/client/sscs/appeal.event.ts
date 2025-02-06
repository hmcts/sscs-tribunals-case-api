/* eslint-disable complexity */
import {
  accessId,
  accessToken,
  getSSCSServiceToken
} from '../idam/idam.service';
import { credentials } from '../../../config/config';
import { performEventOnCaseWithEmptyBody } from './factory/appeal.update.factory';
import logger from '../../../utils/loggerUtil';

export default async function performAppealDormantOnCase(caseId: string) {
  let token: string = await accessToken(credentials.amSuperUser);
  logger.debug('The value of the IDAM Token : ' + token);
  let serviceToken: string = await getSSCSServiceToken();
  let userId: string = await accessId(credentials.amSuperUser);
  await new Promise((f) => setTimeout(f, 3000));
  await performEventOnCaseWithEmptyBody(
    token.trim(),
    serviceToken.trim(),
    userId.trim(),
    'SSCS',
    'Benefit',
    caseId.trim(),
    'appealDormant'
  );
}
