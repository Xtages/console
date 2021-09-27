import {Auth as CognitoAuth} from 'aws-amplify';
import {CdApi,
  CheckoutApi,
  CiApi,
  Configuration,
  GitHubAppApi,
  LogsApi,
  OrganizationApi,
  ProjectApi,
  UsageApi,
  UserApi} from 'gen/api';
import {BaseAPI} from 'gen/api/base';

function getBasePath() {
  const url = new URL('/api/v1', window.location.href);
  return url.toString();
}

function buildAuthdApi<T extends BaseAPI>(ApiType: typeof BaseAPI): T {
  return new ApiType(new Configuration({
    basePath: getBasePath(),
    accessToken: async () => {
      const session = await CognitoAuth.currentSession();
      return session.getIdToken()
        .getJwtToken();
    },
  })) as T;
}

function buildUnauthdApi<T extends BaseAPI>(ApiType: typeof BaseAPI): T {
  return new ApiType(new Configuration({
    basePath: getBasePath(),
  })) as T;
}

export const checkoutApi = buildAuthdApi<CheckoutApi>(CheckoutApi);
export const organizationApi = buildAuthdApi<OrganizationApi>(OrganizationApi);
export const unauthdOrganizationApi = buildUnauthdApi<OrganizationApi>(OrganizationApi);
export const projectApi = buildAuthdApi<ProjectApi>(ProjectApi);
export const logsApi = buildAuthdApi<LogsApi>(LogsApi);
export const ciApi = buildAuthdApi<CiApi>(CiApi);
export const cdApi = buildAuthdApi<CdApi>(CdApi);
export const usageApi = buildAuthdApi<UsageApi>(UsageApi);
export const userApi = buildAuthdApi<UserApi>(UserApi);
export const gitHubAppApi = buildAuthdApi<GitHubAppApi>(GitHubAppApi);
