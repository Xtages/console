import React from 'react';
import {Redirect, Route, RouteProps} from 'react-router-dom';
import {useAuth} from 'hooks/useAuth';
import {isOrgInGoodStanding} from 'helpers/organization';

/**
 * A {@link Route} that will only render {@link RouteProps.component} if there is
 * an authenticated user, it's associated to an {@link Organization} and the {@link Organization} is
 * in good standing, otherwise it will redirect to `/login`.
 */
export default function AuthdAndGoodStandingRoute({location, ...props}: RouteProps) {
  const auth = useAuth();

  if (auth.inProgress) {
    return <></>;
  }
  if (auth.principal !== null && isOrgInGoodStanding(auth.organization)) {
    return <Route {...props} />;
  }
  if (auth.principal === null) {
    return (
      <Route {...props}>
        <Redirect to={{
          pathname: '/login',
          state: {referrer: location},
        }}
        />
      </Route>
    );
  }
  if (!isOrgInGoodStanding(auth.organization)) {
    return (
      <Route {...props}>
        <Redirect to={{
          pathname: '/badorg',
          state: {referrer: location},
        }}
        />
      </Route>
    );
  }
  return <></>;
}
