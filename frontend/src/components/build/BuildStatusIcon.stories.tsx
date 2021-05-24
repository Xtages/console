import React from 'react';
import {Meta, Story} from '@storybook/react';
import {BuildStatusIcon, BuildStatusIconProps} from './BuildStatusIcon';

export default {
  title: 'Xtages/Build/BuildIconStatus',
  component: BuildStatusIcon,
} as Meta;

const Template: Story<BuildStatusIconProps> = (args) => <BuildStatusIcon {...args} />;

export const Primary = Template.bind({});
Primary.args = {};
Primary.storyName = 'BuildStatusIcon';

export const StatusIconWithLabel = Template.bind({});
StatusIconWithLabel.args = {
  showLabel: true,
};
StatusIconWithLabel.storyName = 'BuildStatusIconWithLabel';
