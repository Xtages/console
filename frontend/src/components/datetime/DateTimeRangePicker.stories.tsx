import React from 'react';
import {Meta, Story} from '@storybook/react';
import {DateTimeRangePicker,
  DateTimeRangePickerProps} from 'components/datetime/DateTimeRangePicker';

export default {
  title: 'Xtages/DateTime/DateTimeRangePicker',
  component: DateTimeRangePicker,
} as Meta;

const Template: Story<DateTimeRangePickerProps> = (args) => <DateTimeRangePicker {...args} />;

export const Primary = Template.bind({});
Primary.args = {
  startDateTime: new Date(),
  endDateTime: new Date(),
};
Primary.storyName = 'DateTimeRangePicker';
