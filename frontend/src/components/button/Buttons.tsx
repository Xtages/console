import React from 'react';
import cx from 'classnames';
import {button} from 'aws-amplify';
import styles from './Buttons.module.scss';

export type ButtonProps = {
  /** Kind of button */
  kind?: 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'info' | 'light' | 'dark',
  /** Whether the button is outlined */
  outlined?: boolean,
  size?: 'lg' | 'sm' | 'xs',
  /** Whether the button should be rendered to look like a link */
  asLink?: boolean,
} & JSX.IntrinsicElements['button'];

/**
 * Button component.
 */
export function Button({
  children,
  type = 'button',
  className,
  kind = 'primary',
  outlined = false,
  size,
  asLink = false,
  ...props
}: ButtonProps) {
  return (
    <button
      {...props}
      // eslint-disable-next-line react/button-has-type
      type={asLink ? 'button' : type}
      className={cx(
        'btn',
        {
          [`btn${outlined ? '-outline' : ''}-${kind}`]: !asLink,
          'btn-link': asLink,
          'rounded-0': asLink,
          'p-0': asLink,
          [`${styles.buttonAsLink}`]: asLink,
        },
        {
          'btn-lg': size === 'lg',
          'btn-sm': size === 'sm',
          [`${styles.btnXs}`]: size === 'xs',
        },
        className,
      )}
    >
      {children}
    </button>
  );
}
