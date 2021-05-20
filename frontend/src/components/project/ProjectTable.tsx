import React, {ReactElement} from 'react';
import ReactTooltip from 'react-tooltip';
import Avatar from 'components/avatar/Avatar';
import {Link} from 'react-router-dom';
import {format, formatDuration, intervalToDuration} from 'date-fns';
import {ReactComponent as GoToDetails} from 'assets/img/GoToDetails.svg';
import {BuildStatusIcon} from 'components/build/Build';
import tableStyles from '../table/Table.module.scss';
import styles from './ProjectTable.module.scss';
import {Build, Project} from '../../gen/api';

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
          <th className="border-0 border-bottom pr-0" scope="col">Started by</th>
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
  project: Project,
  build?: Build | undefined,
}

/** A table row displaying a project and the last build info. */
export function ProjectRow({project, build = undefined}: ProjectRowProps) {
  return (
    <tr>
      <td className="px-4">
        <h2 className="h3 mb-0 lh-100">
          <Link to={`/project/${project.id}`}>{project.name}</Link>
        </h2>
        <div>
          <a
            className="text-xs text-muted text-underline--dashed"
            href={project.ghRepoUrl}
            target="_blank"
            rel="noreferrer"
          >
            See in GitHub
          </a>
        </div>
      </td>
      <td className="pt-4">
        {build && <BuildStatusIcon showLabel status={build.status} className="pr-2" />}
      </td>
      <td>
        {build && (
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
        )}
      </td>
      <td>
        {build
        && (
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
            <span className="font-weight-bold">started on</span>
            {' '}
            <Link to={`project/${project.id}/build/${build.id}`}>
              <span
                data-tip="true"
                data-for="seeMoreBuildDetailsTooltip"
              >
                {format(build.startTimestampInMillis, 'MMM d, yyyy hh:mm:ss a')}
              </span>
            </Link>
            <ReactTooltip id="seeMoreBuildDetailsTooltip" place="top" effect="solid">
              See build details
            </ReactTooltip>
          </div>
            {build.endTimestampInMillis
              ? (
                <>
                  <span className="font-weight-bold">and took</span>
                  {' '}
                  {formatDuration(intervalToDuration({
                    start: build.startTimestampInMillis,
                    end: build.endTimestampInMillis!!,
                  }))}
                  {' '}
                  to finish
                </>
              ) : <span>is still running</span>}
        </div>
        )}
      </td>
      <td className={`${styles.goToDetails}`}>
        <Link to={`project/${project.id}`} data-tip="true" data-for="seeMoreProjectDetailsTooltip">
          <GoToDetails height={70} style={{stroke: '#A0AEC0'}} />
          <ReactTooltip id="seeMoreProjectDetailsTooltip" place="top" effect="solid">
            See more details about project
            {' '}
            &quot;
            {project.name}
            &quot;
          </ReactTooltip>
        </Link>
      </td>
    </tr>
  );
}
