import React from 'react';
import {Meta, Story} from '@storybook/react';
import {PlanSelector} from 'components/plan/PlanSelector';
import {AnalyticsProvider} from 'use-analytics';
import {buildAnalytics} from 'service/AnalyticsService';

export default {
  title: 'Xtages/PlanSelector',
  component: PlanSelector,
} as Meta;

const analytics = buildAnalytics();

const Template: Story<{}> = (args) => <AnalyticsProvider instance={analytics}><PlanSelector {...args} /></AnalyticsProvider>;

export const Primary = Template.bind({});
Primary.args = {};
Primary.storyName = 'PlanSelector';
