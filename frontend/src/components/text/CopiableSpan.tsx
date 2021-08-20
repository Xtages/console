import React, {useEffect, useState} from 'react';
import {Copy} from 'react-feather';
import {onlyText} from 'react-children-utilities';
import {Button} from 'react-bootstrap';
import cx from 'classnames';
import styles from './CopiableSpan.module.scss';

export type CopiableSpanProps = JSX.IntrinsicElements['span'];

/**
 * A `<span>` of text that is copiable via a `<Button>` that is rendered. Only the test contents
 * will be copied.
 */
export function CopiableSpan({
  children,
  ...props
}: CopiableSpanProps) {
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setCopied(false);
    }, 2000);
    return () => clearTimeout(timer);
  }, [copied]);

  async function copy() {
    await navigator.clipboard?.writeText(onlyText(children));
    setCopied(true);
  }

  return (
    <span className={cx(styles.copySpan, {[styles.copied]: copied})}>
      <span {...props}>
        {children}
      </span>
      <Button
        variant="link"
        className={cx('btn-icon-only', styles.copyButton)}
      >
        <Copy
          size="1.2em"
          className="font-weight-bolder pl-1"
          onClick={copy}
        />
      </Button>
    </span>
  );
}
