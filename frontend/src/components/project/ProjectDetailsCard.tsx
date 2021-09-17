import React, {ReactNode} from 'react';
import {Button, Card, Col, Container, OverlayTrigger, Row, Tooltip} from 'react-bootstrap';
import {Link} from 'react-router-dom';
import {GaugeConfig} from '@ant-design/charts/es/gauge';
import {GitMerge, Settings, UploadCloud} from 'react-feather';
import {Deployment, DeploymentStatusEnum, Project} from 'gen/api';
import {formatDateTimeFull} from 'helpers/time';
import colors from 'assets/css/colors.module.scss';
import loadable from '@loadable/component';
import cx from 'classnames';
import {useQueryClient} from 'react-query';
import {ciApi} from 'service/Services';
import {DocsLink, GitHubCommitLink, GitHubLink} from '../link/XtagesLink';

const LoadableGauge = loadable.lib(() => import('@ant-design/charts/es/gauge'));

export interface SimpleProjectCardProps {
  project: Project;

  children?: ReactNode | ReactNode[];

  projectVitalsColWidth?: number;

  showSettingsLink?: boolean;

  showRunCiButton?: boolean;
}

/**
 * A simple {@link Project} card, containing the projects name, github link and optionally a link
 * to the project's settings. Additionally it will render {@link children} next to the project
 * "vitals" column.
 */
export function SimpleProjectCard({
  children,
  project,
  projectVitalsColWidth = 3,
  showSettingsLink = true,
  showRunCiButton = true,
}
: SimpleProjectCardProps) {
  const queryClient = useQueryClient();

  async function runCi() {
    await ciApi.ci(project.name, {
      commitHash: 'HEAD',
    });
    await queryClient.invalidateQueries(project.name);
    await queryClient.invalidateQueries('projects');
  }

  return (
    <Card>
      <Card.Body>
        <Container>
          <Row>
            <Col sm={projectVitalsColWidth}>
              <h2 className="mb-0 lh-100">
                <Link to={`/project/${project.name}`}>{project.name}</Link>
              </h2>
              {showSettingsLink && (
              <div className="pt-2 text-sm">
                <Link to={`/project/${project.name}/settings`}>
                  <Settings size="1em" className="pr-1" />
                  Project settings
                </Link>
              </div>
              )}
              {showRunCiButton && (
              <div className="pt-2">
                <Button className="btn-xs" variant="success" onClick={runCi}>
                  <GitMerge size="1em" />
                  {' '}
                  Run CI from HEAD
                </Button>
              </div>
              )}
              <div className="pt-2">
                <GitHubLink href={project.ghRepoUrl}>See in GitHub</GitHubLink>
              </div>
            </Col>
            {children}
          </Row>
        </Container>
      </Card.Body>
    </Card>
  );
}

export type DeploymentDetailsProps = {
  project: Project;

  colWidth?: number;

  showDeploymentSectionIfEmpty?: boolean;

  showDeploymentLinkDetailsLink?: boolean;
};

export function DeploymentDetails({
  project,
  colWidth = 7,
  showDeploymentSectionIfEmpty = true,
  showDeploymentLinkDetailsLink = true,
}: DeploymentDetailsProps) {
  const {
    deployments,
    name,
  } = project;
  const prodDeploys = deployments.filter((deployment) => deployment.env === 'production');
  const stagingDeploys = deployments.filter((deployment) => deployment.env === 'staging');
  const showDivider = (prodDeploys.length > 0 && stagingDeploys.length > 0)
      || showDeploymentSectionIfEmpty;
  return (
    <Col sm={colWidth} className="text-sm">
      {(prodDeploys.length > 0 || showDeploymentSectionIfEmpty) && (
      <DeploymentsRow
        type="production"
        projectName={name}
        deployments={prodDeploys}
        showDeploymentLinkDetailsLink={showDeploymentLinkDetailsLink}
      />
      )}
      {showDivider && (
      <Row>
        <Col>
          <hr />
        </Col>
      </Row>
      )}
      {(stagingDeploys.length > 0 || showDeploymentSectionIfEmpty) && (
        <DeploymentsRow
          type="staging"
          projectName={name}
          deployments={stagingDeploys}
          showDeploymentLinkDetailsLink={showDeploymentLinkDetailsLink}
        />
      )}
    </Col>
  );
}

