import {HubCallback} from '@aws-amplify/core';
import {CognitoUser} from 'amazon-cognito-identity-js';
import {Auth, Hub} from 'aws-amplify';
import React, {createContext, ReactNode, useContext, useState} from 'react';
import useAsyncEffect from 'use-async-effect';

Auth.configure({
  aws_project_region: 'us-east-1',
  userPoolId: 'us-east-1_F4FzoEObF',
  userPoolWebClientId: '4iilttvg5aqlnnujs088tisjl',
});

const AuthContext = createContext<ProvideAuthType | null>(null);

// Provider component that wraps your app and makes auth object
// available to any child component that calls useAuth().
export function ProvideAuth({children}: {children: ReactNode}) {
  const auth = useProvideAuth();
  return <AuthContext.Provider value={auth}>{children}</AuthContext.Provider>;
}

// Hook for child components to get the auth object
// and re-render when it changes.
export function useAuth() {
  return useContext(AuthContext);
}

export class Principal {
  readonly id: string;

  readonly name: string;

  readonly email: string;

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
}

export type NullablePrincipal = Principal | null;

interface ProvideAuthType {
  getPrincipal: () => Promise<NullablePrincipal>;
  signIn: (args: {email: string; password: string}) => Promise<Principal>;
  signUp: (args: {
    email: string;
    password: string;
    name: string;
    org: string;
  }) => Promise<NullablePrincipal>;
  logOut: (args?: {global?: boolean}) => Promise<void>;
}

async function cognitoUserToPrincipal(user: CognitoUser): Promise<Principal> {
  const attrList = await Auth.userAttributes(user);
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

// Provider hook that creates auth object and handles state
function useProvideAuth(): ProvideAuthType {
  const [principal, setPrincipal] = useState<NullablePrincipal>(null);

  async function signIn({email, password}: {email: string; password: string}) {
    const user: CognitoUser = await Auth.signIn(email, password);
    const converted = await cognitoUserToPrincipal(user);
    setPrincipal(converted);
    return converted;
  }

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
    const result = await Auth.signUp({
      username: email,
      password,
      attributes: {
        name,
        'custom:org': org,
      },
    });
    if (result.user != null) {
      const converted = await cognitoUserToPrincipal(result.user);
      setPrincipal(converted);
      return converted;
    }
    return null;
  }

  async function logOut({global = false}: {global?: boolean} = {}) {
    await Auth.signOut({global});
    setPrincipal(null);
  }

  async function getPrincipal() {
    if (principal != null) {
      return principal;
    }
    let user: CognitoUser | null;
    try {
      user = await Auth.currentAuthenticatedUser();
    } catch (e) {
      user = null;
    }
    if (user != null) {
      return cognitoUserToPrincipal(user);
    }
    return null;
  }

  let listener: HubCallback | null;

  // Subscribe to auth events on mount.
  // Because this sets state in the callback it will cause any
  // component that utilizes this hook to re-render with the
  // latest auth object.
  useAsyncEffect(
    async (isMounted) => {
      if (isMounted()) {
        listener = async (data) => {
          switch (data.payload.event) {
            case 'signIn': {
              const user = data.payload.data;
              setPrincipal(await cognitoUserToPrincipal(user));
              break;
            }
            case 'signUp': {
              const user = data.payload.data;
              setPrincipal(await cognitoUserToPrincipal(user));
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

  // Return the user object and auth methods
  return {
    getPrincipal,
    signIn,
    signUp,
    logOut,
  };
}
