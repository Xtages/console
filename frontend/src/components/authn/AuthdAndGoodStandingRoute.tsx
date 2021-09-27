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
  const {inProgress, organization, principal} = useAuth();

  if (inProgress) {
    return <></>;
  }
  if (principal !== null && (organization === null || isOrgInGoodStanding(organization))) {
    return <Route {...props} />;
  }
  if (principal === null) {
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
  if (organization && !isOrgInGoodStanding(organization)) {
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
