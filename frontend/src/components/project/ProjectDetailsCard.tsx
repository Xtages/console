import React, {ReactNode} from 'react';
import {Card, Col, Container, OverlayTrigger, Row, Tooltip} from 'react-bootstrap';
import {Link} from 'react-router-dom';
import {Gauge} from '@ant-design/charts';
import {GaugeConfig} from '@ant-design/charts/es/gauge';
import {Settings} from 'react-feather';
import colors from '../../assets/css/colors.module.scss';
import {GitHubCommitLink, GitHubLink} from '../link/XtagesLink';
import {Deployment, Project} from '../../gen/api';
import {formatDateTimeFull} from '../../helpers/time';

export interface SimpleProjectCardProps {
  project: Project;

  children?: ReactNode | ReactNode[];

  projectVitalsColWidth?: number;

  showSettingsLink?: boolean;
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
}
: SimpleProjectCardProps) {
  return (
    <Card>
      <Card.Body>
        <Container>
          <Row>
            <Col sm={projectVitalsColWidth}>
              <h2 className="mb-0 lh-100">
                <Link to={`/project/${project.name}`}>{project.name}</Link>
              </h2>
              <div>
                <GitHubLink href={project.ghRepoUrl}>See in GitHub</GitHubLink>
              </div>
              {showSettingsLink && (
              <div className="pt-2 text-sm">
                <Link to={`/project/${project.name}/settings`}>
                  <Settings size="1em" className="pr-1" />
                  Project settings
                </Link>
              </div>
              )}
            </Col>
            {children}
          </Row>
        </Container>
      </Card.Body>
    </Card>
  );
}

/**
 * Renders some vitals about a [Project] and its last deployments.
 */
export function ProjectDetailsCard({project}: SimpleProjectCardProps) {
  return (
    <SimpleProjectCard project={project}>
      <DeploymentDetailsAndBuildChart project={project} />
    </SimpleProjectCard>
  );
}

export type DeploymentDetailsProps = {
  project: Project;

  colWidth?: number;

  showDeploymentSectionIfEmpty?: boolean;
};

export function DeploymentDetails({
  project,
  colWidth = 7,
  showDeploymentSectionIfEmpty = true,
}: DeploymentDetailsProps) {
  const {
    deployments,
    name,
  } = project;
  const prodDeploy = deployments.find((deployment) => deployment.env === 'production');
  const stagingDeploy = deployments.find((deployment) => deployment.env === 'staging');
  return (
    <Col sm={colWidth} className="text-sm">
      {(prodDeploy || showDeploymentSectionIfEmpty) && (
        <>
          <DeploymentDetailsRow
            title="In production"
            name="production"
            projectName={name}
            deployment={prodDeploy}
          />
          <Row>
            <Col>
              <hr />
            </Col>
          </Row>
        </>
      )}
      {(stagingDeploy || showDeploymentSectionIfEmpty) && (
        <DeploymentDetailsRow
          title="In staging"
          name="staging"
          projectName={name}
          deployment={stagingDeploy}
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
                month
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
};

/**
 * Renders information about a deployment.
 */
function DeploymentDetailsRow({
  title,
  name,
  projectName,
  deployment,
}: DeploymentDetailsRowProps) {
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
                  to={`/project/${projectName}/build/${(deployment.id)}`}
                >
                  {formatDateTimeFull(deployment.timestampInMillis)}
                </Link>
              </OverlayTrigger>
            </div>
            <div>
              <a
                href={deployment.serviceUrl}
                target="_blank"
                rel="noreferrer"
              >
                {deployment.serviceUrl}
              </a>
              {' '}
            </div>
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
  return <Gauge {...config} />;
}
