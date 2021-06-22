import React from 'react';
import {Meta, Story} from '@storybook/react';
import {UsageDashboard, UsageDashboardProps} from './UsageDashboard';
import {ResourceType, UsageDetailStatusEnum} from '../../gen/api';

export default {
  title: 'Xtages/UsageDashboard',
  component: UsageDashboard,
} as Meta;

const Template: Story<UsageDashboardProps> = (args) => <UsageDashboard {...args} />;

export const Primary = Template.bind({});
Primary.args = {
  usageDetails: [
    {
      resourceType: ResourceType.Project,
      status: UsageDetailStatusEnum.OverLimit,
      limit: 2,
      usage: 2,
      resetTimestampInMillis: undefined,
    },
    {
      resourceType: ResourceType.MonthlyBuildMinutes,
      status: UsageDetailStatusEnum.UnderLimit,
      limit: 2500,
      usage: 1350,
      resetTimestampInMillis: Date.now() + (5 * 24 * 60 * 60 * 1000),
    },
    {
      resourceType: ResourceType.MonthlyDataTransferGbs,
      status: UsageDetailStatusEnum.UnderLimit,
      limit: 25,
      usage: 17,
      resetTimestampInMillis: Date.now() + (5 * 24 * 60 * 60 * 1000),
    },
  ],
};
Primary.storyName = 'UsageDashboard';
