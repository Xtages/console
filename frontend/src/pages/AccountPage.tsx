import React, {ReactNode} from 'react';
import {useQuery} from 'react-query';
import {checkoutApi} from 'service/Services';
import {User} from 'react-feather';
import Page from '../components/layout/Page';
import {Section, SectionTitle} from '../components/layout/Section';

/**
 * A simple account page where the user can then click to go to their Striper customer portal.
 */
export default function AccountPage() {
  const {
    isLoading,
    error,
    data,
  } = useQuery(
    'customerPortalLink',
    () => checkoutApi.getCustomerPortalSession(),
  );

  let content: string | ReactNode;
  if (isLoading) {
    content = 'Loading...';
  } else if (error) {
    content = `An error has occurred: ${error}`;
  } else {
    content = <a href={data?.data} target="_blank" rel="noreferrer">Manage my payments</a>;
  }

  return (
    <Page>
      <Section last>
        <SectionTitle title="Account settings" icon={User} />
        <div className="col-12">
          {content}
        </div>
      </Section>
    </Page>
  );
}
