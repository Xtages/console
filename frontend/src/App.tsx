import React from 'react';
import {BrowserRouter, Switch} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from 'react-query';
import AuthdRoute from 'components/authn/AuthdRoute';
import UnauthdRoute from 'components/authn/UnauthdRoute';
import {ProvideAuth} from 'hooks/useAuth';
import LoginPage from 'pages/authn/LoginPage';
import SignUpPage from 'pages/authn/SignUpPage';
import HomePage from 'pages/home/HomePage';
import ConfirmSignUpPage from 'pages/authn/ConfirmSignUpPage';
import AccountPage from 'pages/account/AccountPage';

const queryClient = new QueryClient();

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ProvideAuth>
        <BrowserRouter>
          <Switch>
            <UnauthdRoute path="/login" component={LoginPage} />
            <UnauthdRoute path="/signup">
              <SignUpPage />
            </UnauthdRoute>
            <UnauthdRoute path="/confirm" component={ConfirmSignUpPage} />
            <AuthdRoute path="/account" component={AccountPage} />
            <AuthdRoute path="/">
              <HomePage />
            </AuthdRoute>
          </Switch>
        </BrowserRouter>
      </ProvideAuth>
    </QueryClientProvider>
  );
}