export function DeploymentDetailsAndBuildChart({project}: {project: Project}) {
  const {percentageOfSuccessfulBuildsInTheLastMonth} = project;
  return (
    <>
      <DeploymentDetails project={project} />
      <Col sm={2} className="p-0">
        {percentageOfSuccessfulBuildsInTheLastMonth !== undefined
          && (
          <>
            <BuildSuccessRadialBarChart
              percentage={percentageOfSuccessfulBuildsInTheLastMonth}
            />
            <div className="text-center pt-1">
              <small>
                % of successful builds in the last
                30 days
              </small>
            </div>
          </>
          )}
      </Col>
    </>
  );
}

type DeploymentsRowProps = {
  /** Deployment environment name */
  type: 'production' | 'staging';

  /** Project name */
  projectName: string;

  /** [Deployment] data */
  deployments: Deployment[];

  showDeploymentLinkDetailsLink: boolean;
};

/**
 * Renders information about a deployment.
 */
function DeploymentsRow({
  type,
  projectName,
  deployments,
  showDeploymentLinkDetailsLink,
}: DeploymentsRowProps) {
  const inProgressDeploy = deployments
    .find((deploy) => deploy.status === DeploymentStatusEnum.Stopping
      || deploy.status === DeploymentStatusEnum.Starting);
  const finishedDeploy = deployments
    .find((deploy) => deploy.status === DeploymentStatusEnum.Stopped
      || deploy.status === DeploymentStatusEnum.Running);
  let title: ReactNode;
  if (type === 'staging') {
    title = (
      <>
        In staging
        <DocsLink articlePath="/projects/deployments/" title="Deployments" size="sm" />
      </>
    );
  } else {
    title = (
      <>
        In production
        <DocsLink articlePath="/projects/promotions/" title="Promotions" size="sm" />
      </>
    );
  }
  let noDeploymentsMessage: ReactNode;
  if (type === 'staging') {
    noDeploymentsMessage = 'Your app is not running in staging yet.';
  } else {
    noDeploymentsMessage = 'Your app is not running in production yet.';
  }
  return (
    <Row>
      <Col>
        <h3 className="h5">
          {title}
        </h3>
        {deployments.length > 0 ? (
          <>
            <SingleDeployDetails
              deployment={finishedDeploy}
              projectName={projectName}
              showDeploymentLinkDetailsLink={showDeploymentLinkDetailsLink}
            />
            <SingleDeployDetails
              deployment={inProgressDeploy}
              projectName={projectName}
              showDeploymentLinkDetailsLink={showDeploymentLinkDetailsLink}
            />
          </>
        ) : noDeploymentsMessage}
      </Col>
    </Row>
  );
}

type SingleDeployDetailsProps = {
  deployment?: Deployment;

  /** Project name */
  projectName: string;

  showDeploymentLinkDetailsLink: boolean;
};

