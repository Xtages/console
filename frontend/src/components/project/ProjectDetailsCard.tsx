import React from 'react';
import {Card, Col, Container, OverlayTrigger, Row, Tooltip} from 'react-bootstrap';
import {Link} from 'react-router-dom';
import ReactApexChart from 'react-apexcharts';
import {ApexOptions} from 'apexcharts';
import colors from '../../assets/css/colors.module.scss';
import {GitHubCommitLink, GitHubLink} from '../link/XtagesLink';
import {Project} from '../../gen/api';
import {formatDateTimeFull} from '../../helpers/time';

export interface ProjectDetailsCardProps {
  project: Project;
}

/**
 * Renders some vitals about a [Project] and its last deployments.
 */
export default function ProjectDetailsCard({project}: ProjectDetailsCardProps) {
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
                env="Prod"
                commitHash="abcdeeb"
                gitHubCommitUrl=""
                projectId={project.id}
                deployBuildId={123}
                lastDeployTimestampInMillis={99990}
                serviceUrl="https://somecompany-someproject.xtages.dev"
              />
              <Row>
                <Col>
                  <hr className="m-2" />
                </Col>
              </Row>
              <DeploymentDetailsRow
                env="Staging"
                commitHash="abcdeeb"
                gitHubCommitUrl=""
                projectId={project.id}
                deployBuildId={123}
                lastDeployTimestampInMillis={99990}
                serviceUrl="https://somecompany-someproject.xtages.dev"
              />
            </Col>
            <Col sm={3} className="p-0">
              <BuildSuccessRadialBarChart percentage={100} />
              <div className="text-center">
                <small>
                  % of successful builds in the last
                  month
                </small>
              </div>
            </Col>
          </Row>
        </Container>
      </Card.Body>
    </Card>
  );
}

interface DeploymentDetailsRowProps {
  env: string;

  commitHash: string;

  gitHubCommitUrl: string;

  projectId: number,

  deployBuildId: number;

  lastDeployTimestampInMillis: number;

  serviceUrl: string,
}

/**
 * Renders information about a deployment.
 */
function DeploymentDetailsRow({
  env,
  commitHash,
  gitHubCommitUrl,
  projectId,
  deployBuildId,
  lastDeployTimestampInMillis,
  serviceUrl,
}: DeploymentDetailsRowProps) {
  return (
    <Row>
      <Col>
        <h3 className="h5">
          {env}
          :
        </h3>
        <div>
          Commit:
          {' '}
          <GitHubCommitLink gitHubCommitUrl={gitHubCommitUrl} commitHash={commitHash} />
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
              to={`/project/${projectId}/build/${deployBuildId}`}
            >
              {formatDateTimeFull(lastDeployTimestampInMillis)}
            </Link>
          </OverlayTrigger>
        </div>
        <div>
          <a href={serviceUrl} target="_blank" rel="noreferrer">{serviceUrl}</a>
          {' '}
        </div>
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
            top: 3,
            left: 0,
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
            top: -3,
            left: 0,
            blur: 4,
            opacity: 0.20,
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
