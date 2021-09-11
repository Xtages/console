import React from 'react';
import {Redirect, Route, RouteProps} from 'react-router-dom';
import {useAuth} from 'hooks/useAuth';

/**
 * A {@link Route} that will only render {@link RouteProps.component} if there is
 * an authenticated user otherwise it will redirect to `/login`.
 *
 * NOTE: Having an authenticated user doesn't imply that the user is part of an {@link Organization}
 * that is in good standing. Use {@link AuthdAndGoodStandingRoute} for that, instead.
 */
export default function AuthdRoute({location, ...props}: RouteProps) {
  const auth = useAuth();

  if (auth.inProgress) {
    return <></>;
  }
  if (auth.principal != null) {
    return <Route {...props} />;
  }
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
