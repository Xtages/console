import React from 'react';
import {useQuery} from 'react-query';
import {checkoutApi, resourceApi, userApi} from 'service/Services';
import {Activity, User} from 'react-feather';
import {Alert, Col} from 'react-bootstrap';
import {LoadIndicatingSection, Section, SectionTitle} from 'components/layout/Section';
import {UsageDashboard} from 'components/usage/UsageDashboard';
import {UserTable} from 'components/user/UserTable';
import {InviteUserFormCard} from 'components/user/InviteUserFormCard';
import {DocsLink} from 'components/link/XtagesLink';
import {useOrganization} from 'hooks/useOrganization';
import Page from '../components/layout/Page';

/**
 * A simple account page where the user can then click to go to their Striper customer portal.
 */
export default function AccountPage() {
  function is403Error(error: any) {
    const {
      isAxiosError,
      response,
    } = error;
    return isAxiosError && response.status === 403;
  }

  const customerPortalLinkQueryResult = useQuery(
    'customerPortalLink',
    () => checkoutApi.getCustomerPortalSession(), {
      retry: (failureCount, error) => failureCount <= 3 && !is403Error(error),
    },
  );

  const usageQueryResult = useQuery(
    'usage',
    () => resourceApi.getAllUsageDetails(),
  );

  const usersQueryResult = useQuery(
    'users',
    () => userApi.getUsers(), {
      retry: (failureCount, error) => failureCount <= 3 && !is403Error(error),
    },
  );

  const {orgNotFound} = useOrganization();

  function handleForbiddenError(error: any) {
    return is403Error(error) ? <></> : undefined;
  }

  return (
    <Page>
      {orgNotFound && (
        <Section className="justify-content-center">
          <Col sm="auto">
            <Alert variant="warning">
              You must select a Plan before being able manage your
              Organization&apos;s settings.
            </Alert>
          </Col>
        </Section>
      )}
      <LoadIndicatingSection
        queryResult={customerPortalLinkQueryResult}
        errorHandler={handleForbiddenError}
      >
        {(axiosResponse) => (
          <>
            <SectionTitle title="Billing settings" icon={User} />
            <Col sm={12}>
              <a href={axiosResponse.data} target="_blank" rel="noreferrer">Manage payments</a>
            </Col>
          </>
        )}
      </LoadIndicatingSection>
      <LoadIndicatingSection queryResult={usageQueryResult}>
        {(axiosResponse) => (
          <>
            <SectionTitle
              title={(
                <>
                  Usage this month
                  <DocsLink articlePath="/usage" title="Usage" size="sm" />
                </>
              )}
              icon={Activity}
            />
            <Col sm={12}>
              <UsageDashboard usageDetails={axiosResponse.data} />
            </Col>
          </>
        )}
      </LoadIndicatingSection>
      <LoadIndicatingSection
        queryResult={usersQueryResult}
        errorHandler={handleForbiddenError}
        last
      >
        {(axiosResponse) => (
          <>
            <SectionTitle
              title={(
                <>
                  User management
                  <DocsLink articlePath="/accounts#inviting-users" title="Inviting users" size="sm" />
                </>
              )}
              icon={User}
            />
            <Col sm={12}>
              <h2 className="h5">Invite user</h2>
              <InviteUserFormCard />
              <UserTable users={axiosResponse.data} />
            </Col>
          </>
        )}
      </LoadIndicatingSection>
    </Page>
  );
}
