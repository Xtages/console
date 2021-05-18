import React from 'react';
import {Meta, Story} from '@storybook/react';
import ProjectTemplateCard, {ProjectTemplateCardProps} from './ProjectTemplateCard';

export default {
  title: 'Xtages/Project/ProjectTemplateCard',
  component: ProjectTemplateCard,
} as Meta;

const Template: Story<ProjectTemplateCardProps> = (args) => <ProjectTemplateCard {...args} />;

export const Primary = Template.bind({});
Primary.args = {
  title: 'Simple Node.js server',
  description: 'A simple Node.js server template, using Express.js as well as Jest for running tests',
  imageName: 'nodejs.svg',
};
Primary.storyName = 'ProjectTemplateCard';
