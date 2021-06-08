import React from 'react';
import cx from 'classnames';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';

type LinkWithTooltipProps = & JSX.IntrinsicElements['a'];

/** A link that will have a tooltip based on its [title] property if one exists. */
export function LinkWithTooltip(props: LinkWithTooltipProps) {
  const anchorEl = (
    <a {...props}>
      {props.children}
    </a>
  );
  if (props.title) {
    const effectiveId = props.id ?? props.href;
    return (
      <OverlayTrigger
        overlay={<Tooltip id={effectiveId!!}>{props.title}</Tooltip>}
        placement="top"
      >
        {anchorEl}
      </OverlayTrigger>
    );
  }
  return anchorEl;
}

type GitHubLinkProps = {
  variant?: 'xs' | 'sm' | 'reg' | 'lg' | 'xl';
} & JSX.IntrinsicElements['a'];

/** A link to a GitHub URL. It will always be opened in a new tab. */
export function GitHubLink({
  children,
  className,
  variant = 'xs',
  ...props
}: GitHubLinkProps) {
  return (
    <LinkWithTooltip
      {...props}
      id={`gitHub-${props.href}`}
      title={props.title ?? 'See in GitHub'}
      className={cx({
        'text-xs': variant === 'xs',
        'text-sm': variant === 'sm',
        'text-lg': variant === 'lg',
        'text-xl': variant === 'xl',
      }, 'text-muted', 'text-underline--dashed', className)}
      target="_blank"
      rel="noreferrer"
    >
      {children}
    </LinkWithTooltip>
  );
}

interface GitHubCommitLinkProps {
  id?: string | undefined,
  commitHash: string;
  gitHubCommitUrl: string;
  length?: number;
  variant?: 'xs' | 'sm' | 'reg' | 'lg' | 'xl';
}

/**
 * A link to a GitHub commit URL. It will always be opened in a new tab. The content of link is
 * the commit hash, which might be truncated if [length] (6 is the default) is specified.
 */
export function GitHubCommitLink({
  id = undefined,
  commitHash,
  gitHubCommitUrl,
  length = 6,
  variant = 'reg',
}: GitHubCommitLinkProps) {
  const commit = commitHash.substr(0, length);
  return (
    <GitHubLink
      id={id}
      title="See commit in GitHub"
      href={gitHubCommitUrl}
      variant={variant}
    >
      {commit}
    </GitHubLink>
  );
}
