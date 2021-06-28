import {useQueries} from 'react-query';
import React, {ReactNode} from 'react';
import {useParams} from 'react-router-dom';
import {Col, Container, Row, Spinner} from 'react-bootstrap';
import {AxiosResponse} from 'axios';
import {projectApi} from '../service/Services';
import {Section} from '../components/layout/Section';
import Page from '../components/layout/Page';
import UsageChecker from '../components/usage/UsageChecker';
import {ProjectSettingsCard} from '../components/project/ProjectSettingsCard';
import {Project, ProjectSettings} from '../gen/api';

export default function ProjectSettingsPage() {
  const {name} = useParams<{name: string}>();

  // TODO(czuniga): Figure out how to get <LoadIndicatingSection> to handle multiple queries.
  const queryResult = useQueries([
    {
      queryKey: 'project',
      queryFn: () => projectApi.getProject(name, false, false, false),
    },
    {
      queryKey: 'projectSettings',
      queryFn: () => projectApi.getProjectSettings(name),
    },
  ]);
  const {
    isLoading: isLoadingProject,
    error: projectError,
    data: projectData,
  } = queryResult[0];
  const {
    isLoading: isLoadingProjectSettings,
    error: projectSettingsError,
    data: settingsData,
  } = queryResult[1];
  let content: string | ReactNode;
  if (isLoadingProject || isLoadingProjectSettings) {
    content = (
      <div className="mx-auto py-5">
        <Spinner animation="grow" role="status" variant="dark-secondary">
          <span className="sr-only">Loading...</span>
        </Spinner>
      </div>
    );
  } else if (projectError || projectSettingsError) {
    content = 'An error has occurred';
  } else {
    content = (
      <ProjectSettingsCard
        project={(projectData as AxiosResponse<Project>).data}
        projectSettings={(settingsData as AxiosResponse<ProjectSettings>).data}
      />
    );
  }
  return (
    <>
      <UsageChecker />
      <Page>
        <Section last>
          <Container>
            <Row>
              <Col sm={12} className="p-0">
                {content}
              </Col>
            </Row>
          </Container>
        </Section>
      </Page>
    </>
  );
}
