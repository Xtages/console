import {Organization, OrganizationSubscriptionStatusEnum} from 'gen/api';

/**
 * Returns `true` if {@link organization} is `Active` or `PendingCancellation`.
 * @param organization
 */
export function isOrgInGoodStanding(organization: Organization) {
  return organization.subscriptionStatus === OrganizationSubscriptionStatusEnum.Unconfirmed
      || organization.subscriptionStatus === OrganizationSubscriptionStatusEnum.Active
      || organization.subscriptionStatus === OrganizationSubscriptionStatusEnum.PendingCancellation;
}
