import React, {FC, useState} from 'react';
import {ArrowUpCircle,
  Check,
  ChevronDown,
  ChevronUp,
  GitMerge,
  IconProps,
  RotateCcw,
  UploadCloud,
  X} from 'react-feather';
import cx from 'classnames';
import {useQuery, useQueryClient} from 'react-query';
import {Alert,
  Button,
  Col,
  Container,
  Dropdown,
  DropdownButton,
  Row,
  Tab,
  Tabs} from 'react-bootstrap';
import {Build,
  BuildAction,
  BuildActionDisabledReason,
  BuildActionType,
  BuildPhase,
  BuildType,
  Project} from 'gen/api';
import {durationString, formatDateTimeMed, formatDateTimeRelativeToNow} from 'helpers/time';
import {cdApi, ciApi, logsApi} from 'service/Services';
import {UseQueryLoaderElement} from 'components/layout/UseQueryLoaderElement';
import {UserName} from 'components/user/UserName';
import {ButtonVariant} from 'react-bootstrap/types';
import {BuildStatusIcon} from './BuildStatusIcon';
import Avatar from '../avatar/Avatar';
import {LogViewer} from '../logviewer/LogViewer';
import {GitHubCommitLink} from '../link/XtagesLink';

export interface BuildTableProps {
  project: Project,
}

/** Renders a list of [Build]s. */
export function BuildTable({
  project,
}: BuildTableProps) {
  return (
    <>
      {project.builds.map((build) => (
        <BuildRow key={build.id} project={project} build={build} />))}
    </>
  );
}

interface BuildRowProps {
  project: Project,
  build: Build,
}

/** Render details of a [Build] in a card. */
export function BuildRow(args: BuildRowProps) {
  return (
    <div className="container card mb-3">
      <div className="card-body p-3">
        <BuildRowInner {...args} collapsible />
      </div>
    </div>
  );
}

export type BuildRowInnerProps = {
  collapsible?: boolean,

  initiatorColWidth?: number,
} & BuildRowProps;

/** Render details of a [Build] (without wrapping it in a card). */
export function BuildRowInner({
  collapsible = false,
  project,
  build,
  initiatorColWidth = 3,
}: BuildRowInnerProps) {
  const [collapsed, setCollapsed] = useState(true);
  const toggleCollapsed = () => setCollapsed(!collapsed);
  let buildIcon;
  // eslint-disable-next-line default-case
  switch (build.env) {
    case 'dev':
      buildIcon = <GitMerge size="1em" className="text-muted" />;
      break;
    case 'staging':
      buildIcon = <ArrowUpCircle size="1em" className="text-primary" />;
      break;
    case 'production':
      buildIcon = <UploadCloud size="1em" className="text-dark-primary" />;
      break;
  }

  return (
    <>
      <div className="row">
        <div className="col-12 text-sm pl-0 text-muted">
          {build.type === BuildType.Ci ? (
            <>
              {buildIcon}
              {' '}
              Integration
            </>
          ) : (
            <>
              {buildIcon}
              {' '}
              Deployment to
              {' '}
              {build.env}
            </>
          )}
          {' '}
          (#
          {build.buildNumber}
          )
          <hr />
        </div>
      </div>
      <div className="row">
        {collapsible && (
        <div className="col-1 text-muted px-0 text-center">
          <Button variant="grayish" className="p-0 text-muted" onClick={toggleCollapsed}>
            {collapsed ? <ChevronDown size="2em" /> : <ChevronUp size="2em" />}
          </Button>
        </div>
        )}
        <div className="col-1">
          <BuildStatusIcon status={build.status} showLabel />
        </div>
        <div className={`col-${initiatorColWidth}`}>
          <div className="media align-items-center pr-2">
            <div>
              <Avatar
                size="sm"
                rounded
                img={build.initiatorAvatarUrl ?? ''}
                imgAltText={build.initiatorName ?? ''}
              />
            </div>
            <div className="media-body ml-4">
              <UserName name={build.initiatorName} className="name h6 mb-0 text-sm" />
              {' '}
              <small className="d-block font-weight-bold">
                {build.initiatorEmail}
              </small>
            </div>
          </div>
        </div>
        <div className="col">
          <div className="text-sm">
            <div>
              Build for commit
              {' '}
              <GitHubCommitLink
                id="seeCommitInGhTooltip"
                className="font-weight-bolder"
                commitHash={build.commitHash}
                gitHubCommitUrl={build.commitUrl}
              />
            </div>
            <div>
              started
              {' '}
              <span className="font-weight-bolder">
                {formatDateTimeRelativeToNow(build.startTimestampInMillis)}
              </span>
            </div>
            {build.endTimestampInMillis
              ? (
                <>
                  and took
                  {' '}
                  <span className="font-weight-bolder">
                    {durationString({
                      startInMillis: build.startTimestampInMillis,
                      endInMillis: build.endTimestampInMillis!!,
                    })}
                  </span>
                  {' '}
                  to finish.
                </>
              ) : <span>is still running</span>}
          </div>
        </div>
        <div className="col-2">
          <BuildActionsDropdown project={project} build={build} />
        </div>
      </div>
      {collapsible && <AdditionalInfoPane collapsed={collapsed} project={project} build={build} />}
    </>
  );
}

