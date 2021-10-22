import {useQuery} from 'react-query';
import {gitHubAppApi} from 'service/Services';
import {ReactComponent as GitHubIcon} from 'assets/img/github-icon.svg';
import {PlanSelector} from 'components/plan/PlanSelector';
import {OrganizationGitHubOrganizationTypeEnum, OrganizationSubscriptionStatusEnum} from 'gen/api';
import React, {ReactNode} from 'react';
import {Wizard, WizardStep} from 'components/wizard/Wizard';
import {Button, Col} from 'react-bootstrap';
import {Optional} from 'types/nullable';
import {Section, SectionTitle} from 'components/layout/Section';
import {Flag} from 'react-feather';
import {useOrganization} from 'hooks/useOrganization';
import {buildOauthUrl} from 'helpers/github';
import {useAuth} from 'hooks/useAuth';
import {useTracking} from 'hooks/useTracking';

export function SetupWizardSection() {
  return (
    <SetupWizard />
  );
}

function SetupWizard() {
  const {
    isSuccess,
    orgNotFound,
    organization,
  } = useOrganization();
  const {principal} = useAuth();
  const {trackComponentEvent} = useTracking();

  const {
    data: installUrlData,
    isSuccess: installUrlIsSuccess,
  } = useQuery('ghInstallUrl', () => gitHubAppApi.getInstallUrl(), {
    enabled: orgNotFound,
  });

  const needToInstallGitHubApp = orgNotFound && installUrlIsSuccess;
  const needToAuthorizedGitHubOauth = organization?.gitHubOrganizationType
      === OrganizationGitHubOrganizationTypeEnum.Individual
      && organization?.githubOauthAuthorized === false;
  if (needToInstallGitHubApp || needToAuthorizedGitHubOauth) {
    const installUrl = needToInstallGitHubApp
      ? installUrlData?.data!!
      : buildOauthUrl(organization?.name!!, principal?.id!!).toString();
    trackComponentEvent('SetupWizard', 'GitHub setup needed', {
      needToInstallGitHubApp,
      needToAuthorizedGitHubOauth,
      installUrl,
    });
    return (
      <Section>
        <SectionTitle
          icon={Flag}
          title="Finish your setup"
        />
        <Col sm={11}>
          <Wizard currentStep={2}>
            <NewAccountStep />
            <InstallGitHubAppStep installUrl={installUrl} />
            <SelectPlanStep />
          </Wizard>
        </Col>
      </Section>
    );
  }
  if (isSuccess
      && organization?.subscriptionStatus === OrganizationSubscriptionStatusEnum.Unconfirmed) {
    trackComponentEvent('SetupWizard', 'Checkout needed', {
      org: organization.name,
    });
    return (
      <Section>
        <SectionTitle
          icon={Flag}
          title="Finish your setup"
        />
        <Col sm={11}>
          <Wizard currentStep={3}>
            <NewAccountStep />
            <InstallGitHubAppStep completed />
            <SelectPlanStep enabled />
          </Wizard>
        </Col>
      </Section>
    );
  }
  return <></>;
}

function NewAccountStep() {
  return (
    <WizardStep step={1} title="Account creation" completed>
      Your account was created
    </WizardStep>
  );
}

type InstallGitHubAppStepProps = {
  installUrl?: Optional<string>;
  completed?: boolean;
};

function InstallGitHubAppStep({
  installUrl,
  completed = false,
}: InstallGitHubAppStepProps) {
  let callToAction: ReactNode;
  if (installUrl) {
    callToAction = (
      <a
        href={installUrl}
        className="btn btn-sm btn-primary btn-icon-label"
      >
        <span className="btn-inner--icon">
          <GitHubIcon height="1.5em" fill="white" />
        </span>
        <span className="btn-inner--text">Install</span>
      </a>
    );
  } else {
    callToAction = (
      <Button className="btn-icon-label" size="sm" disabled>
        <span className="btn-inner--icon">
          <GitHubIcon height="1.5em" fill="white" />
        </span>
        <span className="btn-inner--text">Install</span>
      </Button>
    );
  }
  return (
    <WizardStep step={2} title="Link your GitHub organization" completed={completed}>
      <p>
        Before we get rocking, you
        {' '}
        <strong>must</strong>
        {' '}
        install our GitHub App on your GitHub organization.
      </p>
      <p>
        Through our GitHub app, we will be able to:
      </p>
      <ul>
        <li>Create new repos backing Xtages projects.</li>
        <li>Run continuous integration on every push made to your project.</li>
        <li>Tag your repo when we deploy your project.</li>
      </ul>
      <p className="text-right">
        {callToAction}
      </p>
    </WizardStep>
  );
}

type SelectPlanStepProps = {
  completed?: boolean;
  enabled?: boolean;
};

function SelectPlanStep({
  completed = false,
  enabled = false,
}: SelectPlanStepProps) {
  return (
    <WizardStep step={3} title="Select your plan" completed={completed}>
      <PlanSelector purchaseEnabled={!completed && enabled} />
    </WizardStep>
  );
}
