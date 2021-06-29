import React from 'react';
import {Meta, Story} from '@storybook/react';
import {InviteUserFormCard} from 'components/user/InviteUserFormCard';

export default {
  title: 'Xtages/User/InviteUserFormCard',
  component: InviteUserFormCard,
} as Meta;

const Template: Story<{}> = (args) => <InviteUserFormCard {...args} />;

export const Primary = Template.bind({});
Primary.args = {};
Primary.storyName = 'InviteUserFormCard';
