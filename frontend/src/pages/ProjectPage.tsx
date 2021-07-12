import {useQuery} from 'react-query';
import React from 'react';
import {useParams} from 'react-router-dom';
import {Col, Container, Row} from 'react-bootstrap';
import {projectApi} from 'service/Services';
import {LoadIndicatingSection} from 'components/layout/Section';
import {BuildTable} from 'components/build/BuildTable';
import {DeploymentDetailsAndBuildChart, SimpleProjectCard} from 'components/project/ProjectDetailsCard';
import Page from 'components/layout/Page';
import UsageChecker from 'components/usage/UsageChecker';

export default function ProjectPage() {
  const {name} = useParams<{name: string}>();
  const getProjectQueryResult = useQuery(
    'project',
    () => projectApi.getProject(name, true, true, true),
  );
  return (
    <>
      <UsageChecker />
      <Page>
        <LoadIndicatingSection queryResult={getProjectQueryResult} last>
          {(function render(axiosResponse) {
            const project = axiosResponse.data;
            return (
              <>
                <Container>
                  <Row>
                    <Col sm={12} className="p-0">
                      <SimpleProjectCard project={project}>
                        <DeploymentDetailsAndBuildChart project={project} />
                      </SimpleProjectCard>
                    </Col>
                  </Row>
                </Container>
                <BuildTable project={project} />
              </>
            );
          })}
        </LoadIndicatingSection>
      </Page>
    </>
  );
}
