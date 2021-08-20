import React from 'react';
import {Meta, Story} from '@storybook/react';
import {CopiableSpan, CopiableSpanProps} from 'components/text/CopiableSpan';

export default {
  title: 'Xtages/CopiableSpan',
  component: CopiableSpan,
} as Meta;

const Template: Story<CopiableSpanProps> = (args) => <CopiableSpan {...args}>Copy me!</CopiableSpan>;

export const Primary = Template.bind({});
Primary.args = {};
Primary.storyName = 'CopiableSpan';
