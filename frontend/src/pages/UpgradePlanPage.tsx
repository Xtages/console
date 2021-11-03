import React from 'react';
import {PlanSelector} from 'components/plan/PlanSelector';
import Page from 'components/layout/Page';
import {Section} from 'components/layout/Section';
import {Col, Container, Row} from 'react-bootstrap';
import styles from './UpgradePlanPage.module.scss';

export default function UpgradePlanPage() {
  return (
    <Page>
      <Section>
        <Container>
          <Row className="mb-4 justify-content-center text-center">
            <Col>
              <h1 className="mt-4">Choose your plan</h1>
            </Col>
          </Row>
        </Container>
        <PlanSelector showFreePlan={false} />
      </Section>
      <Section last>
        <Container className="container">
          <Row className="mb-2 justify-content-center text-center">
            <Col>
              <h1 className="mt-4">Deployment specs</h1>
            </Col>
          </Row>
          <Row className="justify-content-center">
            <Col sm={8} className="mx-auto">
              <table className={`table table-bordered table-responsive-sm mx-auto text-center ${styles.specs}`}>
                <thead>
                  <tr>
                    <th scope="col">{' '}</th>
                    <th scope="col">Starter</th>
                    <th scope="col">Professional</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <th scope="row" className="text-left">No. of apps</th>
                    <td>1</td>
                    <td>3</td>
                  </tr>
                  <tr>
                    <th scope="row" className="text-left">No. of environments per app</th>
                    <td>
                      2
                      <div>(staging and production)</div>
                    </td>
                    <td>
                      2
                      <div>(staging and production)</div>
                    </td>
                  </tr>
                  <tr>
                    <th scope="row" className="text-left">Per app specs</th>
                    <td>
                      2 vCPU
                      <br />
                      {' '}
                      4GB RAM
                    </td>
                    <td>
                      2 vCPU
                      <br />
                      {' '}
                      4GB RAM
                    </td>
                  </tr>
                  <tr>
                    <th scope="row" className="text-left">No. of DBs</th>
                    <td>1</td>
                    <td>1</td>
                  </tr>
                  <tr>
                    <th scope="row" className="text-left">DB specs</th>
                    <td>
                      2 vCPU
                      <br />
                      {' '}
                      8GB RAM
                      <br />
                      {' '}
                      20GB of storage
                    </td>
                    <td>
                      2 vCPU
                      <br />
                      {' '}
                      8GB RAM
                      <br />
                      {' '}
                      20GB of storage
                    </td>
                  </tr>
                  <tr>
                    <th scope="row" className="text-left">Log collection</th>
                    <td>yes</td>
                    <td>yes</td>
                  </tr>
                  <tr>
                    <th scope="row" className="text-left">Metrics dashboard</th>
                    <td><span className="text-sm">(coming soon)</span></td>
                    <td><span className="text-sm">(coming soon)</span></td>
                  </tr>
                </tbody>
              </table>
            </Col>
          </Row>
        </Container>
      </Section>
    </Page>
  );
}
