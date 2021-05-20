import React from 'react';
import ReactTooltip from 'react-tooltip';
import {Link} from 'react-router-dom';
import {ReactComponent as GoToDetails} from 'assets/img/GoToDetails.svg';
import cx from 'classnames';
import styles from './ProjectTable.module.scss';
import {Build, Project, ProjectAndLastBuild} from '../../gen/api';
import {BuildRowInner} from '../build/BuildTable';

export interface ProjectTableProps {
  /** A list of [Project]s and their last [Build] (if one exists). */
  projectsAndBuilds: ProjectAndLastBuild[],
}

/** A table that displays projects */
export function ProjectTable({projectsAndBuilds}: ProjectTableProps) {
  return (
    <div className={styles.projectTable}>
      <div className="container">
        <div className="row px-3">
          <div className={`col-2 ${styles.head}`}>Project</div>
          <div className={`col ${styles.head}`}>Last build</div>
        </div>
      </div>
      {projectsAndBuilds.map((projectAndBuild) => (
        <ProjectRow
          key={projectAndBuild.project!.id}
          project={projectAndBuild.project!}
          build={projectAndBuild.lastBuild}
        />
      ))}
    </div>
  );
}

export interface ProjectRowProps {
  project: Project,
  build?: Build | undefined,
}

/** A table row displaying a project and the last build info. */
export function ProjectRow({
  project,
  build = undefined,
}: ProjectRowProps) {
  return (
    <div className="container card">
      <div className="card-body p-3">
        <div className="row">
          <div className={cx({'col-2': build, 'col-11': !build})}>
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
          </div>
          {build && (
          <div className="col">
            <BuildRowInner build={build} project={project} collapsible={false} />
          </div>
          )}
          <div className={`col-1 ${styles.goToDetails}`}>
            <Link
              className="h-100 d-flex"
              to={`project/${project.id}`}
              data-tip="true"
              data-for="seeMoreProjectDetailsTooltip"
            >
              <GoToDetails className="align-self-center" height={70} style={{stroke: '#A0AEC0'}} />
              <ReactTooltip id="seeMoreProjectDetailsTooltip" place="top" effect="solid">
                See more details about project
                {' '}
                &quot;
                {project.name}
                &quot;
              </ReactTooltip>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
