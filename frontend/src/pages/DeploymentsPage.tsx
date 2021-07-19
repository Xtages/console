import React, {ReactNode, useState} from 'react';
import {useHistory, useParams} from 'react-router-dom';
import {useInfiniteQuery, useQuery} from 'react-query';
import {organizationApi, projectApi} from 'service/Services';
import UsageChecker from 'components/usage/UsageChecker';
import Page from 'components/layout/Page';
import {LoadIndicatingSection} from 'components/layout/Section';
import {Alert, Button, Col, Container, Dropdown, Row, Spinner} from 'react-bootstrap';
import {DeploymentDetails, SimpleProjectCard} from 'components/project/ProjectDetailsCard';
import {Deployment, Logs, Project} from 'gen/api';
import {UseInfiniteQueryLoaderElement} from 'components/layout/UseQueryLoaderElement';
import {LogViewer} from 'components/logviewer/LogViewer';
import {DateTimeRange, DateTimeRangePicker} from 'components/datetime/DateTimeRangePicker';
import {startOfToday} from 'date-fns';
import {UseInfiniteQueryResult} from 'react-query/types/react/types';
import {AxiosResponse} from 'axios';
import styles from './DeploymentsPage.module.scss';

export default function DeploymentsPage() {
  const {
    projectName,
    env,
  } = useParams<{projectName?: string, env?: string}>();
  const history = useHistory();
  const getProjectsDeployed = useQuery('projectsDeployed', () => organizationApi.projectsDeployed());
  const [dateTimeRange, setDateTimeRange] = useState<DateTimeRange>({
    startDateTime: startOfToday(),
    endDateTime: new Date(),
  });

  function changeDateRange(newDateRange: DateTimeRange) {
    setDateTimeRange(newDateRange);
  }

  function changeDeployment(project: Project, deployment: Deployment) {
    setDateTimeRange({
      startDateTime: startOfToday(),
      endDateTime: new Date(),
    });
    history.push(`/deployments/${project.name}/${deployment.env}`);
  }

  return (
    <>
      <UsageChecker />
      <Page>
        <LoadIndicatingSection queryResult={getProjectsDeployed} last>
          {function render(axiosResponse) {
            const sortedProjects = axiosResponse?.data
              .projects
              ?.slice()
              .sort((projectA, projectB) => projectA.name.localeCompare(projectB.name));

            // Check if one of the projects matches the project name from the route params, or
            // select the first one.
            const currentProject = sortedProjects?.find((project) => project.name === projectName)
                  || (sortedProjects !== undefined && sortedProjects[0])
                  || undefined;

            // Sort its deployments, putting the production ones first.
            const sortedDeployments = currentProject
              ?.deployments
              .slice()
              .sort((deployA, deployB) => {
                if (deployA.env === 'production') return -1;
                if (deployB.env === 'production') return 1;
                return 0;
              });

            // Check if one of the deployments matches the env from the route param, or select
            // the first one.
            const currentDeployment = sortedDeployments?.find((deploy) => deploy.env === env)
                  || (sortedDeployments && sortedDeployments[0])
                  || undefined;

            const getAppLogsQueryResult = useInfiniteQuery(
              [
                'deployLogs',
                currentProject!!.name,
                currentDeployment!!.id,
                currentDeployment!!.env,
                dateTimeRange.startDateTime.getTime(),
                dateTimeRange.endDateTime.getTime(),
              ],
              ({pageParam}) => projectApi.getDeployLogs(
                currentProject!!.name,
                currentDeployment!!.id,
                currentDeployment!!.env,
                dateTimeRange.startDateTime.getTime(),
                dateTimeRange.endDateTime.getTime(),
                pageParam,
              ), {
                // We only get appLogs if we have pinpointed a project and deployment.
                enabled: currentProject !== undefined && currentDeployment !== undefined,
                getNextPageParam: (lastPage, pages) => {
                  if (pages.length > 1) {
                    if (lastPage.data.nextToken !== pages[pages.length - 2].data.nextToken) {
                      return lastPage.data.nextToken;
                    }
                    return undefined;
                  }
                  return lastPage.data.nextToken;
                },
                keepPreviousData: true,
                refetchOnWindowFocus: false,
                refetchOnReconnect: false,
              },
            );

            let projectWithSingleDeployment: Project | undefined;
            if (currentProject && currentDeployment) {
              projectWithSingleDeployment = {...currentProject};
              projectWithSingleDeployment.deployments = [currentDeployment];
            }

            return (
              <Container>
                {sortedProjects && currentProject && currentDeployment && (
                <Row className="pb-3 justify-content-end">
                  <Col sm="auto">
                    <DeploymentSwitcher
                      projects={sortedProjects}
                      currentProject={currentProject}
                      currentDeployment={currentDeployment}
                      onDeploymentSelected={changeDeployment}
                    />
                  </Col>
                </Row>
                )}
                <Row>
                  <Col sm={12}>
                    {currentProject ? (
                      <SimpleProjectCard project={currentProject}>
                        <DeploymentDetails
                          project={projectWithSingleDeployment!!}
                          colWidth={9}
                          showDeploymentSectionIfEmpty={false}
                          showDeploymentLinkDetailsLink={false}
                        />
                      </SimpleProjectCard>
                    ) : (
                      <Alert variant="warning">
                        We couldn&apos;t find any deployed
                        projects
                      </Alert>
                    )}
                  </Col>
                </Row>
                <Row className="justify-content-end pb-3">
                  <Col sm="auto">
                    <DateTimeRangePicker
                      dateTimeRange={dateTimeRange}
                      onDateTimeRangeChange={changeDateRange}
                    />
                  </Col>
                </Row>
                <UseInfiniteQueryLoaderElement queryResult={getAppLogsQueryResult}>
                  {function renderLogs(logsAxiosResponse) {
                    const logEvents = logsAxiosResponse.pages
                      .flatMap((value) => value.data.events);
                    if (logEvents.length > 0) {
                      return (
                        <>
                          <LoadMoreButton queryResult={getAppLogsQueryResult} />
                          <LogViewer logLines={logEvents} />
                          <LoadMoreButton queryResult={getAppLogsQueryResult} />
                        </>
                      );
                    }
                    return (
                      <Row className="justify-content-center">
                        <Col sm="auto">
                          We coudn&apos;t find any logs for the selected time range.
                          Try adjusting the start date and end date.
                        </Col>
                      </Row>
                    );
                  }}
                </UseInfiniteQueryLoaderElement>
              </Container>
            );
          }}
        </LoadIndicatingSection>
      </Page>
    </>
  );
}

