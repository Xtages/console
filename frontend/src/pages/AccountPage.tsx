import React from 'react';
import {useQuery} from 'react-query';
import {checkoutApi, usageApi} from 'service/Services';
import {Activity, User} from 'react-feather';
import {Col} from 'react-bootstrap';
import Page from '../components/layout/Page';
import {LoadIndicatingSection, SectionTitle} from '../components/layout/Section';
import {UsageDashboard} from '../components/usage/UsageDashboard';

/**
 * A simple account page where the user can then click to go to their Striper customer portal.
 */
export default function AccountPage() {
  const customerPortalLinkQueryResult = useQuery(
    'customerPortalLink',
    () => checkoutApi.getCustomerPortalSession(),
  );
  const usageQueryResult = useQuery(
    'usage',
    () => usageApi.getAllUsageDetails(),
  );

  return (
    <Page>
      <LoadIndicatingSection queryResult={customerPortalLinkQueryResult}>
        {(axiosResponse) => (
          <>
            <SectionTitle title="Billing settings" icon={User} />
            <Col sm={12}>
              <a href={axiosResponse.data} target="_blank" rel="noreferrer">Manage payments</a>
            </Col>
          </>
        )}
      </LoadIndicatingSection>
      <LoadIndicatingSection queryResult={usageQueryResult} last>
        {(axiosResponse) => (
          <>
            <SectionTitle title="Usage this month" icon={Activity} />
            <Col sm={12}>
              <UsageDashboard usageDetails={axiosResponse.data} />
            </Col>
          </>
        )}
      </LoadIndicatingSection>
    </Page>
  );
}
