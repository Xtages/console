import React from 'react';
import {Meta, Story} from '@storybook/react';
import {ProjectDetailsCard, SimpleProjectCardProps} from './ProjectDetailsCard';
import {Project, ProjectTypeEnum} from '../../gen/api';

export default {
  title: 'Xtages/Project/ProjectDetailsCard',
  component: ProjectDetailsCard,
} as Meta;

const projectData: Project = {
  id: 10,
  name: 'console',
  ghRepoUrl: 'https://github.com/Xtages/console',
  organization: 'Xtages',
  type: ProjectTypeEnum.Node,
  version: '15',
  passCheckRuleEnabled: false,
  builds: [],
  deployments: [],
};

const Template: Story<SimpleProjectCardProps> = (args) => <ProjectDetailsCard {...args} />;

export const Primary = Template.bind({});
Primary.args = {
  project: projectData,
};
Primary.storyName = 'ProjectDetailsCard';
