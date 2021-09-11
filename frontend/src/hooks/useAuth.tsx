import {HubCallback} from '@aws-amplify/core';
import {CognitoUser} from 'amazon-cognito-identity-js';
import {Auth as CognitoAuth, Hub} from 'aws-amplify';
import React, {createContext, ReactNode, useContext, useState} from 'react';
import useAsyncEffect from 'use-async-effect';
import {Nullable} from 'types/nullable';
import {Organization} from 'gen/api';
import {organizationApi} from 'service/Services';
import {useQueryClient} from 'react-query';
import axios from 'axios';

CognitoAuth.configure({
  aws_project_region: process.env.REACT_APP_COGNITO_REGION,
  userPoolId: process.env.REACT_APP_COGNITO_USER_POOL_ID,
  userPoolWebClientId: process.env.REACT_APP_COGNITO_USER_POOL_WEB_CLIENT_ID,
});

/**
 * Object representing the currently authenticated user.
 */
export class Principal {
  /** This is the Cognito id of the user */
  readonly id: string;

  /** User's name */
  readonly name: string;

  /** User's email */
  readonly email: string;

  /** User's organization */
  readonly org: string;

  /** Cognito user */
  readonly cognitoUser: CognitoUser;

  constructor({
    id,
    name,
    email,
    org,
    cognitoUser,
  }: {
    id: string;
    name: string;
    email: string;
    org: string;
    cognitoUser: CognitoUser;
  }) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.org = org;
    this.cognitoUser = cognitoUser;
  }

  /**
   * Turns a {@link CognitoUser} into a {@link Principal}.
   *
   * @param user - The CongnitoUser.
   * @returns A {@link Promise} of {@link Principal} from the {@link user}.
   */
  static async fromCognitoUser(user: CognitoUser): Promise<Principal> {
    const attrList = await CognitoAuth.userAttributes(user);
    const attrs = Object.fromEntries(
      attrList.map((attr) => [attr.getName(), attr.getValue()]),
    );
    return new Principal({
      id: user.getUsername(),
      name: attrs.name,
      email: attrs.email,
      org: attrs['custom:organization'],
      cognitoUser: user,
    });
  }
}

/**
 * A {@link Context} for authentication.
 *
 * @remarks
 * Althouth the context is of {@link Auth} or `null`, that's only because we can't provide
 * a sensible default on creation. However when calling {@link useAuth} this will never be
 * `null`.
 */
const AuthContext = createContext<Nullable<Auth>>(null);

/**
 * Provider component that wraps the app and makes an {@link Auth} object
 * available to any child component that calls {@link useAuth}. The `Auth` object
 * will never be `null`;
 */
export function ProvideAuth({children}: {children: ReactNode}) {
  const auth = useProvideAuth();
  return <AuthContext.Provider value={auth}>{children}</AuthContext.Provider>;
}

/**
 * Hook for child components to get the {@link Auth} object and re-render when it changes.
 */
export function useAuth() {
  return useContext(AuthContext)!;
}

type Auth = ReturnType<typeof useProvideAuth>;

export type Credentials = {
  username: string;
  password: string;
};

type SignUpValues = {
  name: string;
  orgName: string;
} & Credentials;

type CognitoUserWithChallenge = CognitoUser & {
  challengeName: any;
};

/**
 * Provider hook that creates {@link Auth} object and handles state of the {@link Principal}
 * and the authentication process.
 *
 * @returns A {@link Auth} object. If {@link Auth.inProgress} is `true` then the authentication
 *    process still hasn't finished. If the user is authenticated then {@link Auth.principal}
 *    will be non-null.
 */
