import React from 'react';
import {BrowserRouter, Switch} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from 'react-query';
import AuthdRoute from 'components/authn/AuthdRoute';
import AuthdAndGoodStandingRoute from 'components/authn/AuthdAndGoodStandingRoute';
import UnauthdRoute from 'components/authn/UnauthdRoute';
import {ProvideAuth} from 'hooks/useAuth';
import loadable from '@loadable/component';
import {buildAnalytics} from 'service/AnalyticsService';
import {AnalyticsProvider} from 'use-analytics';
import {usePageViews} from 'hooks/usePageViews';
import {useFetchInstrumentation} from 'hooks/useFetchInstrumentation';
import {useXmlHttpRequestInstrumentation} from 'hooks/useXmlHttpRequestInstrumentation';
import {ReactQueryDevtools} from 'react-query/devtools';
import {useSurvicate} from 'hooks/useSurvicate';
import * as Sentry from '@sentry/react';
import {CenteredOnScreen} from 'components/layout/CenteredOnScreen';
import {Alert} from 'react-bootstrap';
import LinkedInTag from 'react-linkedin-insight';

const AccountPage = loadable(() => import('pages/AccountPage'));
const ChangePasswordPage = loadable(() => import('pages/authn/ChangePasswordPage'));
const ConfirmSignUpPage = loadable(() => import('pages/authn/ConfirmSignUpPage'));
const CreateProjectPage = loadable(() => import('pages/CreateProjectPage'));
const DeploymentsPage = loadable(() => import('pages/DeploymentsPage'));
const GitHubAppPostInstallPage = loadable(() => import('pages/GitHubAppPostInstallPage'));
const GitHubOauthPostInstallPage = loadable(() => import('pages/GitHubOauthPostInstallPage'));
const HomePage = loadable(() => import('pages/HomePage'));
const InvalidOrgPage = loadable(() => import('pages/authn/InvalidOrgPage'));
const LoginPage = loadable(() => import('pages/authn/LoginPage'));
const ProjectPage = loadable(() => import('pages/ProjectPage'));
const ProjectSettingsPage = loadable(() => import('pages/ProjectSettingsPage'));
const ResourcesPage = loadable(() => import('pages/ResourcesPage'));
const SignUpPage = loadable(() => import('pages/authn/SignUpPage'));
const StripePostCheckoutPage = loadable(() => import('pages/StripePostCheckoutPage'));
const UpgradePlanPage = loadable(() => import('pages/UpgradePlanPage'));

const queryClient = new QueryClient();
const analytics = buildAnalytics();

export default function App() {
  LinkedInTag.init('3590140', 'px');
  function errorFallback() {
    return (
      <CenteredOnScreen>
        <Alert variant="danger" className="text-lg">
          An unexpected error occurred. Please reload the page.
        </Alert>
      </CenteredOnScreen>
    );
  }

  function trackError(error: Error, componentStack: string, eventId: string) {
    analytics.track('Uncaught error', {
      ...error,
      componentStack,
      eventId,
    }).then(() => null);
  }

  return (
    <Sentry.ErrorBoundary showDialog fallback={errorFallback} onError={trackError}>
      <AnalyticsProvider instance={analytics}>
        <BrowserRouter>
          <InstrumentedApp />
        </BrowserRouter>
      </AnalyticsProvider>
    </Sentry.ErrorBoundary>
  );
}

function InstrumentedApp() {
  useSurvicate();
  useXmlHttpRequestInstrumentation();
  useFetchInstrumentation();
  usePageViews();
  return (
    <QueryClientProvider client={queryClient}>
      <ProvideAuth>
        <Switch>
          <UnauthdRoute path="/login" component={LoginPage} />
          <UnauthdRoute path="/signup" component={SignUpPage} />
          <UnauthdRoute path="/changePassword" component={ChangePasswordPage} />
          <UnauthdRoute path="/confirm" component={ConfirmSignUpPage} />
          <AuthdRoute path="/ghappinstalled" exact component={GitHubAppPostInstallPage} />
          <AuthdRoute path="/ghoauth" exact component={GitHubOauthPostInstallPage} />
          <AuthdRoute path="/badorg" component={InvalidOrgPage} />
          <AuthdRoute path="/checkoutdone" component={StripePostCheckoutPage} />
          <AuthdAndGoodStandingRoute path="/account" component={AccountPage} />
          <AuthdAndGoodStandingRoute path="/project/:name" exact component={ProjectPage} />
          <AuthdAndGoodStandingRoute
            path="/project/:name/settings"
            component={ProjectSettingsPage}
          />
          <AuthdAndGoodStandingRoute path="/new/project" component={CreateProjectPage} />
          <AuthdAndGoodStandingRoute
            path="/deployments/:projectName/:env"
            component={DeploymentsPage}
          />
          <AuthdAndGoodStandingRoute path="/deployments" component={DeploymentsPage} />
          <AuthdAndGoodStandingRoute path="/resources" component={ResourcesPage} />
          <AuthdAndGoodStandingRoute path="/upgrade" component={UpgradePlanPage} />
          <AuthdAndGoodStandingRoute path="/" component={HomePage} />
        </Switch>
      </ProvideAuth>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}
