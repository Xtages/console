import React from 'react';
import {Meta, Story} from '@storybook/react';
import {BrowserRouter} from 'react-router-dom';
import NavBar from './NavBar';

export default {
  title: 'Xtages/NavBar',
  component: NavBar,
} as Meta;

const Template: Story = () => <BrowserRouter><NavBar /></BrowserRouter>;

export const Primary = Template.bind({});
Primary.args = {};
Primary.storyName = 'NavBar';
