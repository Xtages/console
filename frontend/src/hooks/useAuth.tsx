import {HubCallback} from '@aws-amplify/core';
import {CognitoUser} from 'amazon-cognito-identity-js';
import {Auth as CognitoAuth, Hub} from 'aws-amplify';
import React, {createContext, ReactNode, useContext, useState} from 'react';
import useAsyncEffect from 'use-async-effect';

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

  constructor({
    id,
    name,
    email,
    org,
  }: {
    id: string;
    name: string;
    email: string;
    org: string;
  }) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.org = org;
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
      org: attrs['custom:org'],
    });
  }
}

export type NullablePrincipal = Principal | null;

/**
 * A {@link Context} for authentication.
 *
 * @remarks
 * Althouth the context is of {@link Auth} or `null`, that's only because we can't provide
 * a sensible default on creation. However when calling {@link useAuth} this will never be
 * `null`.
 */
const AuthContext = createContext<Auth | null>(null);

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

/**
 * Provider hook that creates {@link Auth} object and handles state of the {@link Principal}
 * and the authentication process.
 *
 * @returns A {@link Auth} object. If {@link Auth.inProgress} is `true` then the authentication
 *    process still hasn't finished. If the user is authenticated then {@link Auth.principal}
 *    will be non-null.
 */
function useProvideAuth() {
  const [principal, setPrincipal] = useState<NullablePrincipal>(null);
  const [inProgress, setInProgress] = useState(true);

  /** Sign-in using email and password. */
  async function signIn({email, password}: {email: string; password: string}) {
    const user: CognitoUser = await CognitoAuth.signIn(email, password);
    const converted = await Principal.fromCognitoUser(user);
    setPrincipal(converted);
    return converted;
  }

  /** Sign-up using email, password, name and org. */
  async function signUp({
    email,
    password,
    name,
    org,
  }: {
    email: string;
    password: string;
    name: string;
    org: string;
  }) {
    const result = await CognitoAuth.signUp({
      username: email,
      password,
      attributes: {
        name,
        'custom:org': org,
      },
    });
    if (result.user != null) {
      const converted = await Principal.fromCognitoUser(result.user);
      setPrincipal(converted);
      return converted;
    }
    throw Error();
  }

  /**
   * Log-out.
   *
   * @param global - if `true` the user will be logged out of all devices.
   */
  async function logOut({global = false}: {global?: boolean} = {}) {
    await CognitoAuth.signOut({global});
    setPrincipal(null);
  }

  async function getPrincipal() {
    try {
      const user = await CognitoAuth.currentAuthenticatedUser();
      setPrincipal(await Principal.fromCognitoUser(user));
      setInProgress(false);
    } catch (e) {
      setInProgress(false);
    }
  }

  let listener: HubCallback | null;

  // Subscribe to auth events on mount.
  // Because this sets state in the callback it will cause any
  // component that utilizes this hook to re-render with the
  // latest auth object.
  useAsyncEffect(
    async (isMounted) => {
      if (isMounted()) {
        getPrincipal();
        listener = async (data) => {
          switch (data.payload.event) {
            case 'signIn': {
              const user = data.payload.data;
              if (principal == null) {
                setPrincipal(await Principal.fromCognitoUser(user));
              }
              break;
            }
            case 'signUp': {
              const user = data.payload.data;
              if (principal == null) {
                setPrincipal(await Principal.fromCognitoUser(user));
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
    signIn,
    signUp,
    logOut,
  };
}
