/* eslint-disable complexity */
import { request } from '@playwright/test';
import { urls, resources } from '../../../config/config';
import logger from '../../../utils/loggerUtil';
import NodeCache from 'node-cache';
//Idam access token expires for every 8 hrs
const tokenIDCache = new NodeCache({
  stdTTL: 25200,
  checkperiod: 1800,
  maxKeys: 12,
  deleteOnExpire: true
});

export async function accessToken(user) {
  if (tokenIDCache.get(user.email) != null) {
    return tokenIDCache.get(user.email);
  } else {
    if (user.email && user.password) {
      const accessToken = await getIDAMUserToken(user);
      tokenIDCache.set(user.email, accessToken);
      return accessToken;
    } else {
      logger.error(
        '******* Missing user details. Cannot get access token ******'
      );
    }
  }
}

export async function accessId(user) {
  let user_id_key = user.email + '_id';
  if (tokenIDCache.get(user_id_key) != null) {
    return tokenIDCache.get(user_id_key);
  } else {
    if (user.email && user.password) {
      const idamToken = await accessToken(user);
      const userId = await getIDAMUserID(idamToken);
      tokenIDCache.set(user_id_key, userId);
      return userId;
    } else {
      logger.error(
        '******* Missing user details. Cannot get the user id ******'
      );
    }
  }
}

async function getIDAMUserToken(user) {
  const scope = 'openid profile roles';
  const grantType = 'password';
  const idamTokenPath = '/o/token';
  let apiContext;

  //Formulate API Context For Request,wrapping the Request Endpoint
  apiContext = await request.newContext({
    // All requests we send go to this API Endpoint.
    baseURL: urls.idamUrl,
    extraHTTPHeaders: {
      // We set this header per GitHub guidelines.
      'content-type': 'application/x-www-form-urlencoded',
      Accept: 'application/json'
    }
  });
  const response = await apiContext.post(`${urls.idamUrl}${idamTokenPath}`, {
    headers: {
      'content-type': 'application/x-www-form-urlencoded',
      Accept: 'application/json'
    },
    form: {
      grant_type: `${grantType}`,
      client_secret: `${resources.idamClientSecret}`,
      client_id: `${resources.idamClientId}`,
      scope: `${scope}`,
      redirect_uri: `${resources.idamClientRedirect}`,
      username: `${user.email}`,
      password: `${user.password}`
    }
  });
  const body: string = await response.body();
  await response.dispose();
  return JSON.parse(body).access_token;
}

export async function getSSCSServiceToken() {
  const s2sTokenPath = '/testing-support/lease';

  let apiContext = await request.newContext({
    // All requests we send go to this API Endpoint.
    baseURL: urls.s2sUrl,
    extraHTTPHeaders: {
      'Content-Type': 'application/x-www-form-urlencoded',
      Accept: 'application/json'
    }
  });

  const response = await apiContext.post(`${urls.s2sUrl}${s2sTokenPath}`, {
    headers: {
      'content-type': 'application/json'
    },
    data: { microservice: `${resources.idamClientId}` }
  });
  const body = await response.body();
  await response.dispose();
  return body.toString();
}

export async function getIDAMUserID(idamToken) {
  const idamDetailsPath = '/details';

  let apiContext = await request.newContext({
    // All requests we send go to this API Endpoint.
    baseURL: urls.idamUrl,
    extraHTTPHeaders: {
      'content-type': 'application/json'
    }
  });

  const response = await apiContext.get(`${urls.idamUrl}${idamDetailsPath}`, {
    headers: {
      Authorization: `Bearer ${idamToken}`,
      'content-type': 'application/json'
    }
  });
  const body = await response.body();
  // @ts-ignore
  return JSON.parse(body).id;
}
