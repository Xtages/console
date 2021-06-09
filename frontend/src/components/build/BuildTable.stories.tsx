import React from 'react';
import {Meta, Story} from '@storybook/react';
import {BrowserRouter} from 'react-router-dom';
import {BuildTable, BuildTableProps} from './BuildTable';
import {BuildStatusEnum, BuildType, ProjectTypeEnum} from '../../gen/api';

export default {
  title: 'Xtages/Build/BuildTable',
  component: BuildTable,
} as Meta;

// eslint-disable-next-line max-len
const Template: Story<BuildTableProps> = (args) => <BrowserRouter><BuildTable {...args} /></BrowserRouter>;

const buildData = {
  id: 100,
  status: BuildStatusEnum.Failed,
  type: BuildType.Ci,
  initiatorName: 'Bill Murray',
  initiatorEmail: 'b.murray@xtages.com',
  initiatorAvatarUrl: 'http://www.fillmurray.com/100/100',
  commitHash: '81acee7df324793c6409e178798dab5d197ba50f',
  commitUrl: 'https://github.com/Xtages/console/commit/81acee7df324793c6409e178798dab5d197ba50f',
  startTimestampInMillis: Date.now(),
  endTimestampInMillis: Date.now() + (7 * 60 * 1000),
  phases: [
    {
      id: 1,
      name: 'PROVISIONING',
      status: 'SUCCEEDED',
      message: 'A very long message A very long messageA very long messageA very long messageA very long messageA very long messageA very long messageA very long messageA very long message',
      startTimestampInMillis: Date.now(),
      endTimestampInMillis: Date.now() + (2 * 60 * 1000),
    },
    {
      id: 2,
      name: 'BUILD',
      status: 'FAILED',
      message: 'A very long message A very long messageA very long messageA very long messageA very long messageA very long messageA very long messageA very long messageA very long message',
      startTimestampInMillis: Date.now() + (2 * 60 * 1000),
      endTimestampInMillis: Date.now() + (3 * 60 * 1000),
    },
  ],
};

const prodDeploymentData = {
  id: 100,
  env: 'production',
  initiatorName: 'Bill Murray',
  initiatorEmail: 'b.murray@xtages.com',
  initiatorAvatarUrl: 'http://www.fillmurray.com/100/100',
  commitHash: '81acee7df324793c6409e178798dab5d197ba50f',
  commitUrl: 'https://github.com/Xtages/console/commit/81acee7df324793c6409e178798dab5d197ba50f',
  timestampInMillis: Date.now(),
  serviceUrl: 'https://FIXME',
};

const stagingDeploymentData = {
  id: 100,
  env: 'staging',
  initiatorName: 'Bill Murray',
  initiatorEmail: 'b.murray@xtages.com',
  initiatorAvatarUrl: 'http://www.fillmurray.com/100/100',
  commitHash: '81acee7df324793c6409e178798dab5d197ba50f',
  commitUrl: 'https://github.com/Xtages/console/commit/81acee7df324793c6409e178798dab5d197ba50f',
  timestampInMillis: Date.now() - (24 * 60 * 60 * 1000),
  serviceUrl: 'https://FIXME',
};

const projectData = {
  id: 10,
  name: 'console',
  ghRepoUrl: 'https://github.com/Xtages/console',
  organization: 'Xtages',
  type: ProjectTypeEnum.Node,
  version: '15',
  passCheckRuleEnabled: false,
  builds: [buildData],
  deployments: [prodDeploymentData, stagingDeploymentData],
};

export const Primary = Template.bind({});
Primary.args = {
  project: projectData,
};
Primary.storyName = 'BuildTable';
