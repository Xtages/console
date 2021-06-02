import React from 'react';
import {Meta, Story} from '@storybook/react';
import {Tab, Tabs} from './Tab';

export default {
  title: 'Xtages/Tabs',
  component: Tabs,
} as Meta;

const Template: Story<void> = () => (
  <Tabs>
    <Tab id="hello-1" title="Hello Tab 1">Hello 1</Tab>
    <Tab id="hello-2" title="Hello Tab 2">Hello 2</Tab>
  </Tabs>
);

export const Primary = Template.bind({});
Primary.storyName = 'Tabs';
