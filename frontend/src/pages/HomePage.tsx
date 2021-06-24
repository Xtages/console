import React from 'react';
import {useQuery} from 'react-query';
import {projectApi} from 'service/Services';
import {Codesandbox, Copy} from 'react-feather';
import {ProjectTable} from 'components/project/ProjectTable';
import ProjectTemplateCard from 'components/project/ProjectTemplateCard';
import {LoadIndicatingSection, Section, SectionTitle} from 'components/layout/Section';
import Page from 'components/layout/Page';
import {useHistory} from 'react-router-dom';
import UsageChecker from '../components/usage/UsageChecker';

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
        <Section>
          <SectionTitle icon={Copy} title="Project templates" subtitle="Create new projects" />
          <div className="d-block col-md-3 col-sm-6">
            <ProjectTemplateCard
              id="nodejs"
              title="Simple Node.js server"
              description="A simple Node.js server template, using Express.js as well as Jest for running tests"
              imageName="nodejs.svg"
              onClick={() => history.push('/new/project')}
            />
          </div>
        </Section>
        <LoadIndicatingSection queryResult={getProjectsQueryResult} last>
          {(axiosResponse) => (
            <>
              <SectionTitle icon={Codesandbox} title="Projects" subtitle="Manage all your projects" />
              <div className="col-12">
                <ProjectTable projects={axiosResponse.data} />
              </div>
            </>
          )}
        </LoadIndicatingSection>
      </Page>
    </>
  );
}
