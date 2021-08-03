import React from 'react';
import {Link} from 'react-router-dom';
import {ReactComponent as GoToDetails} from 'assets/img/GoToDetails.svg';
import {Col, OverlayTrigger, Tooltip} from 'react-bootstrap';
import styles from './ProjectTable.module.scss';
import {Project} from '../../gen/api';
import {BuildRowInner} from '../build/BuildTable';
import {SimpleProjectCard} from './ProjectDetailsCard';

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
          <div className={`col-3 ${styles.head}`}>Project</div>
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
    <SimpleProjectCard project={project} projectVitalsColWidth={build ? 3 : 11}>
      {build && (
      <div className="col">
        <BuildRowInner build={build} project={project} collapsible={false} initiatorColWidth={4} />
      </div>
      )}
      <Col sm={1} className={styles.goToDetails}>
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
      </Col>
    </SimpleProjectCard>
  );
}
