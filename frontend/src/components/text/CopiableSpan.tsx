import React, {useState} from 'react';
import {Check, Copy} from 'react-feather';
import {onlyText} from 'react-children-utilities';
import {Button} from 'react-bootstrap';
import styles from './CopiableSpan.module.scss';

type CopiableSpanProps = JSX.IntrinsicElements['span'];

/**
 * A `<span>` of text that is copiable via a `<Button>` that is rendered. Only the test contents
 * will be copied.
 */
export function CopiableSpan({
  children,
  ...props
}: CopiableSpanProps) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    await navigator.clipboard?.writeText(onlyText(children));
    setCopied(true);
  }

  return (
    <span {...props}>
      {children}
      {copied
        ? <Check size="1.5em" className="text-dark-success font-weight-900 pl-1" />
        : (
          <Button variant="link" className={`btn-icon-only ${styles.copyButton}`}>
            <Copy
              size="1.2em"
              className="text-dark-info font-weight-bolder pl-1"
              onClick={copy}
            />
          </Button>
        )}
    </span>
  );
}
