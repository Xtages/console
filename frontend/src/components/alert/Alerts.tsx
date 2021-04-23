import React, {ReactNode} from 'react';
import cx from 'classnames';
import styles from './Alerts.module.scss';

/**
 * Renders an Alert box.
 * @param color - The color of the alert box and text.
 * @param outline - Whether to use an outline box or fill it if `false`.
 * @param children - The children to render inside the box.
 */
export default function Alert({
  color = 'primary',
  outline = false,
  children,
}: {
  color?: 'primary' | 'secondary' | 'neutral' | 'success' | 'info' | 'warning' | 'danger',
  outline?: boolean,
  children?: ReactNode
} = {}) {
  const alertType = `alert${outline ? '-outline' : ''}-${color}`;
  return (
    <div
      className={cx(styles.alert, styles[alertType])}
      role="alert"
    >
      {children}
    </div>
  );
}
