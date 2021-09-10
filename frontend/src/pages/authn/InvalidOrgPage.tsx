import React from 'react';
import {Alert, Button} from 'react-bootstrap';
import {useAuth} from 'hooks/useAuth';
import {isOrgInGoodStanding} from 'helpers/organization';
import {FullScreenErrorPage} from 'components/layout/FullScreenErrorPage';
import {Redirect} from 'react-router-dom';

/**
 * Rendered when the user is either: a) not part of an organization (meaning that the checkout flow
 * was left hanging) or b) the organization they belong to is not in good standing.
 */
export default function InvalidOrgPage() {
  const {organization, principal, logOut} = useAuth();

  async function signOut() {
    await logOut();
  }

  if (organization == null) {
    return (
      <FullScreenErrorPage>
        <Alert variant="danger" className="text-lg">
          User
          {' '}
          <strong className="font-weight-bolder text-xl">
            {principal?.name}
            {' <'}
            {principal?.email}
            {'> '}
          </strong>
          is not currently associated to an Xtages Organization.
        </Alert>
        <div className="d-flex justify-content-center">
          <Button variant="outline-primary" onClick={signOut}>Sign out</Button>
        </div>
      </FullScreenErrorPage>
    );
  }
  if (!isOrgInGoodStanding(organization)) {
    return (
      <FullScreenErrorPage>
        <Alert variant="danger" className="text-lg">
          Organization
          {' '}
          {organization.name}
          {' '}
          is no longer active.
        </Alert>
        <div className="d-flex justify-content-center">
          <Button variant="outline-primary" onClick={signOut}>Sign out</Button>
        </div>
      </FullScreenErrorPage>
    );
  }
  return <Redirect to="/" />;
}
