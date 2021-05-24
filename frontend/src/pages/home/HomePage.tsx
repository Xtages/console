import React, {ReactNode} from 'react';
import {useQuery} from 'react-query';
import {projectApi} from 'service/Services';
import {Codesandbox, Copy} from 'react-feather';
import {ProjectTable} from 'components/project/ProjectTable';
import ProjectTemplateCard from 'components/project/ProjectTemplateCard';
import {Section, SectionTitle} from 'components/layout/Section';
import Page from 'components/layout/Page';

export default function HomePage() {
  const {
    isLoading,
    error,
    data,
  } = useQuery(
    'projects',
    () => projectApi.getProjects(true),
  );
  let projectTable: string | ReactNode;
  if (isLoading) {
    projectTable = 'Loading...';
  } else if (error) {
    projectTable = `An error has occurred: ${error}`;
  } else if (data?.data != null) {
    projectTable = (
      <ProjectTable projects={data.data} />
    );
  }
  return (
    <Page>
      <Section>
        <SectionTitle icon={Copy} title="Project templates" subtitle="Create new projects" />
        <div className="d-block col-md-3 col-sm-6">
          <ProjectTemplateCard
            id="nodejs"
            title="Simple Node.js server"
            description="A simple Node.js server template, using Express.js as well as Jest for running tests"
            imageName="nodejs.svg"
          />
        </div>
      </Section>
      <Section last>
        <SectionTitle icon={Codesandbox} title="Projects" subtitle="Manage all your projects" />
        <div className="col-12">
          {projectTable}
        </div>
      </Section>
    </Page>
  );
}
