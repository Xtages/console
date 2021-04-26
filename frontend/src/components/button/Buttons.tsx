import React from 'react';
import cx from 'classnames';
import styles from './Buttons.module.scss';

/**
 * Button that renders like a link. Useful for a de-emphasized clickable affordance.
 * @param children - The button's content
 * @param className - Optional `class` to apply to the element.
 * @param props - Everything else a button supports.
 */
export default function ButtonAsLink({
  children,
  className,
  ...props
}: JSX.IntrinsicElements['button']) {
  return (
    <button {...props} type="button" className={cx('btn', 'btn-link', 'rounded-0', 'p-0', styles.buttonAsLink, className)}>
      {children}
    </button>
  );
}
