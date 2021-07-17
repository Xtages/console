import React from 'react';
import {BrowserRouter, Switch} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from 'react-query';
import AuthdRoute from 'components/authn/AuthdRoute';
import UnauthdRoute from 'components/authn/UnauthdRoute';
import {ProvideAuth} from 'hooks/useAuth';
import loadable from '@loadable/component';

const ProjectPage = loadable(() => import('pages/ProjectPage'));
const CreateProjectPage = loadable(() => import('pages/CreateProjectPage'));
const LoginPage = loadable(() => import('pages/authn/LoginPage'));
const SignUpPage = loadable(() => import('pages/authn/SignUpPage'));
const HomePage = loadable(() => import('pages/HomePage'));
const ConfirmSignUpPage = loadable(() => import('pages/authn/ConfirmSignUpPage'));
const AccountPage = loadable(() => import('pages/AccountPage'));
const ProjectSettingsPage = loadable(() => import('pages/ProjectSettingsPage'));
const ChangePasswordPage = loadable(() => import('pages/authn/ChangePasswordPage'));
const DeploymentsPage = loadable(() => import('pages/DeploymentsPage'));
const queryClient = new QueryClient();

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ProvideAuth>
        <BrowserRouter>
          <Switch>
            <UnauthdRoute path="/login" component={LoginPage} />
            <UnauthdRoute path="/signup" component={SignUpPage} />
            <UnauthdRoute path="/changePassword" component={ChangePasswordPage} />
            <UnauthdRoute path="/confirm" component={ConfirmSignUpPage} />
            <AuthdRoute path="/account" component={AccountPage} />
            <AuthdRoute path="/project/:name" exact component={ProjectPage} />
            <AuthdRoute path="/project/:name/settings" component={ProjectSettingsPage} />
            <AuthdRoute path="/new/project" component={CreateProjectPage} />
            <AuthdRoute path="/deployments/:projectName/:env" component={DeploymentsPage} />
            <AuthdRoute path="/deployments" component={DeploymentsPage} />
            <AuthdRoute path="/" component={HomePage} />
          </Switch>
        </BrowserRouter>
      </ProvideAuth>
    </QueryClientProvider>
  );
}
