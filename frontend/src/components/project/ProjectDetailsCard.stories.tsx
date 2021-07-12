import React from 'react';
import {Meta, Story} from '@storybook/react';
import {Project, ProjectTypeEnum} from 'gen/api';
import {DeploymentDetails,
  DeploymentDetailsAndBuildChart,
  DeploymentDetailsProps,
  SimpleProjectCard,
  SimpleProjectCardProps} from './ProjectDetailsCard';

export default {
  title: 'Xtages/Project/SimpleProjectCard',
  component: SimpleProjectCard,
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
  percentageOfSuccessfulBuildsInTheLastMonth: 0.1,
};

const TemplateWithDeploymentsAndBuildChart: Story<SimpleProjectCardProps> = (args) => (
  <SimpleProjectCard {...args}>
    <DeploymentDetailsAndBuildChart {...args} />
  </SimpleProjectCard>
);

export const Primary = TemplateWithDeploymentsAndBuildChart.bind({});
Primary.args = {
  project: projectData,
};
Primary.storyName = 'SimpleProjectCardWithDeploymentsAndBuildChart';

const TemplateWithDeployments: Story<SimpleProjectCardProps & DeploymentDetailsProps> = (args) => (
  <SimpleProjectCard {...args}>
    <DeploymentDetails {...args} />
  </SimpleProjectCard>
);

export const WithDeployments = TemplateWithDeployments.bind({});
WithDeployments.args = {
  project: projectData,
  colWidth: 9,
};
WithDeployments.storyName = 'SimpleProjectCardWithDeployments';
