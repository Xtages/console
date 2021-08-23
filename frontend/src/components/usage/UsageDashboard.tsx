import React, {FC} from 'react';
import {Card, CardDeck, ProgressBar} from 'react-bootstrap';
import {Clock, Codesandbox, Database, DownloadCloud, IconProps} from 'react-feather';
import {differenceInDays, toDate} from 'date-fns';
import {ResourceType, UsageDetail, UsageDetailStatusEnum} from 'gen/api';

type ResourceMetadata = {
  icon: FC<IconProps>;
  title: string;
  unit: string;
};

const RESOURCE_TO_METADATA = new Map<ResourceType, ResourceMetadata>([
  [ResourceType.Project, {
    icon: Codesandbox,
    title: 'Projects',
    unit: 'project(s)',
  }],
  [ResourceType.MonthlyBuildMinutes, {
    icon: Clock,
    title: 'Build minutes',
    unit: 'min',
  }],
  [ResourceType.MonthlyDataTransferGbs, {
    icon: DownloadCloud,
    title: 'Data transfer (egress)',
    unit: 'GB',
  }],
  [ResourceType.DbStorageGbs, {
    icon: Database,
    title: 'DB Storage',
    unit: 'GB',
  }],
]);

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
  return (
    <CardDeck>
      {Array.from(RESOURCE_TO_METADATA.keys()).map((resource) => (
        <UsageCard key={resource} usageDetails={usagePerResourceType[resource]} />
      ))}
    </CardDeck>
  );
}

/**
 * A {@link Card} that displays the usage of a {@link ResourceType}.
 */
function UsageCard({usageDetails}: {usageDetails: UsageDetail}) {
  const metaData = RESOURCE_TO_METADATA.get(usageDetails.resourceType)!;
  const {title, icon, unit} = metaData;
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
        <p>
          Used:
          {' '}
          {usageDetails.usage}
          {' '}
          {unit}
        </p>
        <p>
          Quota:
          {' '}
          {usageDetails.limit}
          {' '}
          {unit}
        </p>
        <p className="text-sm">
          {resetsInDays ? (
            <>
              (Quota resets in
              {' '}
              {resetsInDays}
              {' '}
              days.)
            </>
          ) : <>&nbsp;</>}
        </p>
        <ProgressBar
          variant={usageDetails.status === UsageDetailStatusEnum.OverLimit ? 'danger' : 'light-success'}
          now={(usageDetails.usage * 100) / usageDetails.limit}
        />
      </Card.Body>
    </Card>
  );
}
