import React, {ReactNode, MouseEvent, useState} from 'react';
import {Badge,
  Button,
  Card,
  Col,
  Container,
  Row,
  ToggleButton,
  ToggleButtonGroup} from 'react-bootstrap';
import cx from 'classnames';
import {BillingCycle, PlanType} from 'types/planTypes';
import {SelectedPlanContext, useSelectedPlan} from 'hooks/useSelectedPlan';
import {Nullable} from 'types/nullable';
import {useTracking} from 'hooks/useTracking';
import redirectToStripeCheckoutSession from 'service/CheckoutService';
import {checkoutApi} from 'service/Services';
import {useQueryClient} from 'react-query';
import styles from './PlanSelector.module.scss';

type SkuDetails = {
  priceId: string;
  price: string;
  billingInfo?: string | null;
  callToAction: string;
  title: string;
  subtitle?: Nullable<string>;
};
type Plan = Record<BillingCycle, SkuDetails>;
type Plans = Record<PlanType, Plan>;

let plans: Plans = {
  free: {
    monthly: {
      priceId: '',
      price: '0',
      callToAction: 'Get started',
      title: 'Free',
    },
    yearly: {
      priceId: '',
      price: '0',
      callToAction: 'Get started',
      title: 'Free',
    },
  },
  starter: {
    monthly: {
      priceId: 'price_1JrxP2IfxICi4AQgc1IrRkmF',
      price: '188',
      billingInfo: 'per month',
      callToAction: 'Purchase Starter',
      title: 'Starter',
    },
    yearly: {
      priceId: 'price_1JrxN0IfxICi4AQgOdyA0KrI',
      price: '150',
      billingInfo: 'per month, paid yearly',
      callToAction: 'Purchase Starter',
      title: 'Starter',
    },
  },
  pro: {
    monthly: {
      priceId: 'price_1JrxRfIfxICi4AQgqOq4bq3C',
      price: '438',
      billingInfo: 'per month',
      callToAction: 'Purchase Pro',
      title: 'Professional',
    },
    yearly: {
      priceId: 'price_1JrxQUIfxICi4AQgdh0e4NG3',
      price: '350',
      billingInfo: 'per month, paid yearly',
      callToAction: 'Purchase Pro',
      title: 'Professional',
    },
  },
};

if (process.env.NODE_ENV !== 'production') {
  plans = {
    free: {
      monthly: {
        priceId: '',
        price: '0',
        callToAction: 'Get started',
        title: 'Free',
      },
      yearly: {
        priceId: '',
        price: '0',
        callToAction: 'Get started',
        title: 'Free',
      },
    },
    starter: {
      monthly: {
        priceId: 'price_1JrxP2IfxICi4AQgc1IrRkmF',
        price: '188',
        billingInfo: 'per month',
        callToAction: 'Purchase Starter',
        title: 'Starter',
      },
      yearly: {
        priceId: 'price_1JrxN0IfxICi4AQgOdyA0KrI',
        price: '150',
        billingInfo: 'per month, paid yearly',
        callToAction: 'Purchase Starter',
        title: 'Starter',
      },
    },
    pro: {
      monthly: {
        priceId: 'price_1JrxRfIfxICi4AQgqOq4bq3C',
        price: '438',
        billingInfo: 'per month',
        callToAction: 'Purchase Pro',
        title: 'Professional',
      },
      yearly: {
        priceId: 'price_1JrxQUIfxICi4AQgdh0e4NG3',
        price: '350',
        billingInfo: 'per month, paid yearly',
        callToAction: 'Purchase Pro',
        title: 'Professional',
      },
    },
  };
}

export type PlanSelectorProps = {
  purchaseEnabled?: boolean;

  showFreePlan?: boolean;
};

