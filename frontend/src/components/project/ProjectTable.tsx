import React from 'react';
import {Link} from 'react-router-dom';
import {ReactComponent as GoToDetails} from 'assets/img/GoToDetails.svg';
import cx from 'classnames';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';
import styles from './ProjectTable.module.scss';
import {Project} from '../../gen/api';
import {BuildRowInner} from '../build/BuildTable';
import {GitHubLink} from '../link/XtagesLink';

export interface ProjectTableProps {
  /** A list of [Project]s and their last [Build] (if one exists). */
  projects: Project[],
}

/** A table that displays projects */
export function ProjectTable({projects}: ProjectTableProps) {
  return (
    <div className={styles.projectTable}>
      <div className="container">
        <div className="row px-3">
          <div className={`col-2 ${styles.head}`}>Project</div>
          <div className={`col ${styles.head}`}>Last build</div>
        </div>
      </div>
      {projects.map((project) => (
        <ProjectRow
          key={project.id}
          project={project}
        />
      ))}
    </div>
  );
}

export interface ProjectRowProps {
  project: Project,
}

/** A table row displaying a project and the last build info. */
export function ProjectRow({
  project,
}: ProjectRowProps) {
  const build = project.builds[0];
  return (
    <div className="container card mb-3">
      <div className="card-body p-3">
        <div className="row">
          <div className={cx({'col-2': build, 'col-11': !build})}>
            <h2 className="mb-0 lh-100">
              <Link to={`/project/${project.name}`}>{project.name}</Link>
            </h2>
            <div>
              <GitHubLink href={project.ghRepoUrl}>See in GitHub</GitHubLink>
            </div>
          </div>
          {build && (
          <div className="col">
            <BuildRowInner build={build} project={project} collapsible={false} />
          </div>
          )}
          <div className={`col-1 ${styles.goToDetails}`}>
            <Link
              className="h-100 d-flex justify-content-end"
              to={`project/${project.name}`}
            >
              <OverlayTrigger
                overlay={(
                  <Tooltip id="seeMoreProjectDetailsTooltip">
                    See more details about project
                    {' '}
                    &quot;
                    {project.name}
                    &quot;
                  </Tooltip>
                      )}
                placement="top"
              >
                <GoToDetails className="align-self-center" height={70} style={{stroke: '#A0AEC0'}} />
              </OverlayTrigger>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
