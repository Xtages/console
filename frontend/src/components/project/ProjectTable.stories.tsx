import React from 'react';
import {Meta, Story} from '@storybook/react';
import {BrowserRouter} from 'react-router-dom';
import {ProjectRow, ProjectRowProps, ProjectTable, ProjectTableProps} from './ProjectTable';

export default {
  title: 'Xtages/Project/ProjectTable',
  component: ProjectTable,
} as Meta;

// eslint-disable-next-line max-len
const ProjectTableTemplate: Story<ProjectTableProps> = (args) => <BrowserRouter><ProjectTable {...args} /></BrowserRouter>;

export const Primary = ProjectTableTemplate.bind({});
Primary.args = {
  children: [
    <ProjectRow
      id={10}
      name="Xtages/console"
      repoUrl="https://github.com/Xtages/console"
      buildId={100}
      buildStatus="failed"
      buildInitiator="Bill Murray"
      buildInitiatorEmail="b.murray@xtages.com"
      buildInitiatorAvatarUrl="http://www.fillmurray.com/100/100"
      buildCommitHash="81acee7df324793c6409e178798dab5d197ba50f"
      buildCommitUrl="https={//github.com/Xtages/console/commit/81acee7df324793c6409e178798dab5d197ba50f"
      buildStartTimestamp={Date.now()}
      buildEndTimestamp={Date.now() + (7 * 60 * 1000)}
    />,
  ],
};
Primary.storyName = 'ProjectTable';

// eslint-disable-next-line max-len
const ProjectRowTemplate: Story<ProjectRowProps> = (args) => <BrowserRouter><ProjectTable><ProjectRow {...args} /></ProjectTable></BrowserRouter>;

export const ProjectRowStory = ProjectRowTemplate.bind({});
ProjectRowStory.args = {
  id: 10,
  name: 'Xtages/console',
  repoUrl: 'https://github.com/Xtages/console',
  buildId: 100,
  buildStatus: 'failed',
  buildInitiator: 'Bill Murray',
  buildInitiatorEmail: 'b.murray@xtages.com',
  buildInitiatorAvatarUrl: 'http://www.fillmurray.com/100/100',
  buildCommitHash: '81acee7df324793c6409e178798dab5d197ba50f',
  buildCommitUrl: 'https={//github.com/Xtages/console/commit/81acee7df324793c6409e178798dab5d197ba50f',
  buildStartTimestamp: Date.now(),
  buildEndTimestamp: Date.now() + (7 * 60 * 1000),
};
ProjectRowStory.storyName = 'ProjectRow';
