import React, {useContext} from 'react';
import {BillingCycle, PlanType} from 'types/planTypes';

type SelectedPlanOptions = {
  billingCycle: BillingCycle;
  setBillingCycle: (cycle: BillingCycle) => void,
  planType: PlanType;
  setPlanType: (planType: PlanType) => void,
};

export function useSelectedPlan() {
  return useContext(SelectedPlanContext);
}

export const SelectedPlanContext = React.createContext<SelectedPlanOptions>({
  billingCycle: 'yearly',
  setBillingCycle: (ignoreCycle: BillingCycle) => {},
  planType: 'pro',
  setPlanType: (ignorePlanType: PlanType) => {},
});
