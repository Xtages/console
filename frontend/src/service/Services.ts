import {Auth as CognitoAuth} from 'aws-amplify';
import {CheckoutApi, Configuration, OrganizationApi} from '../gen/api';
import {BaseAPI} from '../gen/api/base';

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

const checkoutApi = buildAuthdApi<CheckoutApi>(CheckoutApi);
const organizationApi = buildAuthdApi<OrganizationApi>(OrganizationApi);

export {checkoutApi, organizationApi};
