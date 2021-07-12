import React from 'react';
import {Col, Row} from 'react-bootstrap';
import {LogEvent} from 'gen/api';
import {formatDateTimeMed} from 'helpers/time';
import styles from './LogViewer.module.scss';

const map = new WeakMap();
let keyIndex = 0;

function getLineKey(logLine: LogEvent) {
  let key = map.get(logLine);
  if (!key) {
    key = `log-line-${keyIndex++}`;
    map.set(logLine, key);
  }
  return key;
}

export interface LogViewerProps {
  /** The [LogEvent]s to display. */
  logLines: LogEvent[];

  /** Max width of the viewer. */
  maxWidth?: number | undefined;

  /** Max height of the viewer. */
  maxHeight?: number | undefined;
}

export function LogViewer({logLines, maxWidth, maxHeight}: LogViewerProps) {
  return (
    <Row>
      <Col sm={12}>
        <table
          style={{
            maxWidth,
            maxHeight,
          }}
          className={styles.logViewer}
        >
          <tbody>
            {logLines.map((logLine, index) => (
              <tr key={getLineKey(logLine)}>
                <td className={styles.lineNumber} aria-hidden>{index}</td>
                <td className={styles.timestamp}>
                  |
                  {formatDateTimeMed(logLine.timestamp)}
                  |
                </td>
                <td>{logLine.message}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Col>
    </Row>
  );
}
