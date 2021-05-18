import React, {ReactNode} from 'react';
import cx from 'classnames';
import styles from './Avatar.module.scss';

export interface AvatarProps {
  children?: ReactNode | string,

  /** Optional url of the image for the Avatar. */
  img?: string,

  /** Optional alt text of the image for the Avatar. */
  imgAltText?: string,

  /** Optional url for a link wrapping the Avatar. */
  href?: string,

  /** Size of the Avatar. */
  size?: '2xl' | 'xl'| 'lg' |'sm' | 'xs' | undefined,

  rounded?: boolean,

  /** Avatar's background color. */
  background?: 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'info' | 'light' | 'dark',
}

/** Avatar component */
export default function Avatar({
  children,
  href,
  img,
  imgAltText,
  size,
  rounded = false,
  background = 'primary',
}: AvatarProps) {
  const hasImg = img !== undefined && img.trim().length > 0;
  const content = hasImg ? <img alt={imgAltText} src={img} /> : children;
  const classes = cx(styles.avatar, {
    [`${styles.avatar2xl}`]: size === '2xl',
    [`${styles.avatarXl}`]: size === 'xl',
    [`${styles.avatarLg}`]: size === 'lg',
    [`${styles.avatarSm}`]: size === 'sm',
    [`${styles.avatarXs}`]: size === 'xs',
    'rounded-circle': rounded,
    [`bg-${background}`]: !hasImg,
  });
  if (href !== undefined && href.trim().length > 0) {
    return (
      <a href={href} className={classes}>
        {content}
      </a>
    );
  }
  if (hasImg) {
    return (
      <img
        alt={imgAltText}
        src={img}
        className={classes}
      />
    );
  }
  return (
    <span className={classes}>{children}</span>
  );
}