export function PlanSelector({purchaseEnabled = true, showFreePlan = true}: PlanSelectorProps) {
  const [billingCycle, setBillingCycle] = useState<BillingCycle>('yearly');
  const [planType, setPlanType] = useState<PlanType>('pro');
  const {trackComponentEvent} = useTracking();
  const queryClient = useQueryClient();

  function recordEnterprisePlanClick(e: MouseEvent<HTMLAnchorElement>) {
    if (purchaseEnabled) {
      trackComponentEvent('PlanSelector', 'Enterprise plan clicked');
    } else {
      e.preventDefault();
    }
  }

  async function freePlanClickHandler() {
    await checkoutApi.freeTierCheckout();
    await queryClient.invalidateQueries({queryKey: 'org'});
  }

  return (
    <SelectedPlanContext.Provider value={{
      billingCycle,
      setBillingCycle,
      planType,
      setPlanType,
    }}
    >
      <Container className={styles.planSelector}>
        <BillingCycleToggle size="sm" />
        <Row className="mb-3 mt-4 shadow-none justify-content-center">
          {showFreePlan
          && (
          <Col lg={3}>
            <PlanCard
              planType="free"
              purchaseEnabled={purchaseEnabled}
              onClick={freePlanClickHandler}
            >
              <ul className="list-unstyled text-sm mb-4">
                <li>
                  Deploy 1 app
                  <div>(1 environment)</div>
                </li>
                <li>100 CI/CD credits (Linux)</li>
                <li>1 GB of data-transfer</li>
                <li>Deploys to the Xtages Cloud</li>
                <li>Log collection</li>
                <li>
                  Metrics dashboard
                  <span className="d-block text-muted text-sm">(coming soon)</span>
                </li>
              </ul>
            </PlanCard>
          </Col>
          )}
          <Col lg={3}>
            <PlanCard planType="starter" purchaseEnabled={purchaseEnabled}>
              <ul className="list-unstyled text-sm mb-4">
                <li>
                  Deploy up to 2 apps
                  <div>(2 environments for each)</div>
                </li>
                <li>2500 CI/CD credits (Linux)</li>
                <li>20 GB of data-transfer</li>
                <li>Deploys to the Xtages Cloud</li>
                <li>Log collection</li>
                <li>
                  Metrics dashboard
                  <span className="d-block text-muted text-sm">(coming soon)</span>
                </li>
              </ul>
            </PlanCard>
          </Col>
          <Col lg={3}>
            <PlanCard planType="pro" highlighted purchaseEnabled={purchaseEnabled}>
              <ul className="list-unstyled text-white text-sm opacity-8 mb-4">
                <li>
                  Deploy up to 3 apps
                  <div>(2 environments for each)</div>
                </li>
                <li>7500 CI/CD credits (Linux)</li>
                <li>500 GB of data-transfer</li>
                <li>Deploys to the Xtages Cloud</li>
                <li>Log collection</li>
                <li>
                  Metrics dashboard
                  <span className="d-block text-white text-sm">(coming soon)</span>
                </li>
              </ul>
            </PlanCard>
          </Col>
          <Col lg={3}>
            <Card className="card-pricing text-center px-3 shadow border-0">
              <div className="card-header border-0 delimiter-bottom">
                <div className="h2 text-center mb-0">Enterprise</div>
                <span className="h6 text-muted">&nbsp;</span>
              </div>
              <Card.Body>
                <p className="my-7 h4">
                  Need more?
                </p>
                <a
                  href="mailto:support@xtages.com"
                  target="_blank"
                  onClick={recordEnterprisePlanClick}
                  className="btn btn-sm btn-warning hover-translate-y-n3 hover-shadow-lg mb-3 noExternalLinkIcon"
                  rel="noreferrer"
                >
                  Contact Us!
                </a>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Container>
    </SelectedPlanContext.Provider>
  );
}

type PlanCardProps = {
  planType: PlanType;
  highlighted?: boolean;
  purchaseEnabled?: boolean;
  children: ReactNode | ReactNode[];
  onClick?: () => void;
};

