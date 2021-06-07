import {AlertTriangle, HelpCircle, Loader, ThumbsDown, ThumbsUp} from 'react-feather';
import cx from 'classnames';
import React from 'react';
import {BuildStatusEnum} from 'gen/api';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';

export type BuildStatusIconProps = {
  /** The status to render */
  status: BuildStatusEnum,

  /** Whether to show a label underneath the icon */
  showLabel?: boolean,
} & JSX.IntrinsicElements['div'];

/** An icon to show the build status */
export function BuildStatusIcon({
  status,
  showLabel = false,
  className,
}: BuildStatusIconProps) {
  let content: JSX.Element;
  let tooltip: string;
  let colorClass: string | undefined;
  let label: string;
  if (status === BuildStatusEnum.Succeeded) {
    content = <ThumbsUp />;
    tooltip = 'Build succeeded';
    label = 'Succeeded';
    colorClass = 'text-success';
  } else if (status === BuildStatusEnum.Failed) {
    content = <ThumbsDown />;
    tooltip = 'Build failed';
    label = 'Failed';
    colorClass = 'text-danger';
  } else if (status === BuildStatusEnum.NotProvisioned) {
    content = <AlertTriangle />;
    tooltip = 'Build failed to provision';
    label = 'Not provisioned';
    colorClass = 'text-warning';
  } else if (status === BuildStatusEnum.Unknown) {
    content = <HelpCircle />;
    tooltip = 'Unknown';
    label = 'Unknown';
  } else {
    content = <Loader />;
    tooltip = 'Running';
    label = 'Running';
    colorClass = 'text-primary';
  }
  return (
    <>
      <OverlayTrigger overlay={<Tooltip id="status">{tooltip}</Tooltip>} placement="top">
        <div className={cx(className, 'row', colorClass)}>
          <div className="col px-0">
            <div className="row justify-content-center">
              {content}
            </div>
            <div className="row justify-content-center text-sm">
              {showLabel && label}
            </div>
          </div>
        </div>
      </OverlayTrigger>
    </>
  );
}
