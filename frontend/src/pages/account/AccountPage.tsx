import React from 'react';
import {useQuery} from 'react-query';
import {checkoutApi} from 'service/Services';

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
    () => checkoutApi.createCustomerPortalSession(),
  );

  let content: string | JSX.Element;
  if (isLoading) {
    content = 'Loading...';
  } else if (error) {
    content = `An error has occurred: ${error}`;
  } else {
    content = <a href={data?.data} target="_blank" rel="noreferrer">Manage my payments</a>;
  }

  return (
    <section>
      <div className="container d-flex flex-column">
        <div className="row align-items-center justify-content-center min-vh-100">
          <div className="col-md-6 col-lg-5 col-xl-4">
            <div className="mb-5 text-center">
              <h1 className="h3 mb-1">Account settings</h1>
            </div>
            <span className="clearfix" />
            {content}
          </div>
        </div>
      </div>
    </section>
  );
}
