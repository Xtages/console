import {AlertTriangle, HelpCircle, Loader, ThumbsDown, ThumbsUp} from "react-feather";
import cx from "classnames";
import ReactTooltip from "react-tooltip";
import React from "react";

interface BuildStatusIconProps {
  status: 'not_provisioned' | 'succeeded' | 'failed' | 'unknown' | 'running',

  className?: string,
}

/** An icon to show the build status */
export function BuildStatusIcon({status, className}: BuildStatusIconProps) {
  let content: JSX.Element;
  let tooltip: string;
  if (status === 'succeeded') {
    content = <ThumbsUp/>;
    tooltip = 'Build succeeded';
  } else if (status === 'failed') {
    content = <ThumbsDown/>;
    tooltip = 'Build failed';
  } else if (status === 'not_provisioned') {
    content = <AlertTriangle/>;
    tooltip = 'Build failed to provision';
  } else if (status === 'unknown') {
    content = <HelpCircle/>;
    tooltip = 'Unknown';
  } else {
    content = <Loader/>;
    tooltip = 'Running';
  }
  return (
    <>
      <div
        data-tip="true"
        data-for="buildStatusTooltip"
        className={cx(className, {
            'text-success': status === 'succeeded',
            'text-danger': status === 'failed',
            'text-warning': status === 'not_provisioned',
            'text-primary': status === 'running',
        })}
      >
        {content}
      </div>
      <ReactTooltip id="buildStatusTooltip" place="top" effect="solid">
        {tooltip}
      </ReactTooltip>
    </>
  );
}