function LoadMoreButton({queryResult}: {queryResult: UseInfiniteQueryResult<AxiosResponse<Logs>>}) {
  const {hasNextPage, isFetchingNextPage, fetchNextPage} = queryResult;
  function loadMore() {
    return fetchNextPage();
  }
  let content: ReactNode;
  if (isFetchingNextPage) {
    content = (
      <span className="mx-auto align-middle d-inline-block">
        <Spinner animation="border" role="status" variant="light" size="sm">
          <span className="sr-only">Loading...</span>
        </Spinner>
      </span>
    );
  } else if (hasNextPage) {
    content = (
      <Button
        variant="link"
        className="btn-xs small h-100"
        block
        onClick={loadMore}
      >
        Load more
      </Button>
    );
  } else {
    content = <span className={`align-middle ${styles.noMoreLogsMsg}`}>There are no more entries to load.</span>;
  }
  return (
    <Row className={`justify-content-center ${styles.loadMoreButton}`}>
      <Col sm={12}><div className={styles.bg}>{content}</div></Col>
    </Row>
  );
}

type DeploymentSwitcherProps = {
  projects: Project[];

  currentProject: Project;

  currentDeployment: Deployment;

  onDeploymentSelected: (project: Project, deployment: Deployment) => void;
};

function DeployLabel({project, deployment}: {project: Project, deployment: Deployment}) {
  return (
    <>
      {project.name}
      {' '}
      [
      <span className="text-capitalize">
        {deployment.env}
      </span>
      ]
    </>
  );
}

function DeploymentSwitcher({
  projects,
  currentProject,
  currentDeployment,
  onDeploymentSelected,
}: DeploymentSwitcherProps) {
  function maybeRenderItem(project: Project) {
    return project.deployments.sort()
      .map((deployment) => {
        if (deployment.id !== currentDeployment.id) {
          return (
            <Dropdown.Item
              key={deployment.id}
              eventKey={deployment.id}
              onClick={() => onDeploymentSelected(project, deployment)}
            >
              <DeployLabel project={project} deployment={deployment} />
            </Dropdown.Item>
          );
        }
        return [];
      });
  }

  return (
    <Dropdown>
      <Dropdown.Toggle id="switchProjectToggle" size="sm" variant="secondary">
        <DeployLabel project={currentProject} deployment={currentDeployment} />
      </Dropdown.Toggle>
      <Dropdown.Menu className="bg-secondary" align="right">
        {projects.map(maybeRenderItem)
          .flat()}
      </Dropdown.Menu>
    </Dropdown>
  );
}
