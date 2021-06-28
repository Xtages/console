import React from 'react';
import {Meta, Story} from '@storybook/react';
import {ProjectSettingsCard, ProjectSettingsCardProps} from './ProjectSettingsCard';
import {AssociatedDomainCertificateStatusEnum,
  DomainValidationRecordRecordTypeEnum,
  ProjectSettings,
  ProjectTypeEnum} from '../../gen/api';

export default {
  title: 'Xtages/Project/ProjectSettingsCard',
  component: ProjectSettingsCard,
} as Meta;

const projectData = {
  id: 10,
  name: 'console',
  ghRepoUrl: 'https://github.com/Xtages/console',
  organization: 'Xtages',
  type: ProjectTypeEnum.Node,
  version: '15',
  passCheckRuleEnabled: false,
  builds: [],
  deployments: [],
};

const projectSettingsData: ProjectSettings = {
  projectId: 10,
};

const Template: Story<ProjectSettingsCardProps> = (args) => <ProjectSettingsCard {...args} />;

export const Primary = Template.bind({});
Primary.args = {
  project: projectData,
  projectSettings: projectSettingsData,
};
Primary.storyName = 'ProjectSettingsCard';

export const ProjectSettingsCardWithDnsRecord = Template.bind({});
ProjectSettingsCardWithDnsRecord.args = {
  project: projectData,
  projectSettings: {
    ...projectSettingsData,
    associatedDomain: {
      name: 'yourdomain.com',
      certificateStatus: AssociatedDomainCertificateStatusEnum.Issued,
      validationRecord: {
        name: '_a79865eb4cd1a6ab990a45779b4e0b96.example.com.',
        recordType: DomainValidationRecordRecordTypeEnum.Cname,
        value: '_424c7224e9b0146f9a8808af955727d0.hkmpvcwbzw.acm-validations.aws.',
      },
    },
  },
};
ProjectSettingsCardWithDnsRecord.storyName = 'ProjectSettingsCardWithDnsRecord';