function useProvideAuth() {
  const [principal, setPrincipal] = useState<Nullable<Principal>>(null);
  const [inProgress, setInProgress] = useState(true);
  const [organization, setOrganization] = useState<Nullable<Organization>>(null);
  const queryClient = useQueryClient();

  /**
   * Sign-in using email and password. This function will not update the {@link Principal} state,
   * this is necessary so `<AuthdRoute>`s are not refreshed when the state is changed.
   */
  async function logInForOrgSignup({
    username,
    password,
  }: Credentials):
    Promise<Principal | CognitoUserWithChallenge> {
    const user: CognitoUserWithChallenge = await CognitoAuth.signIn(username, password);
    if (user.challengeName) {
      return user;
    }
    return Principal.fromCognitoUser(user);
  }

  async function fetchOrg() {
    if (organization === null) {
      try {
        const response = await queryClient.fetchQuery('org', () => organizationApi.getOrganization());
        setOrganization(response.data);
      } catch (e: any) {
        if (!axios.isAxiosError(e) || (axios.isAxiosError(e) && e.response?.status !== 404)) {
          throw e;
        }
      }
    }
  }

  /** Sign-in using email and password. */
  async function logIn({
    username,
    password,
  }: Credentials):
    Promise<Principal | CognitoUserWithChallenge> {
    const user: CognitoUserWithChallenge = await CognitoAuth.signIn(username, password);
    if (user.challengeName) {
      return user;
    }
    await fetchOrg();
    const converted = await Principal.fromCognitoUser(user);
    setPrincipal(converted);
    setInProgress(false);
    return converted;
  }

  /** Complete login after invitation. The user will have to change their password. */
  async function completeNewPassword({
    user,
    password,
  }: {user: CognitoUser, password: string}) {
    const loggedInUser = await CognitoAuth.completeNewPassword(user, password);
    const converted = await Principal.fromCognitoUser(loggedInUser);
    setPrincipal(converted);
    await fetchOrg();
    return converted;
  }

  /** Changes the user's password */
  async function changePassword({
    user,
    oldPassword,
    newPassword,
  }: {user: CognitoUser, oldPassword: string, newPassword: string}) {
    await CognitoAuth.changePassword(user, oldPassword, newPassword);
  }

  /**
   * Sign-up using email, password, name and org. If we get back a confirmed user then we return
   * a {@link Principal}, `null` otherwise.
   */
  async function signUp({
    username,
    password,
    name,
    orgName,
  }: SignUpValues): Promise<Nullable<Principal>> {
    const result = await CognitoAuth.signUp({
      username,
      password,
      attributes: {
        name,
        'custom:organization': orgName,
      },
    });
    if (result.user != null && result.userConfirmed) {
      const converted = await Principal.fromCognitoUser(result.user);
      setPrincipal(converted);
      await fetchOrg();
      return converted;
    }
    setPrincipal(null);
    return null;
  }

  /**
   * Confirms an user's sign up using a code emailed to the user.
   *
   * N.B.: Confirming the user doesn't automatically signs them in.
   *
   * @param email - The user's email that was used to sign up.
   * @param code - The emailed code.
   */
  async function confirmSignUp({
    email,
    code,
  }: {email: string, code: string}) {
    return CognitoAuth.confirmSignUp(email, code);
  }

  /**
   * Resends the confirmation code the user's email.
   * @param email - Where the confirmation code is sent.
   */
  async function resendConfirmationCode({email}: {email: string}) {
    return CognitoAuth.resendSignUp(email);
  }

  /**
   * Log-out.
   *
   * @param global - if `true` the user will be logged out of all devices.
   */
  async function logOut({global = false}: {global?: boolean} = {}) {
    setPrincipal(null);
    setOrganization(null);
    queryClient.clear();
    await CognitoAuth.signOut({global});
  }

  async function getPrincipal() {
    try {
      const user: CognitoUserWithChallenge = await CognitoAuth.currentAuthenticatedUser();
      if (!user.challengeName) {
        await fetchOrg();
        setPrincipal(await Principal.fromCognitoUser(user));
        setInProgress(false);
      }
    } catch (e) {
      setInProgress(false);
    }
  }

  let listener: Nullable<HubCallback>;

  // Subscribe to auth events on mount.
  // Because this sets state in the callback it will cause any
  // component that utilizes this hook to re-render with the
  // latest auth object.
  useAsyncEffect(
    async (isMounted) => {
      if (isMounted()) {
        await getPrincipal();
        listener = async (data) => {
          switch (data.payload.event) {
            case 'signIn': {
              const user = data.payload.data;
              if (principal == null && user && user.isConfirmed && !user.challengeName) {
                setPrincipal(await Principal.fromCognitoUser(user));
                await fetchOrg();
              }
              break;
            }
            case 'signUp': {
              const user = data.payload.data;
              if (principal == null && user && user.userConfirmed) {
                setPrincipal(await Principal.fromCognitoUser(user));
                await fetchOrg();
              }
              break;
            }
            case 'signOut':
              setPrincipal(null);
              break;
            case 'signIn_failure':
              setPrincipal(null);
              break;
            case 'tokenRefresh_failure':
              setPrincipal(null);
              break;
            default:
          }
        };

        Hub.listen('auth', listener);
      }
    },
    () => listener != null && Hub.remove('auth', listener),
    [],
  );

  return {
    inProgress,
    principal,
    organization,
    logIn,
    logInForOrgSignup,
    completeNewPassword,
    changePassword,
    signUp,
    confirmSignUp,
    resendConfirmationCode,
    logOut,
  };
}
