import {Col, Container, Row} from 'react-bootstrap';
import React, {ReactNode} from 'react';

/**
 * A page that renders {@link children} in the middle of the screen.
 */
export function CenteredOnScreen({children} : {children: ReactNode | ReactNode[]}) {
  return (
    <Container>
      <Row>
        <Col sm={12} className="d-flex flex-column justify-content-center vh-100">
          <div className="align-self-center">
            {children}
          </div>
        </Col>
      </Row>
    </Container>
  );
}
