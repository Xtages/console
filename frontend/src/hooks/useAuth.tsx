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

const authContext = createContext<ProvideAuthType | null>(null);

interface ProvideAuthProps {
  children: ReactNode;
}

// Provider component that wraps your app and makes auth object
// available to any child component that calls useAuth().
export function ProvideAuth({children}: ProvideAuthProps) {
  const auth = useProvideAuth();
  return <authContext.Provider value={auth}>{children}</authContext.Provider>;
}

// Hook for child components to get the auth object
// and re-render when it changes.
export function useAuth() {
  return useContext(authContext);
}

class Principal {
  readonly id: string;
  readonly name: string;
  readonly email: string;
  readonly org: string;

  constructor(id: string, name: string, email: string, org: string) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.org = org;
  }
}

interface ProvideAuthType {
  principal: Principal | null;
  signIn: (email: string, password: string) => Promise<Principal>;
  signUp: (
    email: string,
    password: string,
    name: string,
    org: string,
  ) => Promise<Principal | null>;
  signOut: (global?: boolean) => Promise<void>;
}

// Provider hook that creates auth object and handles state
function useProvideAuth(): ProvideAuthType {
  async function cognitoUserToPrincipal(user: CognitoUser): Promise<Principal> {
    return await new Promise<Principal>((resolve, reject) => {
      user.getUserAttributes((error, attrList) => {
        if (error != null) {
          reject(error);
        }
        if (
          attrList == null ||
          attrList === undefined ||
          attrList.length === 0
        ) {
          reject(new Error());
        }
        const attrs = Object.fromEntries(
          attrList!.map((attr, index) => [attr.getName(), attr.getValue()]),
        );
        const principal = new Principal(
          user.getUsername(),
          attrs.name,
          attrs.email,
          attrs['custom:org'],
        );
        resolve(principal);
      });
    });
  }

  const [principal, setPrincipal] = useState<Principal | null>(null);

  async function signIn(email: string, password: string) {
    const user: CognitoUser = await Auth.signIn(email, password);
    const principal = await cognitoUserToPrincipal(user);
    setPrincipal(principal);
    return principal;
  }

  async function signUp(
    email: string,
    password: string,
    name: string,
    org: string,
  ) {
    const result = await Auth.signUp({
      username: email,
      password: password,
      attributes: {
        name: name,
        'custom:org': org,
      },
    });
    if (result.user != null) {
      const principal = await cognitoUserToPrincipal(result.user);
      setPrincipal(principal);
      return principal;
    }
    return null;
  }

  async function signOut(global = false) {
    await Auth.signOut({global: global});
    setPrincipal(null);
  }

  // Subscribe to auth events on mount.
  // Because this sets state in the callback it will cause any
  // component that utilizes this hook to re-render with the
  // latest auth object.
  useAsyncEffect(async () => {
    const listener: HubCallback = async data => {
      switch (data.payload.event) {
        case 'signIn': {
          console.log('signIn');
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
      }
    };

    Hub.listen('auth', listener);
    // Cleanup subscription on unmount
    return () => Hub.remove('auth', listener);
  }, []);

  // Return the user object and auth methods
  return {
    principal: principal,
    signIn,
    signUp,
    signOut,
  };
}
