import React from 'react';
import {Meta, Story} from '@storybook/react';
import {MemoryRouter} from 'react-router-dom';
import ProjectDetailsCard, {ProjectDetailsCardProps} from './ProjectDetailsCard';
import {ProjectTypeEnum} from '../../gen/api';

export default {
  title: 'Xtages/Project/ProjectDetailsCard',
  component: ProjectDetailsCard,
} as Meta;

const projectData = {
  id: 10,
  name: 'console',
  ghRepoUrl: 'https://github.com/Xtages/console',
  organization: 'Xtages',
  type: ProjectTypeEnum.Node,
  version: '15',
  passCheckRuleEnabled: false,
  builds: [],
};

const Template: Story<ProjectDetailsCardProps> = (args) => <MemoryRouter><ProjectDetailsCard {...args} /></MemoryRouter>;

export const Primary = Template.bind({});
Primary.args = {
  project: projectData,
};
Primary.storyName = 'ProjectDetailsCard';
