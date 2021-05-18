import React, {ReactElement} from 'react';
import ReactTooltip from 'react-tooltip';
import Avatar from 'components/avatar/Avatar';
import {Link} from 'react-router-dom';
import {format, formatDuration, intervalToDuration} from 'date-fns';
import {ReactComponent as GoToDetails} from 'assets/img/GoToDetails.svg';
import {BuildStatusIcon} from 'components/build/Build';
import tableStyles from '../table/Table.module.scss';
import styles from './ProjectTable.module.scss';

export interface ProjectTableProps {
  children: ReactElement<typeof ProjectRow> | ReactElement<typeof ProjectRow>[]
}

/** A table that displays projects */
export function ProjectTable({children}: ProjectTableProps) {
  return (
    <table className={`table ${tableStyles.table} ${tableStyles.tableCards} ${styles.projectTable}`}>
      <thead>
        <tr>
          <th className="border-0" scope="col">Project</th>
          <th className="border-0" scope="col" colSpan={4}>Last build</th>
        </tr>
        <tr className={`${styles.subHead}`}>
          {/* eslint-disable-next-line jsx-a11y/control-has-associated-label */}
          <th className="border-0 border-bottom pr-0" />
          <th className="border-0 border-bottom pr-0" scope="col">Status</th>
          <th className="border-0 border-bottom pr-0" scope="col">Initiator</th>
          <th className="border-0 border-bottom pr-0" scope="col">Info</th>
          {/* eslint-disable-next-line jsx-a11y/control-has-associated-label */}
          <th className="border-0 border-bottom pr-0" />
        </tr>
      </thead>
      <tbody>
        {children}
      </tbody>
    </table>
  );
}

export interface ProjectRowProps {
  /** Project Id */
  id: number,

  /** Project name */
  name: string,

  /** GH repo url */
  repoUrl: string,

  /** Last build id */
  buildId: number,

  /** Last build status */
  buildStatus: 'not_provisioned' | 'succeeded' | 'failed' | 'unknown' | 'running',

  /** Last build initiator */
  buildInitiator: string,

  /** Last build initiator email */
  buildInitiatorEmail: string,

  /** Last build initiator avatar */
  buildInitiatorAvatarUrl: string,

  /** Last build commit hash */
  buildCommitHash: string,

  /** Last build commit hash GH url */
  buildCommitUrl: string,

  /** Last build start timestamp */
  buildStartTimestamp: number,

  /** Last build end timestamp */
  buildEndTimestamp: number,
}

/** A table row displaying a project and the last build info. */
export function ProjectRow({
  id,
  name,
  repoUrl,
  buildId,
  buildStatus,
  buildInitiator,
  buildInitiatorEmail,
  buildInitiatorAvatarUrl,
  buildCommitUrl,
  buildCommitHash,
  buildStartTimestamp,
  buildEndTimestamp,
}: ProjectRowProps) {
  return (
    <tr>
      <td className="h4 pr-4">
        <Link to={`/project/${id}`}>{name}</Link>
        <div>
          <a
            className="text-xs text-muted text-underline--dashed"
            href={repoUrl}
            target="_blank"
            rel="noreferrer"
          >
            See in GitHub
          </a>
        </div>
      </td>
      <td>
        <BuildStatusIcon status={buildStatus} className="pr-2 row justify-content-center" />
      </td>
      <td>
        <div className="media align-items-center pr-2">
          <div>
            <Avatar
              size="sm"
              rounded
              img={buildInitiatorAvatarUrl}
              imgAltText={buildInitiator}
            />
          </div>
          <div className="media-body ml-4">
            <span className="name h6 mb-0 text-sm">
              {buildInitiator}
            </span>
            {' '}
            <small className="d-block font-weight-bold">
              {buildInitiatorEmail}
            </small>
          </div>
        </div>
      </td>
      <td>
        <div className="text-sm">
          <div>
            <span className="font-weight-bold">Commit:</span>
            {' '}
            <a
              data-tip="true"
              data-for="seeCommitInGhTooltip"
              href={buildCommitUrl}
            >
              {buildCommitHash.substr(0, 6)}
            </a>
            <ReactTooltip id="seeCommitInGhTooltip" place="top" effect="solid">
              See commit in GitHub
            </ReactTooltip>
          </div>
          <div>
            <span className="font-weight-bold">Started on</span>
            {' '}
            <Link to={`project/${id}/build/${buildId}`}>
              <span
                data-tip="true"
                data-for="seeMoreBuildDetailsTooltip"
              >
                {format(buildStartTimestamp, 'MMM d, yyyy hh:mm:ss a')}
              </span>
            </Link>
            <ReactTooltip id="seeMoreBuildDetailsTooltip" place="top" effect="solid">
              See build details
            </ReactTooltip>
          </div>
          <div>
            <span className="font-weight-bold">and took</span>
            {' '}
            {formatDuration(intervalToDuration({
              start: buildStartTimestamp,
              end: buildEndTimestamp,
            }))}
          </div>
        </div>
      </td>
      <td>
        <Link to={`project/${id}`} data-tip="true" data-for="seeMoreProjectDetailsTooltip">
          <GoToDetails height={70} style={{stroke: '#A0AEC0'}} />
          <ReactTooltip id="seeMoreProjectDetailsTooltip" place="top" effect="solid">
            See more details about
            {' '}
            {name}
          </ReactTooltip>
        </Link>
      </td>
    </tr>
  );
}
