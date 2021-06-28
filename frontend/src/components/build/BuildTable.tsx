import React, {ReactNode, useState} from 'react';
import {Link} from 'react-router-dom';
import {ArrowUpCircle,
  Check,
  ChevronDown,
  ChevronUp,
  GitMerge,
  RotateCcw,
  UploadCloud,
  X} from 'react-feather';
import cx from 'classnames';
import {useQuery, useQueryClient} from 'react-query';
import {Button,
  Dropdown,
  DropdownButton,
  OverlayTrigger,
  Tab,
  Tabs,
  Tooltip} from 'react-bootstrap';
import {Build, BuildPhase, BuildType, Project} from '../../gen/api';
import {BuildStatusIcon} from './BuildStatusIcon';
import Avatar from '../avatar/Avatar';
import {durationString, formatDateTimeMed, formatDateTimeRelativeToNow} from '../../helpers/time';
import {cdApi, logsApi} from '../../service/Services';
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
      <div className="container">
        <div className="row p-3">
          <div className="col-1" />
          <div className="col-1">Status</div>
          <div className="col-3">Started by</div>
          <div className="col">Info</div>
          <div className="col-2" />
        </div>
      </div>
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
  const queryClient = useQueryClient();

  async function deploy() {
    await cdApi.deploy(project.name, {
      commitHash: build.commitHash,
    });
    await queryClient.invalidateQueries(project.name);
  }

  async function promote() {
    await cdApi.promote(project.name, {
      commitHash: build.commitHash,
      env: 'prod',
    });
    await queryClient.invalidateQueries(project.name);
  }

  async function rollback() {
    await cdApi.rollback(project.name, {
      commitHash: build.commitHash,
      env: 'prod',
    });
    await queryClient.invalidateQueries(project.name);
  }

  return (
    <>
      <div className="row">
        <div className="col-12 text-muted text-sm pl-0">
          {build.type === BuildType.Ci ? (
            <>
              <GitMerge size=".9em" />
              {' '}
              Integration
            </>
          ) : (
            <>
              <UploadCloud size=".9em" />
              {' '}
              Deployment
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
                imgAltText={build.initiatorName}
              />
            </div>
            <div className="media-body ml-4">
              <span className="name h6 mb-0 text-sm">
                {build.initiatorName}
              </span>
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
          <DropdownButton title="Actions" id={`actions-${build.id}`} menuRole="menu" size="sm">
            <Dropdown.Item onClick={promote}>
              <span className="pr-2 text-dark-success"><UploadCloud size="1.3em" /></span>
              Promote to Prod
            </Dropdown.Item>
            <Dropdown.Item onClick={deploy}>
              <span className="pr-2 text-primary"><ArrowUpCircle size="1.3em" /></span>
              Deploy to Staging
            </Dropdown.Item>
            <Dropdown.Item onClick={rollback}>
              <span className="pr-2 text-danger"><RotateCcw size="1.3em" /></span>
              Rollback
            </Dropdown.Item>
          </DropdownButton>
        </div>
      </div>
      {collapsible && <AdditionalInfoPane collapsed={collapsed} project={project} build={build} />}
    </>
  );
}

/** Renders the [Build] logs and phases */
function AdditionalInfoPane({
  project,
  build,
  collapsed,
}: {project: Project, build: Build, collapsed: boolean}) {
  if (!collapsed) {
    const {
      isLoading,
      error,
      data,
    } = useQuery(
      `project/${project.name}/${build.type}/${build.id}/logs`,
      () => logsApi.logs(project.name, build.id),
    );
    let logs: string | ReactNode;
    if (isLoading) {
      logs = 'Loading...';
    } else if (error) {
      logs = `An error has occurred: ${error}`;
    } else if (data?.data != null) {
      logs = (
        <LogViewer logLines={data.data.events} maxHeight={500} />
      );
    }
    return (
      <Tabs defaultActiveKey="logs">
        <Tab eventKey="logs" title="Logs">
          {logs}
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
