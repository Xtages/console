import React from 'react';
import {BrowserRouter, Switch} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from 'react-query';
import AuthdRoute from 'components/authn/AuthdRoute';
import UnauthdRoute from 'components/authn/UnauthdRoute';
import {ProvideAuth} from 'hooks/useAuth';
import LoginPage from 'pages/authn/LoginPage';
import SignUpPage from 'pages/authn/SignUpPage';
import HomePage from 'pages/HomePage';
import ConfirmSignUpPage from 'pages/authn/ConfirmSignUpPage';
import AccountPage from 'pages/AccountPage';
import ProjectSettingsPage from 'pages/ProjectSettingsPage';
import ProjectPage from './pages/ProjectPage';
import CreateProjectPage from './pages/CreateProjectPage';

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
            <AuthdRoute path="/project/:name" exact component={ProjectPage} />
            <AuthdRoute path="/project/:name/settings" component={ProjectSettingsPage} />
            <AuthdRoute path="/new/project">
              <CreateProjectPage />
            </AuthdRoute>
            <AuthdRoute path="/">
              <HomePage />
            </AuthdRoute>
          </Switch>
        </BrowserRouter>
      </ProvideAuth>
    </QueryClientProvider>
  );
}
