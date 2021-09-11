import React from 'react';
import {Redirect, Route, RouteProps} from 'react-router-dom';
import {useAuth} from 'hooks/useAuth';

/**
 * A {@link Route} that will only render {@link RouteProps.component} if there
 * *is not* an authenticated user, otherwise it will redirect to `/`.
 */
export default function UnauthdRoute({location, ...props}: RouteProps) {
  const auth = useAuth();

  if (auth.inProgress) {
    return <></>;
  }
  if (auth.principal == null) {
    return <Route {...props} />;
  }
  return (
    <Route {...props}>
      <Redirect to={{
        pathname: '/',
        state: {referrer: location},
      }}
      />
    </Route>
  );
}
