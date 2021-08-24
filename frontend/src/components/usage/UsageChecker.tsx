import React, {useState} from 'react';
import {useQuery} from 'react-query';
import {Toast} from 'react-bootstrap';
import {AlertOctagon} from 'react-feather';
import {Link} from 'react-router-dom';
import {usageApi} from 'service/Services';
import {ResourceType, UsageDetail, UsageDetailStatusEnum} from 'gen/api';
import styles from './UsageChecker.module.scss';

const ALERTABLE_RESOURCES_METADATA = new Map([
  [
    ResourceType.MonthlyBuildMinutes,
    [
      'The build minutes quota allowed by your plan has been exceeded.',
      'No new builds will be scheduled.',
    ],
  ],
  [
    ResourceType.MonthlyDataTransferGbs,
    [
      'The data transfer quota allowed by your plan has been exceeded.',
      'Requests to your running apps will be rejected.',
    ],
  ],
  [
    ResourceType.DbStorageGbs,
    [
      'The storage quota, allowed by your plan, for your DB has been exceeded.',
      'Queries to the DB will start failing',
    ],
  ],
]);
const ALERTABLE_RESOURCES = Array.from(ALERTABLE_RESOURCES_METADATA.keys());

/**
 * A toast informing the user that usage for a {@link ResourceType} is over the limit.
 */
function UsageOverLimitToast({usage}: {usage: UsageDetail}) {
  const messageLines = ALERTABLE_RESOURCES_METADATA.get(usage.resourceType);
  const message = <>{messageLines?.map((line) => <p key={line}>{line}</p>)}</>;
  const [show, setShow] = useState(true);
  const onClose = () => setShow(false);
  return (
    <Toast
      show={show}
      onClose={onClose}
      className={`${styles.usageOverLimitToast} border-danger`}
    >
      <Toast.Header className="bg-danger text-light">
        <AlertOctagon />
        <strong className="pl-2 mr-auto">
          Over quota
        </strong>
      </Toast.Header>
      <Toast.Body>
        {message}
        <p>
          See more usage details
          {' '}
          <Link className="alert-link" to="/account" onClick={onClose}>here</Link>
          .
        </p>
      </Toast.Body>
    </Toast>
  );
}

/**
 * Checks for usages over the limit and shows toasts informing the user about them.
 */
export default function UsageChecker() {
  const {
    isLoading,
    error,
    data,
  } = useQuery(
    'usage',
    () => usageApi.getAllUsageDetails(), {
      refetchOnWindowFocus: false,
      refetchInterval: false,
      refetchIntervalInBackground: false,
    },
  );
  if (isLoading || error || !data) {
    return <></>;
  }

  const usageDetails = data.data
    .filter((usage) => ALERTABLE_RESOURCES.includes(usage.resourceType))
    .filter((usage) => usage.status === UsageDetailStatusEnum.OverLimit);
  return (
    <div aria-live="polite" aria-atomic className={styles.usageCheckerToastArea}>
      <div className={styles.inner}>
        {usageDetails.map((usage) => (
          <UsageOverLimitToast
            key={usage.resourceType}
            usage={usage}
          />
        ))}
      </div>
    </div>
  );
}
