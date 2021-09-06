import React from 'react';
import cx from 'classnames';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';
import {HelpCircle} from 'react-feather';
import {useTracking} from 'hooks/useTracking';
import styles from './XtagesLink.module.scss';

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
} & LinkWithTooltipProps;

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
      }, 'text-muted', 'text-underline--dashed', styles.gitHubLink, className)}
      target="_blank"
      rel="noreferrer"
    >
      {children}
    </LinkWithTooltip>
  );
}

type GitHubCommitLinkProps = {
  id?: string | undefined,
  commitHash: string;
  gitHubCommitUrl: string;
  length?: number;
} & GitHubLinkProps;

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
  ...props
}: GitHubCommitLinkProps) {
  const commit = commitHash.substr(0, length);
  return (
    <GitHubLink
      {...props}
      id={id}
      title="See commit in GitHub"
      href={gitHubCommitUrl}
      variant={variant}
    >
      {commit}
    </GitHubLink>
  );
}

const docsBaseUrl = 'https://docs.xtages.com';
const iconSizes = {
  sm: '0.7em',
  reg: '1em',
  lg: '1.5em',
};

type DocsLinkProps = {
  articlePath: string;
  icon?: boolean;
  size?: 'sm' | 'lg';
  title: string;
} & Omit<LinkWithTooltipProps, 'href' | 'onClick'>;

/**
 * A link to a documentation article.
 *
 * {@link articlePath} is require and should point to the path (after `docs.xtages.com`) of the
 * article we want to link to. If {@link icon} is false, then the `children` passed to this
 * component will be rendered, otherwise a `help` icon will be rendered.
 */
export function DocsLink({
  articlePath,
  icon = true,
  size,
  ...props
}: DocsLinkProps) {
  const {trackComponentEvent} = useTracking();

  function clickHandler(e: React.MouseEvent<HTMLAnchorElement>) {
    trackComponentEvent('DocsLink', 'Help Link Clicked', {
      helpPage: e.currentTarget.href,
    });
  }

  return (
    <LinkWithTooltip
      {...props}
      href={`${docsBaseUrl}${articlePath.startsWith('/') ? articlePath : `/${articlePath}`}`}
      className={cx('text-info', {noExternalLinkIcon: icon, [styles.docsLinkIcon]: icon})}
      onClick={clickHandler}
      target="_blank"
    >
      {icon
        ? (<HelpCircle size={iconSizes[size || 'reg']} />)
        : props.children}
    </LinkWithTooltip>
  );
}
