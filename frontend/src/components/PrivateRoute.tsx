/* eslint-disable prettier/prettier */
/* eslint-disable multiline-ternary */
/* eslint-disable @typescript-eslint/indent */
import React from 'react';
import {Redirect, Route, RouteProps} from 'react-router-dom';
import {useAuth} from '../hooks/useAuth';

export default function PrivateRoute(props: RouteProps) {
  const {children, location, ...rest} = props;
  const auth = useAuth()!;
  return (
    <Route
      {...rest}
      render={props =>
        auth.principal != null ? (
              children
            ) : (
          <Redirect
            to={{
              pathname: '/login',
              state: {from: location},
            }}
          />
            )
      }
    />
  );
}
