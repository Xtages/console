import React, {FC} from 'react';
import {Card, CardDeck, ProgressBar} from 'react-bootstrap';
import {Clock, Codesandbox, DownloadCloud, IconProps} from 'react-feather';
import {toDate, differenceInDays} from 'date-fns';
import {ResourceType, UsageDetail, UsageDetailStatusEnum} from '../../gen/api';

export interface UsageDashboardProps {
  usageDetails: UsageDetail[];
}

/**
 * A dashboard showing usage details for {@link ResourceType}s.
 */
export function UsageDashboard({usageDetails}: UsageDashboardProps) {
  const usagePerResourceType = usageDetails.reduce((map, usage) => {
    // eslint-disable-next-line no-param-reassign
    map[usage.resourceType] = usage;
    return map;
  }, {} as Record<ResourceType, UsageDetail>);
  const projectUsage = usagePerResourceType[ResourceType.Project];
  const buildMinUsage = usagePerResourceType[ResourceType.MonthlyBuildMinutes];
  const dataTransferUsage = usagePerResourceType[ResourceType.MonthlyDataTransferGbs];
  return (
    <CardDeck>
      <UsageCard
        title="Projects"
        message={`${projectUsage.usage} / ${projectUsage.limit} projects`}
        usageDetails={projectUsage}
      />
      <UsageCard
        title="Build minutes"
        message={`${buildMinUsage.usage} min / ${buildMinUsage.limit} build minutes`}
        usageDetails={buildMinUsage}
      />
      <UsageCard
        title="Data transfer (egress)"
        message={`${dataTransferUsage.usage} / ${dataTransferUsage.limit} GB`}
        usageDetails={dataTransferUsage}
      />
    </CardDeck>
  );
}

const RESOURCE_TYPE_TO_ICON: Record<ResourceType, FC<IconProps>> = {
  [ResourceType.Project]: Codesandbox,
  [ResourceType.MonthlyBuildMinutes]: Clock,
  [ResourceType.MonthlyDataTransferGbs]: DownloadCloud,
};

interface UsageCardProps {
  /** The title of the card. */
  title: string;

  /** The message of the card. */
  message: string;

  usageDetails: UsageDetail;
}

/**
 * A {@link Card} that displays the usage of a {@link ResourceType}.
 */
function UsageCard({
  title,
  message,
  usageDetails,
}: UsageCardProps) {
  const icon = RESOURCE_TYPE_TO_ICON[usageDetails.resourceType];
  const iconEl = icon && React.createElement(icon);
  const resetsInDays = usageDetails.resetTimestampInMillis
    ? (differenceInDays(toDate(usageDetails.resetTimestampInMillis), Date.now()))
    : undefined;
  return (
    <Card
      border={usageDetails.status === UsageDetailStatusEnum.OverLimit ? 'danger' : undefined}
    >
      <Card.Body>
        <Card.Title>
          {iconEl}
          {' '}
          {title}
        </Card.Title>
        <Card.Text>
          <p>{message}</p>
          <p>
            {resetsInDays ? (
              <>
                Resets in
                {' '}
                {resetsInDays}
                {' '}
                days.
              </>
            ) : <>&nbsp;</>}
          </p>
        </Card.Text>
        <ProgressBar
          variant={usageDetails.status === UsageDetailStatusEnum.OverLimit ? 'danger' : 'light-success'}
          now={(usageDetails.usage * 100) / usageDetails.limit}
        />
      </Card.Body>
    </Card>
  );
}
