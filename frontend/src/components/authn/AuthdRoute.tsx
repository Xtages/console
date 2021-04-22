import React from 'react';
import {Redirect, Route, RouteProps} from 'react-router-dom';
import {useAuth} from '../../hooks/useAuth';

/**
 * A {@link Route} that will only render {@link RouteProps.children} if there is
 * an authenticated user, otherwise it will redirect to `/login`.
 */
export default function AuthdRoute(props: RouteProps) {
  const {children, location, ...rest} = props;
  const auth = useAuth();

  if (auth.inProgress) {
    return <></>;
  }
  if (auth.principal != null) {
    return <Route {...rest}>{children}</Route>;
  }
  return (
    <Route {...rest}>
      <Redirect to={{
        pathname: '/login',
        state: {referrer: location},
      }}
      />
    </Route>
  );
}
