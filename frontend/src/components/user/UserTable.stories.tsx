import React from 'react';
import {Meta, Story} from '@storybook/react';
import {UserTable, UserTableProps} from 'components/user/UserTable';
import {UserStatusEnum} from 'gen/api';

export default {
  title: 'Xtages/User/UserTable',
  component: UserTable,
} as Meta;

const Template: Story<UserTableProps> = (args) => <UserTable {...args} />;

export const Primary = Template.bind({});
Primary.args = {
  users: [
    {
      id: 0,
      name: 'Bosco Baracus',
      username: 'b.a.baracus@ateam.com',
      status: UserStatusEnum.Active,
      isOwner: false,
    },
    {
      id: 1,
      name: 'John "Hannibal" Smith',
      username: 'jsmith@ateam.com',
      status: UserStatusEnum.Active,
      isOwner: true,
    },
    {
      id: 2,
      name: 'Frankie Santana',
      username: 'fsantana@gmail.com',
      status: UserStatusEnum.Invited,
      isOwner: false,
    },
    {
      id: 3,
      name: '"Howling Mad" Murdock',
      username: 'murdock@ateam.com',
      status: UserStatusEnum.Expired,
      isOwner: false,
    },
  ],
};
Primary.storyName = 'UserTable';
