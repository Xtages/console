import {useQuery} from 'react-query';
import React from 'react';
import {useParams} from 'react-router-dom';
import {Col, Container, Row} from 'react-bootstrap';
import {projectApi} from '../service/Services';
import {LoadIndicatingSection} from '../components/layout/Section';
import {BuildTable} from '../components/build/BuildTable';
import Page from '../components/layout/Page';
import ProjectDetailsCard from '../components/project/ProjectDetailsCard';
import UsageChecker from '../components/usage/UsageChecker';

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
          {((axiosResponse) => (
            <>
              <Container>
                <Row>
                  <Col sm={12} className="p-0">
                    <ProjectDetailsCard project={axiosResponse.data} />
                  </Col>
                </Row>
              </Container>
              <BuildTable project={axiosResponse.data} />
            </>
          ))}
        </LoadIndicatingSection>
      </Page>
    </>
  );
}
