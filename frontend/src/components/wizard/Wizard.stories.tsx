import React from 'react';
import {Meta, Story} from '@storybook/react';
import {Wizard, WizardProps, WizardStep} from 'components/wizard/Wizard';

export default {
  title: 'Xtages/Wizard',
  component: Wizard,
} as Meta;

const Template: Story<WizardProps> = (args) => (
  <Wizard {...args}>
    <WizardStep step={1} title="Step #1" completed>
      First step
      {' '}
      <a href="https://yahoo.com">Yahoo</a>
    </WizardStep>
    <WizardStep step={2} title="Step #2">Second step</WizardStep>
    <WizardStep step={3} title="Step #3">
      Completed step
      {' '}
      <a href="https://yahoo.com">Yahoo</a>
    </WizardStep>
  </Wizard>
);

export const Primary = Template.bind({});
Primary.args = {};
Primary.storyName = 'Wizard';
