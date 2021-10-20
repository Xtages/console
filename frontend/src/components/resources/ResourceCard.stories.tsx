import React from 'react';
import {Meta, Story} from '@storybook/react';
import {ResourceCard, ResourceCardProps} from 'components/resources/ResourceCard';
import {ResourceProvisioningStatus, ResourceType} from 'gen/api';

export default {
  title: 'Xtages/ResourceCard',
  component: ResourceCard,
} as Meta;

const Template: Story<ResourceCardProps> = (args) => <ResourceCard {...args} />;

export const Primary = Template.bind({});
Primary.args = {
  resource: ResourceType.Postgresql,
  title: 'PostgreSQL',
};
Primary.storyName = 'ResourceCard';

export const ResourceProvisioned = Template.bind({});
ResourceProvisioned.args = {
  resource: ResourceType.Postgresql,
  title: 'PostgreSQL',
  provisioningStatus: ResourceProvisioningStatus.Provisioned,
};
ResourceProvisioned.storyName = 'ResourceCardProvisioned';

export const ResourceRequested = Template.bind({});
ResourceRequested.args = {
  resource: ResourceType.Postgresql,
  title: 'PostgreSQL',
  provisioningStatus: ResourceProvisioningStatus.Requested,
};
ResourceRequested.storyName = 'ResourceCardRequested';

export const ResourceComingSoon = Template.bind({});
ResourceComingSoon.args = {
  resource: ResourceType.Postgresql,
  title: 'PostgreSQL',
  comingSoon: true,
};
ResourceRequested.storyName = 'ResourceCardComingSoon';
