/**
 * Builds an {@link URL} to authorize the GitHub OAuth app.
 * @param login The GitHub login name for the user.
 * @param cognitoUserId The cognitoUserId of the user.
 */
export function buildOauthUrl(login: string, cognitoUserId: string) {
  const url = new URL('https://github.com/login/oauth/authorize');
  url.searchParams.set('client_id', process.env.REACT_APP_GIT_HUB_OAUTH_CLIENT_ID!!);
  url.searchParams.set('login', login);
  url.searchParams.set('scope', 'repo user');
  url.searchParams.set('state', cognitoUserId);
  url.searchParams.set('allow_signup', false.toString());
  return url;
}
