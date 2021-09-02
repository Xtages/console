import React, {ReactNode} from 'react';
import {Button, Card, Col, Container, OverlayTrigger, Row, Tooltip} from 'react-bootstrap';
import {Link} from 'react-router-dom';
import {GaugeConfig} from '@ant-design/charts/es/gauge';
import {GitMerge, Settings} from 'react-feather';
import {Deployment, DeploymentStatusEnum, Project} from 'gen/api';
import {formatDateTimeFull} from 'helpers/time';
import colors from 'assets/css/colors.module.scss';
import loadable from '@loadable/component';
import cx from 'classnames';
import {useQueryClient} from 'react-query';
import {ciApi} from 'service/Services';
import {GitHubCommitLink, GitHubLink} from '../link/XtagesLink';

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
  const prodDeploy = deployments.find((deployment) => deployment.env === 'production');
  const stagingDeploy = deployments.find((deployment) => deployment.env === 'staging');
  const showDivider = (prodDeploy && stagingDeploy) || showDeploymentSectionIfEmpty;
  return (
    <Col sm={colWidth} className="text-sm">
      {(prodDeploy || showDeploymentSectionIfEmpty) && (
      <DeploymentDetailsRow
        title="In production"
        name="production"
        projectName={name}
        deployment={prodDeploy}
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
      {(stagingDeploy || showDeploymentSectionIfEmpty) && (
        <DeploymentDetailsRow
          title="In staging"
          name="staging"
          projectName={name}
          deployment={stagingDeploy}
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

type DeploymentDetailsRowProps = {
  /** Row title */
  title: string;

  /** Deployment environment name */
  name: string;

  /** Project name */
  projectName: string;

  /** [Deployment] data */
  deployment: Deployment | undefined;

  showDeploymentLinkDetailsLink: boolean;
};

/**
 * Renders information about a deployment.
 */
function DeploymentDetailsRow({
  title,
  name,
  projectName,
  deployment,
  showDeploymentLinkDetailsLink,
}: DeploymentDetailsRowProps) {
  const isRunning = deployment?.status === DeploymentStatusEnum.Running;
  return (
    <Row>
      <Col>
        <h3 className="h5">
          {title}
        </h3>
        {deployment ? (
          <>
            <div>
              Commit:
              {' '}
              <GitHubCommitLink
                gitHubCommitUrl={deployment.commitUrl}
                commitHash={deployment.commitHash}
              />
            </div>
            <div>
              last deployed on:
              {' '}
              <OverlayTrigger overlay={(
                <Tooltip id="seeMoreBuildDetailsTooltip">
                  See build
                  details
                </Tooltip>
                  )}
              >
                <Link
                  className="font-weight-bold"
                  to={`/project/${projectName}/build/${deployment.id}`}
                >
                  {formatDateTimeFull(deployment.timestampInMillis)}
                </Link>
              </OverlayTrigger>
            </div>
            {showDeploymentLinkDetailsLink && (
              <Container className="px-0">
                <Row noGutters>
                  <Col sm="auto" className="pr-2">
                    Status:
                    {' '}
                    <span className={cx('font-weight-bolder', {'text-dark-success': isRunning})}>
                      {deployment.status}
                    </span>
                  </Col>
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
                </Row>
              </Container>
            )}
            <ServiceUrlsList deployment={deployment} />
          </>
        ) : (
          <span>
            There are no deployments to
            {' '}
            {name}
            {' '}
            yet.
          </span>
        )}
      </Col>
    </Row>
  );
}

function ServiceUrlsList({deployment}: {deployment: Deployment}) {
  const {serviceUrls, status} = deployment;
  return (status === DeploymentStatusEnum.Running
    ? (
      <div className="pt-2">
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
