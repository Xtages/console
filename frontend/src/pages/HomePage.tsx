import React from 'react';
import {useQuery} from 'react-query';
import {projectApi} from 'service/Services';
import {Codesandbox, Copy} from 'react-feather';
import {ProjectTable} from 'components/project/ProjectTable';
import ProjectTemplateCard from 'components/project/ProjectTemplateCard';
import {LoadIndicatingSection, Section, SectionTitle} from 'components/layout/Section';
import Page from 'components/layout/Page';
import {useHistory} from 'react-router-dom';
import UsageChecker from 'components/usage/UsageChecker';
import {GitHubIntegrationSection} from 'components/github/GitHubIntegrationSection';
import {Col} from 'react-bootstrap';
import {DocsLink} from 'components/link/XtagesLink';

export default function HomePage() {
  const getProjectsQueryResult = useQuery(
    'projects',
    () => projectApi.getProjects(true),
  );
  const history = useHistory();
  return (
    <>
      <UsageChecker />
      <Page>
        <GitHubIntegrationSection />
        <Section>
          <SectionTitle
            icon={Copy}
            title={(
              <>
                Project templates
                <DocsLink articlePath="/projects" title="Creating a project" size="sm" />
              </>
            )}
            subtitle="Create new projects"
          />
          <Col sm={3} className="d-block">
            <ProjectTemplateCard
              id="nodejs"
              title="Simple Node.js server"
              description="A simple Node.js server template, using Express.js as well as Jest for running tests"
              imageName="nodejs.svg"
              onClick={() => history.push('/new/project')}
            />
          </Col>
        </Section>
        <LoadIndicatingSection queryResult={getProjectsQueryResult} last>
          {(axiosResponse) => (
            <>
              <SectionTitle
                icon={Codesandbox}
                title="Projects"
                subtitle="Manage all your projects"
              />
              <div className="col-12">
                {axiosResponse.data.length > 0 ? <ProjectTable projects={axiosResponse.data} />
                  : (
                    <div className="text-center">
                      <h3 className="h5">Looks like don&apos;t have any Projects yet!</h3>
                      <p>
                        For documentation on how to get started take a look at our
                        {' '}
                        <DocsLink articlePath="/" title="Getting started" icon={false}>user docs</DocsLink>
                        .
                      </p>
                    </div>
                  )}
              </div>
            </>
          )}
        </LoadIndicatingSection>
      </Page>
    </>
  );
}
