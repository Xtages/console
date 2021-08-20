import React from 'react';
import {Meta, Story} from '@storybook/react';
import {DeploymentStatusEnum, Project, ProjectTypeEnum} from 'gen/api';
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
  deployments: [
    {
      id: 1,
      initiatorEmail: 'rick.james@gmail.com',
      initiatorName: 'Rick James',
      initiatorAvatarUrl: 'https://im.rick.james.com',
      commitHash: 'abc123',
      commitUrl: 'https://github.com/Xtages/console/abc123',
      env: 'production',
      timestampInMillis: Date.now(),
      serviceUrls: [
        'https://somedomain.com',
        'https://123456-production.xtages.dev',
      ],
      status: DeploymentStatusEnum.Running,
    },
    {
      id: 2,
      initiatorEmail: 'charlie.murphy@gmail.com',
      initiatorName: 'Charlie Murphy',
      initiatorAvatarUrl: 'https://cmurphy.com',
      commitHash: 'uio678',
      commitUrl: 'https://github.com/Xtages/console/uio678',
      env: 'staging',
      timestampInMillis: Date.now(),
      serviceUrls: [
        'https://098765-staging.xtages.dev',
      ],
      status: DeploymentStatusEnum.Stopped,
    },
  ],
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
