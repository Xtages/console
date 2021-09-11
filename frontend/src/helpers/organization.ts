import {Organization, OrganizationSubscriptionStatusEnum} from 'gen/api';
import {Nullable} from 'types/nullable';

/**
 * Returns `true` if {@link organization} is `Active` or `PendingCancellation`.
 * @param organization
 */
export function isOrgInGoodStanding(organization: Nullable<Organization>) {
  return organization !== null && (
    organization.subscriptionStatus === OrganizationSubscriptionStatusEnum.Active
      || organization.subscriptionStatus === OrganizationSubscriptionStatusEnum.PendingCancellation
  );
}