/**
 * Dropdown with available Build actions.
 */
function BuildActionsDropdown({project, build}: {project: Project, build: Build}) {
  const queryClient = useQueryClient();

  async function runCi() {
    await ciApi.ci(project.name, {
      commitHash: build.commitHash,
    });
    await queryClient.invalidateQueries(project.name);
    await queryClient.invalidateQueries('projects');
  }

  async function deploy() {
    await cdApi.deploy(project.name, {
      commitHash: build.commitHash,
    });
    await queryClient.invalidateQueries(project.name);
    await queryClient.invalidateQueries('projects');
  }

  async function promote() {
    await cdApi.promote(project.name);
    await queryClient.invalidateQueries(project.name);
    await queryClient.invalidateQueries('projects');
  }

  async function rollback() {
    await cdApi.rollback(project.name);
    await queryClient.invalidateQueries(project.name);
    await queryClient.invalidateQueries('projects');
  }

  const buildActionsByType = build.actions.reduce(
    (map: Record<BuildActionType, BuildAction>, action) => {
      // eslint-disable-next-line no-param-reassign
      map[action.actionType] = action;
      return map;
    }, {} as Record<BuildActionType, BuildAction>,
  );

  return (
    <DropdownButton title="Actions" id={`actions-${build.id}`} menuRole="menu" size="sm">
      <BuildActionsDropdownItem
        buildAction={buildActionsByType[BuildActionType.Ci]}
        variant="success"
        onClick={runCi}
        icon={GitMerge}
      >
        Run CI
      </BuildActionsDropdownItem>
      <BuildActionsDropdownItem
        buildAction={buildActionsByType[BuildActionType.Promote]}
        variant="dark-primary"
        onClick={promote}
        icon={UploadCloud}
      >
        Promote to Prod
      </BuildActionsDropdownItem>
      <BuildActionsDropdownItem
        buildAction={buildActionsByType[BuildActionType.DeployToStaging]}
        variant="primary"
        onClick={deploy}
        icon={ArrowUpCircle}
      >
        Deploy to Staging
      </BuildActionsDropdownItem>
      <BuildActionsDropdownItem
        buildAction={buildActionsByType[BuildActionType.DeployToProduction]}
        variant="dark-primary"
        onClick={deploy}
        icon={UploadCloud}
      >
        Deploy to Production
      </BuildActionsDropdownItem>
      <BuildActionsDropdownItem
        buildAction={buildActionsByType[BuildActionType.Rollback]}
        variant="danger"
        onClick={rollback}
        icon={RotateCcw}
      >
        Rollback
      </BuildActionsDropdownItem>
    </DropdownButton>
  );
}

type BuildActionsDropdownItemProps = {
  buildAction?: BuildAction;
  children: String;
  variant: ButtonVariant;
  icon: FC<IconProps>;
  onClick: React.MouseEventHandler;
};

