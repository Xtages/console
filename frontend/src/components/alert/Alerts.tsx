import React, {ReactNode} from 'react';
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
} = {}): JSX.Element {
  return (
    <div
      className={`${styles.alert} ${styles[`alert${outline ?? '-outline'}-${color}`]}`}
      role="alert"
    >
      {children}
    </div>
  );
}
