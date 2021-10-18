import React from 'react';
import {Alert, Button, Col, Container, Row} from 'react-bootstrap';
import {useAuth} from 'hooks/useAuth';
import {isOrgInGoodStanding} from 'helpers/organization';
import {CenteredOnScreen} from 'components/layout/CenteredOnScreen';
import {Redirect} from 'react-router-dom';
import Logos from 'components/Logos';

/**
 * Rendered when the user is either: a) not part of an organization (meaning that the checkout flow
 * was left hanging) or b) the organization they belong to is not in good standing.
 */
export default function InvalidOrgPage() {
  const {organization, logOut} = useAuth();

  async function signOut() {
    await logOut();
  }

  if (organization !== null && !isOrgInGoodStanding(organization)) {
    return (
      <CenteredOnScreen>
        <Container>
          <Row>
            <Col sm="auto" className="pb-4">
              <Logos size="sm" />
            </Col>
          </Row>
        </Container>
        <Alert variant="danger" className="text-lg">
          Organization
          {' '}
          {organization.name}
          {' '}
          is no longer active.
        </Alert>
        <Container>
          <Row>
            <Col>
              <p className="prose">
                If you think this is an error, send us an email at
                {' '}
                {/* eslint-disable-next-line react/jsx-no-target-blank */}
                <a href="mailto:support@xtages.com" target="_blank">support@xtages.com</a>
                .
              </p>
            </Col>
            <Col sm="auto">
              <Button variant="outline-primary" onClick={signOut}>Sign out</Button>
            </Col>
          </Row>
        </Container>
      </CenteredOnScreen>
    );
  }
  return <Redirect to="/" />;
}
