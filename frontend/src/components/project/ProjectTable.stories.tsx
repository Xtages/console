import React from 'react';
import {Meta, Story} from '@storybook/react';
import {BrowserRouter} from 'react-router-dom';
import {Build, BuildStatusEnum, BuildType, ProjectTypeEnum} from 'gen/api';
import {ProjectRowProps, ProjectTable, ProjectTableProps} from './ProjectTable';

export default {
  title: 'Xtages/Project/ProjectTable',
  component: ProjectTable,
} as Meta;

const projectData = {
  id: 10,
  name: 'console',
  ghRepoUrl: 'https://github.com/Xtages/console',
  organization: 'Xtages',
  type: ProjectTypeEnum.Node,
  version: '15',
  passCheckRuleEnabled: false,
};

const buildData: Build = {
  id: 100,
  buildNumber: 1,
  status: BuildStatusEnum.Failed,
  type: BuildType.Ci,
  env: 'dev',
  initiatorName: 'Bill Murray',
  initiatorEmail: 'b.murray@xtages.com',
  initiatorAvatarUrl: 'http://www.fillmurray.com/100/100',
  commitHash: '81acee7df324793c6409e178798dab5d197ba50f',
  commitUrl: 'https://github.com/Xtages/console/commit/81acee7df324793c6409e178798dab5d197ba50f',
  startTimestampInMillis: Date.now(),
  endTimestampInMillis: Date.now() + (7 * 60 * 1000),
  actions: [],
  phases: [],
};

// eslint-disable-next-line max-len
const ProjectTableTemplate: Story<ProjectTableProps> = (args) => <BrowserRouter><ProjectTable {...args} /></BrowserRouter>;

export const Primary = ProjectTableTemplate.bind({});
Primary.args = {
  projects: [{
    ...projectData,
    builds: [buildData],
    deployments: [],
  }],
};
Primary.storyName = 'ProjectTable';

export const ProjectTableNoBuild = ProjectTableTemplate.bind({});
ProjectTableNoBuild.args = {
  projects: [{
    ...projectData,
    builds: [],
    deployments: [],
  }],
};
ProjectTableNoBuild.storyName = 'ProjectTable without build';

const ProjectRowTemplate: Story<ProjectRowProps> = ({project}: ProjectRowProps) => (
  <BrowserRouter>
    <ProjectTable
      projects={[project]}
    />
  </BrowserRouter>
);

export const ProjectRowStory = ProjectRowTemplate.bind({});
ProjectRowStory.args = {
  project: {
    ...projectData,
    builds: [buildData],
    deployments: [],
  },
};
ProjectRowStory.storyName = 'ProjectRow';
