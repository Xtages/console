import {useQuery} from 'react-query';
import React, {ReactNode} from 'react';
import {useParams} from 'react-router-dom';
import {Col, Container, Row} from 'react-bootstrap';
import {projectApi} from '../service/Services';
import {Section} from '../components/layout/Section';
import {BuildTable} from '../components/build/BuildTable';
import Page from '../components/layout/Page';
import ProjectDetailsCard from '../components/project/ProjectDetailsCard';

export default function ProjectPage() {
  const {name} = useParams<{name: string}>();
  const {
    isLoading,
    error,
    data,
  } = useQuery(
    'project',
    () => projectApi.getProject(name, true, true, true),
  );
  let contents: string | ReactNode;
  if (isLoading) {
    contents = 'Loading...';
  } else if (error) {
    contents = `An error has occurred: ${error}`;
  } else if (data?.data != null) {
    contents = (
      <>
        <Container>
          <Row>
            <Col sm={12} className="p-0">
              <ProjectDetailsCard project={data!.data} />
            </Col>
          </Row>
        </Container>
        <BuildTable project={data.data} />
      </>
    );
  }
  return (
    <Page>
      <Section last>
        {contents}
      </Section>
    </Page>
  );
}