function BuildActionsDropdownItem({
  buildAction,
  children,
  variant,
  icon: Icon,
  onClick,
}: BuildActionsDropdownItemProps) {
  if (buildAction) {
    const {
      enabled,
      disabledReason,
    } = buildAction;
    const disabled = !enabled;
    return (
      <Dropdown.Item onClick={onClick} disabled={disabled}>
        <span className={cx('pr-2', {[`text-${variant}`]: enabled})}>
          <Icon size="1.3em" />
        </span>
        {children}
        {disabled && disabledReason === BuildActionDisabledReason.NotAvailableForFreePlan
          && <span className="d-block text-xs">(Not available on the Free plan)</span>}
      </Dropdown.Item>
    );
  }
  return null;
}

/** Renders the [Build] logs and phases */
function AdditionalInfoPane({
  project,
  build,
  collapsed,
}: {project: Project, build: Build, collapsed: boolean}) {
  const getBuildsLogsQuery = useQuery(
    `project/${project.name}/${build.type}/${build.id}/logs`,
    () => logsApi.buildLogs(project.name, build.id),
    {
      enabled: !collapsed,
    },
  );

  function maybeHandleLogsError(e: any) {
    if (e.isAxiosError
        && e.response !== undefined
        && e.response.data !== undefined
        && e.response.data.error === 'Not Found'
        && e.response.data.error_code === 'INVALID_LOGS') {
      return (
        <Row className="justify-content-center">
          <Col sm={12}>
            <Alert className="d-block text-center" variant="danger">
              We couldn&apos;t find any logs for this build.
            </Alert>
          </Col>
        </Row>
      );
    }
    return null;
  }

  if (!collapsed) {
    return (
      <Tabs defaultActiveKey="logs">
        <Tab eventKey="logs" title="Logs">
          <Container>
            <UseQueryLoaderElement
              queryResult={getBuildsLogsQuery}
              errorHandler={maybeHandleLogsError}
            >
              {function renderLogs(logsAxiosResponse) {
                return <LogViewer logLines={logsAxiosResponse.data.events} maxHeight={500} />;
              }}
            </UseQueryLoaderElement>
          </Container>
        </Tab>
        <Tab eventKey="phases" title="Phases">
          <BuildPhaseTable phases={build.phases} />
        </Tab>
      </Tabs>
    );
  }
  return <></>;
}

function format(name: string) {
  const noUnderscores = name.toLowerCase()
    .replace(/_+/g, ' ');
  return noUnderscores.charAt(0)
    .toUpperCase() + noUnderscores.slice(1);
}

function PhaseStatus({status}: {status: string}) {
  const succeeded = status === 'SUCCEEDED';
  const failed = status === 'FAILED';
  return (
    <span className={cx({
      'text-dark-success': succeeded,
      'text-danger': failed,
    })}
    >
      {succeeded && <Check size="1em" className="mr-1" />}
      {failed && <X size="1em" className="mr-1" />}
      {format(status)}
    </span>
  );
}

/** Renders a table with [BuildPhase]s. */
function BuildPhaseTable({phases}: {phases: BuildPhase[]}) {
  return (
    <div className="text-sm">
      <div className="row text-muted font-weight-bolder">
        <div className="col-3">
          <div className="row">
            <div className="col-5">
              Phase
            </div>
            <div className="col-7">
              Status
            </div>
          </div>
        </div>
        <div className="col">
          Message
        </div>
        <div className="col-3">
          <div className="row">
            <div className="col-8">
              Started
            </div>
            <div className="col-4 text-right">
              Duration
            </div>
          </div>
        </div>
      </div>
      {phases.map((phase) => {
        if (phase.name === 'SENT_TO_BUILD' || phase.name === 'COMPLETED') {
          return (<div key={phase.id} />);
        }
        return (
          <div className="row mt-3" key={phase.id}>
            <div className="col-3">
              <div className="row">
                <div className="col-5">
                  {format(phase.name)}
                </div>
                <div className="col-6">
                  <PhaseStatus status={phase.status} />
                </div>
              </div>
            </div>
            <div className="col">
              {phase.message || <div className="text-center">-</div>}
            </div>
            <div className="col-3">
              <div className="row">
                <div className="col-8 pr-0">
                  {formatDateTimeMed(phase.startTimestampInMillis)}
                </div>
                <div className="col-4 text-right">
                  {durationString({
                    startInMillis: phase.startTimestampInMillis,
                    endInMillis: phase.endTimestampInMillis,
                  })}
                </div>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
