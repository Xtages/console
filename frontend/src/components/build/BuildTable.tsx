import React, {useState} from 'react';
import ReactTooltip from 'react-tooltip';
import {Link} from 'react-router-dom';
import {Menu, MenuButton, MenuItem} from '@szhsin/react-menu';
import {ArrowUpCircle,
  Check,
  ChevronDown, ChevronUp,
  GitMerge,
  RotateCcw,
  UploadCloud,
  X} from 'react-feather';
import cx from 'classnames';
import {Build, BuildPhase, BuildTypeEnum, Project} from '../../gen/api';
import {BuildStatusIcon} from './BuildStatusIcon';
import Avatar from '../avatar/Avatar';
import '@szhsin/react-menu/dist/index.css';
import {Button} from '../button/Buttons';
import {durationString, formatDateTimeRelativeToNow} from '../../helpers/time';

export interface BuildTableProps {
  project: Project,
  builds: Build[],
}

/** Renders a list of [Build]s. */
export function BuildTable({
  project,
  builds,
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
      {builds.map((build) => (<BuildRow key={build.id} project={project} build={build} />))}
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
    <div className="container card">
      <div className="card-body p-3">
        <BuildRowInner {...args} collapsible />
      </div>
    </div>
  );
}

export type BuildRowInnerProps = {
  collapsible?: boolean,
} & BuildRowProps;

/** Render details of a [Build] (without wrapping it in a card). */
export function BuildRowInner({
  collapsible = false,
  project,
  build,
}: BuildRowInnerProps) {
  const [collapsed, setCollapsed] = useState(true);
  const toggleCollapsed = () => setCollapsed(!collapsed);

  return (
    <>
      <div className="row">
        <div className="col text-muted text-sm">
          {build.type === BuildTypeEnum.Ci ? (
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
          <hr className="mt-2 mb-3" />
        </div>
      </div>
      <div className="row">
        {collapsible && (
        <div className="col-1 text-muted px-0 text-center">
          <Button kind="white" className="p-0 text-muted" onClick={toggleCollapsed}>
            {collapsed ? <ChevronDown size="2em" /> : <ChevronUp size="2em" />}
          </Button>
        </div>
        )}
        <div className="col-1">
          <BuildStatusIcon status={build.status} showLabel />
        </div>
        <div className="col-3">
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
              <span className="font-weight-bold">Build for commit:</span>
              {' '}
              <a
                data-tip="true"
                data-for="seeCommitInGhTooltip"
                href={build.commitUrl}
                target="_blank"
                rel="noreferrer"
              >
                {build.commitHash.substr(0, 6)}
              </a>
              <ReactTooltip id="seeCommitInGhTooltip" place="top" effect="solid">
                See commit in GitHub
              </ReactTooltip>
            </div>
            <div>
              <span className="font-weight-bold">started</span>
              {' '}
              <Link to={`project/${project.id}/build/${build.id}`}>
                <span
                  data-tip="true"
                  data-for="seeMoreBuildDetailsTooltip"
                >
                  {formatDateTimeRelativeToNow(build.startTimestampInMillis)}
                </span>
              </Link>
              <ReactTooltip
                id="seeMoreBuildDetailsTooltip"
                place="top"
                effect="solid"
              >
                See build details
              </ReactTooltip>
            </div>
            {build.endTimestampInMillis
              ? (
                <>
                  <span className="font-weight-bold">and took</span>
                  {' '}
                  {durationString({
                    startInMillis: build.startTimestampInMillis,
                    endInMillis: build.endTimestampInMillis!!,
                  })}
                  {' '}
                  to finish
                </>
              ) : <span>is still running</span>}
          </div>
        </div>
        <div className="col-2">
          <Menu
            className="dropdown-menu"
            menuButton={(
              <MenuButton className="btn btn-outline-primary btn-sm">
                Actions
              </MenuButton>
                        )}
          >
            <MenuItem className="dropdown-item font-weight-bold">
              <span className="pr-2 text-dark-success">
                <UploadCloud
                  size="1.3em"
                />
              </span>
              Deploy to Prod
            </MenuItem>
            <MenuItem className="dropdown-item font-weight-bold">
              <span className="pr-2 text-primary">
                <ArrowUpCircle
                  size="1.3em"
                />
              </span>
              Deploy to Staging
            </MenuItem>
            <MenuItem className="dropdown-item font-weight-bold">
              <span className="pr-2 text-danger"><RotateCcw size="1.3em" /></span>
              Rollback
            </MenuItem>
          </Menu>
        </div>
      </div>
      {collapsible && !collapsed && (<BuildPhaseTable phases={build.phases} />)}
    </>
  );
}

function format(name: string) {
  const noUnderscores = name.toLowerCase()
    .replace(/_+/g, ' ');
  return noUnderscores.charAt(0)
    .toUpperCase() + noUnderscores.slice(1);
}

function PhaseStatus({status}: {status: string}) {
  return (
    <span className={cx({
      'text-dark-success': status === 'SUCCEEDED',
      'text-danger': status === 'FAILED',
    })}
    >
      {status === 'SUCCEEDED'
                  && <Check size="1em" className="mr-1" />}
      {status === 'FAILED' && <X size="1em" className="mr-1" />}
      {format(status)}
    </span>
  );
}

function BuildPhaseTable({phases}: {phases: BuildPhase[]}) {
  return (
    <div className="text-sm">
      <hr />
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
            <div className="col-6">
              Started
            </div>
            <div className="col-6">
              Duration
            </div>
          </div>
        </div>
      </div>
      {phases.map((phase) => (
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
            {phase.message || ''}
          </div>
          <div className="col-3">
            <div className="row">
              <div className="col-6 pr-0">
                {formatDateTimeRelativeToNow(phase.startTimestampInMillis)}
              </div>
              <div className="col-6">
                {durationString({
                  startInMillis: phase.startTimestampInMillis,
                  endInMillis: phase.endTimestampInMillis,
                })}
              </div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
