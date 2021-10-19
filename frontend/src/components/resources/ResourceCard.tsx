import React, {ReactNode, useState} from 'react';
import {ResourceProvisioningStatus, ResourceType} from 'gen/api';
import {Alert, Button, Card, Col, Row, Spinner} from 'react-bootstrap';
import cx from 'classnames';
import {Check} from 'react-feather';
import styles from './ResourceCard.module.scss';

export type ResourceCardProps = {
  resource: ResourceType,

  title: string;

  children?: ReactNode | ReactNode[];

  provisioningStatus?: ResourceProvisioningStatus;

  comingSoon?: boolean;

  onProvisionRequested?(resource: ResourceType): Promise<void>;
};

/**
 * A card showing a resource and it's current status. If it's not provisioned and it's not a
 * "coming soon" resource then a "provision" button will be displayed.
 */
export function ResourceCard({
  resource,
  title,
  children,
  provisioningStatus,
  comingSoon = false,
  onProvisionRequested,
}: ResourceCardProps) {
  const [submitting, setSubmitting] = useState(false);
  const imageName = resource.toLowerCase();

  let provisioningStatusText;
  if (provisioningStatus === ResourceProvisioningStatus.Requested) {
    provisioningStatusText = (
      <Alert variant="warning" className="p-2">
        <Spinner animation="grow" size="sm" className="mr-2" aria-hidden />
        <span className="font-weight-bolder">Provisioning in progress</span>
      </Alert>
    );
  } else if (provisioningStatus === ResourceProvisioningStatus.Provisioned) {
    provisioningStatusText = (
      <Alert variant="success" className="p-2">
        <Check size="1em" className="mr-2" />
        <span className="font-weight-bolder">Provisioning successful</span>
      </Alert>
    );
  }

  async function onClickHandler() {
    if (onProvisionRequested) {
      setSubmitting(true);
      await onProvisionRequested(resource);
      setSubmitting(false);
    }
  }

  return (
    <Card className={cx({[styles.comingSoon]: comingSoon})}>
      {comingSoon && <div className={styles.comingSoonOverlay} />}
      {comingSoon && (
        <Row noGutters>
          <Col sm="auto">
            <Card.Header className="h6 pt-2 pb-0">Coming soon</Card.Header>
          </Col>
        </Row>
      )}
      <Row noGutters>
        <Col className="d-flex pl-3" sm={2}>
          <Card.Img className="h-50 align-self-center" src={`/img/resource/${imageName}.svg`} />
        </Col>
        <Col sm={10}>
          <Card.Body>
            <Card.Title>{title}</Card.Title>
            <Card.Text>{children}</Card.Text>
            {provisioningStatusText}
            {!provisioningStatus && !comingSoon
              && (
              <Button
                onClick={onClickHandler}
                className="btn-xs mt-1"
                disabled={submitting}
              >
                {submitting && (
                <Spinner
                  className="mr-2"
                  as="span"
                  animation="grow"
                  size="sm"
                  aria-hidden
                />
                )}
                Provision
              </Button>
              )}
          </Card.Body>
        </Col>
      </Row>
    </Card>
  );
}
