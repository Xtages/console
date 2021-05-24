import React from 'react';
import {Meta, Story} from '@storybook/react';
import {BrowserRouter} from 'react-router-dom';
import {ProjectRowProps, ProjectTable, ProjectTableProps} from './ProjectTable';
import {BuildStatusEnum, BuildTypeEnum, ProjectTypeEnum} from '../../gen/api';

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

const buildData = {
  id: 100,
  status: BuildStatusEnum.Failed,
  type: BuildTypeEnum.Ci,
  initiatorName: 'Bill Murray',
  initiatorEmail: 'b.murray@xtages.com',
  initiatorAvatarUrl: 'http://www.fillmurray.com/100/100',
  commitHash: '81acee7df324793c6409e178798dab5d197ba50f',
  commitUrl: 'https://github.com/Xtages/console/commit/81acee7df324793c6409e178798dab5d197ba50f',
  startTimestampInMillis: Date.now(),
  endTimestampInMillis: Date.now() + (7 * 60 * 1000),
  phases: [],
};

// eslint-disable-next-line max-len
const ProjectTableTemplate: Story<ProjectTableProps> = (args) => <BrowserRouter><ProjectTable {...args} /></BrowserRouter>;

export const Primary = ProjectTableTemplate.bind({});
Primary.args = {
  projectsAndBuilds: [{
    project: projectData,
    lastBuild: buildData,
  }],
};
Primary.storyName = 'ProjectTable';

export const ProjectTableNoBuild = ProjectTableTemplate.bind({});
ProjectTableNoBuild.args = {
  projectsAndBuilds: [{
    project: projectData,
  }],
};
ProjectTableNoBuild.storyName = 'ProjectTable without build';

const ProjectRowTemplate: Story<ProjectRowProps> = ({project, build}: ProjectRowProps) => (
  <BrowserRouter>
    <ProjectTable
      projectsAndBuilds={[{
        project,
        lastBuild: build,
      }]}
    />
  </BrowserRouter>
);

export const ProjectRowStory = ProjectRowTemplate.bind({});
ProjectRowStory.args = {
  project: projectData,
  build: buildData,
};
ProjectRowStory.storyName = 'ProjectRow';