function SingleDeployDetails({
  deployment,
  projectName,
  showDeploymentLinkDetailsLink,
}: SingleDeployDetailsProps) {
  if (!deployment) {
    return <></>;
  }
  const inProgress = deployment.status === DeploymentStatusEnum.Stopping
      || deployment.status === DeploymentStatusEnum.Starting;
  let inProgressTitle: string;
  if (deployment.env === 'staging') {
    inProgressTitle = 'Changes to staging in progress...';
  } else {
    inProgressTitle = 'Changes to production in progress...';
  }
  return (
    <>
      <Container className={cx('p-0', {
        'border border-dark-secondary rounded p-2 mt-2': inProgress,
      })}
      >
        {inProgress && <h4 className="h6 text-muted">{inProgressTitle}</h4>}
        <Row noGutters>
          {inProgress && (
          <Col sm="auto" className="pr-2 d-flex flex-column justify-content-center">
            <span><UploadCloud size="2em" /></span>
          </Col>
          )}
          <Col sm="auto">
            <div>
              Commit:
              {' '}
              <GitHubCommitLink
                gitHubCommitUrl={deployment.commitUrl}
                commitHash={deployment.commitHash}
              />
            </div>
            {!inProgress && (
            <div>
              last deployed on:
              {' '}
              {formatDateTimeFull(deployment.timestampInMillis)}
            </div>
            )}
            {showDeploymentLinkDetailsLink && (
            <Container className="px-0">
              <Row noGutters>
                <Col sm="auto" className="pr-2">
                  Status:
                  {' '}
                  <span className={cx('font-weight-bolder',
                    {
                      'text-dark-success': deployment.status === DeploymentStatusEnum.Running,
                      'text-dark-danger': deployment.status === DeploymentStatusEnum.Stopped,
                      'text-dark-info': deployment.status === DeploymentStatusEnum.Stopping
                      || DeploymentStatusEnum.Starting,
                    })}
                  >
                    {deployment.status}
                  </span>
                </Col>
                {!inProgress && (
                <>
                  <Col sm="auto" className="text-muted user-select-none" aria-hidden>|</Col>
                  <Col sm="auto" className="pl-2">
                    <OverlayTrigger overlay={(
                      <Tooltip id="seeDeployLogsTooltip">
                        See deployment logs
                      </Tooltip>
                  )}
                    >
                      <Link
                        className="font-weight-bold"
                        to={`/deployments/${projectName}/${deployment.env}`}
                      >
                        Logs
                      </Link>
                    </OverlayTrigger>
                  </Col>
                </>
                )}
              </Row>
            </Container>
            )}
          </Col>
        </Row>
      </Container>
      {!inProgress && <ServiceUrlsList deployment={deployment} />}
    </>
  );
}

function ServiceUrlsList({deployment}: {deployment: Deployment}) {
  const {serviceUrls, status} = deployment;
  return (status === DeploymentStatusEnum.Running
    ? (
      <div>
        {serviceUrls.map((serviceUrl) => (
          <div key={serviceUrl}>
            <a
              href={serviceUrl}
              target="_blank"
              rel="noreferrer"
            >
              {serviceUrl}
            </a>
          </div>
        ))}
      </div>
    ) : <></>
  );
}

/** Renders a gauge chart of the percentage of successful builds. */
function BuildSuccessRadialBarChart({percentage}: {percentage: number}) {
  const config: GaugeConfig = {
    percent: percentage,
    renderer: 'svg',
    height: 140,
    range: {
      ticks: [0, 1],
      color: [`l(0) 0:${colors.danger} 0.5:${colors.warning} 1:${colors.success}`],
    },
    indicator: {
      pointer: {
        style: {
          stroke: colors.infoLight,
          lineWidth: 4,
        },
      },
      pin: {
        style: {
          stroke: colors.infoLight,
          r: 4,
        },
      },
    },
    axis: {
      label: {
        formatter: function formatter(v) {
          return Number(v) * 100;
        },
      },
      subTickLine: {count: 1},
    },
    statistic: {
      content: {
        offsetY: 10,
        style: {
          fontSize: '20px',
          color: '#4B535E',
        },
        formatter: function formatter(datum) {
          // @ts-ignore
          const {percent} = datum;
          return `${Math.trunc(percent * 100)}`;
        },
      },
    },
  };
  return (
    <LoadableGauge>
      {function render({default: GaugeChart}) {
        return <GaugeChart {...config} />;
      }}
    </LoadableGauge>
  );
}
