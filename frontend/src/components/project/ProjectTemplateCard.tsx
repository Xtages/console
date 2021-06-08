import React from 'react';
import {Button, Card} from 'react-bootstrap';
import Avatar from '../avatar/Avatar';

export interface ProjectTemplateCardProps {
  /** The id of the template, will be passed back in the `onClick` callback. */
  id: string,

  /** The title for the card. */
  title: string;

  /** A description of the template */
  description: string;

  /** The name of the image for the card */
  imageName: string;

  /** Callback that will be run when the "Create new project" button is clicked. */
  onClick?(id: string): void;
}

/** A Project template card with a button to create a new project from the template. */
export default function ProjectTemplateCard({
  id,
  title,
  imageName,
  description,
  onClick,
}: ProjectTemplateCardProps) {
  return (
    <Card>
      <Card.Body className="text-center">
        <Avatar
          img={`/img/project/template/${imageName}`}
          imgAltText={`Create new ${title} project`}
          rounded
          size="xl"
        />
        <span className="d-block h6 mt-2 mb-2">{title}</span>
        <span className="d-block text-sm text-muted mb-3">{description}</span>
        {onClick && (
          <div className="actions d-flex justify-content-center">
            <Button className="btn-xs" onClick={() => onClick && onClick(id)}>
              Create new project
            </Button>
          </div>
        )}
      </Card.Body>
    </Card>
  );
}
