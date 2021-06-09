import React from 'react';
import {Card, Col, Container, OverlayTrigger, Row, Tooltip} from 'react-bootstrap';
import {Link} from 'react-router-dom';
import ReactApexChart from 'react-apexcharts';
import {ApexOptions} from 'apexcharts';
import colors from '../../assets/css/colors.module.scss';
import {GitHubCommitLink, GitHubLink} from '../link/XtagesLink';
import {Deployment, Project} from '../../gen/api';
import {formatDateTimeFull} from '../../helpers/time';

export interface ProjectDetailsCardProps {
  project: Project;
}

/**
 * Renders some vitals about a [Project] and its last deployments.
 */
export default function ProjectDetailsCard({project}: ProjectDetailsCardProps) {
  const prodDeployment = project.deployments.find((deployment) => deployment.env === 'production');
  const stagingDeployment = project.deployments.find((deployment) => deployment.env === 'staging');
  return (
    <Card>
      <Card.Body>
        <Container>
          <Row>
            <Col sm={3}>
              <h2 className="mb-0 lh-100">
                <Link to={`/project/${project.name}`}>{project.name}</Link>
              </h2>
              <div>
                <GitHubLink href={project.ghRepoUrl}>See in GitHub</GitHubLink>
              </div>
            </Col>
            <Col sm={6} className="text-sm">
              <DeploymentDetailsRow
                title="In production"
                name="production"
                projectName={project.name}
                deployment={prodDeployment}
              />
              <Row>
                <Col>
                  <hr className="m-2" />
                </Col>
              </Row>
              <DeploymentDetailsRow
                title="In staging"
                name="staging"
                projectName={project.name}
                deployment={stagingDeployment}
              />
            </Col>
            <Col sm={3} className="p-0">
              {project.percentageOfSuccessfulBuildsInTheLastMonth !== undefined
                && (
                <>
                  <BuildSuccessRadialBarChart
                    percentage={project.percentageOfSuccessfulBuildsInTheLastMonth * 100}
                  />
                  <div className="text-center">
                    <small>
                      % of successful builds in the last
                      month
                    </small>
                  </div>
                </>
                )}
            </Col>
          </Row>
        </Container>
      </Card.Body>
    </Card>
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
  let startColor: string;
  let endColor: string;
  if (percentage > 75) {
    startColor = colors.successLight;
    endColor = colors.successBootstrap;
  } else if (percentage > 50) {
    startColor = colors.warningLight;
    endColor = colors.warningBootstrap;
  } else {
    startColor = colors.dangerLight;
    endColor = colors.dangerBootstrap;
  }
  const options: ApexOptions = {
    chart: {
      height: 150,
      width: 50,
      type: 'radialBar',
      toolbar: {
        show: false,
      },
    },
    plotOptions: {
      radialBar: {
        inverseOrder: true,
        startAngle: -180,
        endAngle: 180,
        hollow: {
          margin: 0,
          size: '50%',
          background: '#fff',
          image: undefined,
          imageOffsetX: 0,
          imageOffsetY: 0,
          position: 'front',
          dropShadow: {
            enabled: true,
            blur: 4,
            opacity: 0.20,
          },
        },
        track: {
          background: '#fff',
          strokeWidth: '67%',
          margin: 0, // margin is in pixels
          dropShadow: {
            enabled: true,
            blur: 4,
            opacity: 0.20,
            color: percentage === 0 ? colors.dangerBootstrap : undefined,
          },
        },
        dataLabels: {
          show: true,
          name: {
            show: false,
          },
          value: {
            show: true,
            formatter(val: number) {
              return `${val}%`;
            },
            offsetY: 8,
            color: '#111',
            fontSize: '1.5em',
          },
        },
      },
    },
    colors: [startColor],
    fill: {
      type: 'gradient',
      gradient: {
        shade: 'dark',
        type: 'vertical',
        shadeIntensity: 0.5,
        gradientToColors: [endColor],
        inverseColors: true,
        opacityFrom: 1,
        opacityTo: 1,
        stops: [0, 100],
      },
    },
    stroke: {
      lineCap: 'round',
    },
  };
  return (
    <ReactApexChart
      options={options}
      series={[percentage]}
      type="radialBar"
    />
  );
}