function PlanCard({
  planType,
  highlighted = false,
  purchaseEnabled = true,
  children,
  onClick,
}: PlanCardProps) {
  const {
    billingCycle,
  } = useSelectedPlan();
  const {subtitle} = plans[planType][billingCycle];
  return (
    <Card className={cx('card-pricing border-0 text-center px-3', {'bg-primary': highlighted})}>
      <Card.Header className="border-0 delimiter-bottom">
        <PriceTitle planType={planType} highlighted={highlighted} />
      </Card.Header>
      <Card.Body>
        {subtitle && (
          <p className={`mb-2 ${styles.subtitle}`}>
            <mark>{subtitle}</mark>
          </p>
        )}
        {children}
        {purchaseEnabled && (
        <PurchaseButton
          planType={planType}
          highlighted={highlighted}
          onClick={onClick}
        />
        )}
      </Card.Body>
    </Card>
  );
}

type BillingCycleToggleProps = {
  size?: 'sm' | 'lg';
};

function BillingCycleToggle({size}: BillingCycleToggleProps) {
  return (
    <SelectedPlanContext.Consumer>
      {({
        billingCycle,
        setBillingCycle,
      }) => (
        <Row className={`text-center mb-1 ${styles.billingCycleToggle}`}>
          <Col>
            <ToggleButtonGroup
              name="billing"
              type="radio"
              value={billingCycle}
              aria-label="Billing cycle"
              onChange={setBillingCycle}
              size={size}
            >
              <ToggleButton
                id="monthly"
                variant={billingCycle === 'monthly' ? 'primary' : 'soft-primary'}
                value="monthly"
              >
                Monthly
              </ToggleButton>
              <ToggleButton
                id="yearly"
                variant={billingCycle === 'yearly' ? 'primary' : 'soft-primary'}
                value="yearly"
              >
                Yearly
                <Badge
                  className="badge-floating badge-pill"
                  variant="success"
                >
                  -25%
                </Badge>
              </ToggleButton>
            </ToggleButtonGroup>
          </Col>
        </Row>
      )}
    </SelectedPlanContext.Consumer>
  );
}

type PurchaseButtonProps = {
  planType: PlanType;
  highlighted?: boolean;
  onClick?: () => void;
};

function PurchaseButton({
  planType,
  highlighted = false,
  onClick,
}: PurchaseButtonProps) {
  const {
    billingCycle,
  } = useSelectedPlan();
  const [submitting, setSubmitting] = useState(false);
  const {trackComponentEvent} = useTracking();
  const plan = plans[planType][billingCycle];
  const {
    callToAction,
    priceId,
  } = plan;

  async function handleClick() {
    setSubmitting(true);
    trackComponentEvent('PurchaseButton', 'Checkout Start', {
      planType,
      billingCycle,
      priceId,
    });
    if (onClick) {
      onClick();
    } else {
      await redirectToStripeCheckoutSession({
        priceIds: [priceId],
      });
    }
    setSubmitting(false);
  }

  const variant = highlighted ? 'white' : 'warning';
  return (
    <Button
      variant={variant}
      className="hover-translate-y-n3 hover-shadow-lg mb-3"
      onClick={handleClick}
      disabled={submitting}
    >
      {callToAction}
    </Button>
  );
}

type PriceTitleProps = {
  planType: PlanType;
  highlighted?: boolean;
};

function PriceTitle({
  planType,
  highlighted = false,
}: PriceTitleProps) {
  const {
    billingCycle,
  } = useSelectedPlan();
  const sku = plans[planType][billingCycle];
  return (
    <>
      <h1 className={cx('h6', {
        'text-white': highlighted,
        'text-muted': !highlighted,
      })}
      >
        {sku.title}
      </h1>
      <div className={cx('h1 text-center mb-0', {'text-white': highlighted})}>
        $
        <span className="font-weight-bolder">
          {sku.price}
        </span>
      </div>
      <span className={cx('text-sm', {'text-white': highlighted})}>
        {sku.billingInfo}
      </span>
    </>
  );
}
