import {useQuery} from 'react-query';
import React, {ReactNode} from 'react';
import {Codesandbox} from 'react-feather';
import {useParams} from 'react-router-dom';
import {projectApi} from '../service/Services';
import {Section, SectionTitle} from '../components/layout/Section';
import {BuildTable} from '../components/build/BuildTable';
import Page from '../components/layout/Page';

export default function ProjectPage() {
  const {name} = useParams<{name: string}>();
  const {
    isLoading,
    error,
    data,
  } = useQuery(
    'project',
    () => projectApi.getProject(name, true),
  );
  let buildsTable: string | ReactNode;
  if (isLoading) {
    buildsTable = 'Loading...';
  } else if (error) {
    buildsTable = `An error has occurred: ${error}`;
  } else if (data?.data != null) {
    buildsTable = (
      <BuildTable project={data.data} />
    );
  }
  return (
    <Page>
      <Section last>
        <SectionTitle icon={Codesandbox} title={name} />
        <div className="col-12">
          {buildsTable}
        </div>
      </Section>
    </Page>
  );
}
