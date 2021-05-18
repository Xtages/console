import React from 'react';
import {Meta, Story} from '@storybook/react';
import Avatar, {AvatarProps} from './Avatar';

export default {
  title: 'Xtages/Avatar',
  component: Avatar,
} as Meta;

const Template: Story<AvatarProps> = ({
  children,
  ...args
}: AvatarProps) => <Avatar {...args}>{children}</Avatar>;

const placeholderImg = 'http://www.fillmurray.com/100/100';

export const Primary = Template.bind({});
Primary.args = {
  children: 'MJ',
};
Primary.storyName = 'Avatar';

export const AvatarWithImg = Template.bind({});

AvatarWithImg.args = {
  img: placeholderImg,
  imgAltText: 'Placeholder',
};
AvatarWithImg.storyName = 'Avatar with Img';

export const AvatarWithLinkAndImg = Template.bind({});
AvatarWithLinkAndImg.args = {
  href: 'https://google.com',
  img: placeholderImg,
  imgAltText: 'Placeholder',
};
AvatarWithLinkAndImg.storyName = 'Avatar with Link and Img';

export const AvatarWithLinkAndText = Template.bind({});
AvatarWithLinkAndText.args = {
  href: 'https://google.com',
  children: 'YO',
};
AvatarWithLinkAndText.storyName = 'Avatar with Link and Text';
