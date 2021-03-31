import React, {useEffect, useState} from 'react';
import {Redirect, Route, RouteProps} from 'react-router-dom';
import {NullablePrincipal, useAuth} from '../../hooks/useAuth';

export default function UnauthdRoute(props: RouteProps) {
  const {children, location, ...rest} = props;
  const auth = useAuth()!;
  const [authing, setAuthing] = useState(true);
  const [principal, setPrincipal] = useState<NullablePrincipal>(null);

  const fetchPrincipal = async () => {
    const p = await auth.getPrincipal();
    setPrincipal(p);
    setAuthing(false);
  };

  useEffect(() => {
    fetchPrincipal();
  }, []);

  if (authing) {
    return <></>;
  }
  return (
    <Route
      {...rest}
      render={() => (principal == null ? (
        children
      ) : (
        <Redirect
          to={{
            pathname: '/',
            state: {referrer: location},
          }}
        />
      ))}
    />
  );
}
