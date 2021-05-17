import React from 'react';
import {Meta, Story} from '@storybook/react';
import {Button, ButtonProps} from './Buttons';

export default {
  title: 'Xtages/Button',
  component: Button,
} as Meta;

const Template: Story<ButtonProps> = (args) => <Button {...args}>Example</Button>;

export const Primary = Template.bind({});
Primary.args = {};
Primary.storyName = 'Button';
