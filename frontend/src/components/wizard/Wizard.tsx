import React, {createContext, ReactNode, useContext} from 'react';
import {Accordion, AccordionContext, Card} from 'react-bootstrap';
import cx from 'classnames';
import {Check} from 'react-feather';
import {Undefinable} from 'types/nullable';
import styles from './Wizard.module.scss';

function makeStepEventKey(step: number | undefined) {
  return typeof step !== 'undefined' ? `step-${step}` : undefined;
}

const WizardContext = createContext<Undefinable<string>>('');

export type WizardProps = {
  children: ReactNode[];
  currentStep?: number;
};

export function Wizard({
  children,
  currentStep,
}: WizardProps) {
  const currentStepDefaultActiveKey = makeStepEventKey(currentStep);

  return (
    <Accordion
      defaultActiveKey={currentStepDefaultActiveKey}
      className={cx('col-12', styles.wizard)}
    >
      <WizardContext.Provider value={currentStepDefaultActiveKey}>
        {children}
      </WizardContext.Provider>
    </Accordion>
  );
}

export type WizardStepProps = {
  step: number;
  title: Exclude<ReactNode, undefined | null>;
  children: ReactNode;
  completed?: boolean;
};

export function WizardStep({
  step,
  title,
  children,
  completed = false,
}: WizardStepProps) {
  const currentEventKey = useContext(AccordionContext);
  const defaultEventKey = useContext(WizardContext);
  const eventKey = makeStepEventKey(step)!!;
  const toggleId = `${eventKey}-toggle`;
  const collapseId = `${eventKey}-collapse`;
  const isCurrentEventKey = currentEventKey === eventKey;
  const isCurrentDefaultEventKey = defaultEventKey === eventKey;
  return (
    <Card className="card-compact">
      <Accordion.Toggle
        id={toggleId}
        as={Card.Header}
        eventKey={eventKey}
        aria-expanded={isCurrentEventKey}
        aria-controls={collapseId}
      >
        <span className={cx('badge badge-lg badge-circle rounded-circle mx-2', {
          'badge-dark': !completed && !isCurrentDefaultEventKey,
          'badge-success': completed && !isCurrentDefaultEventKey,
          'badge-warning': isCurrentDefaultEventKey,
        })}
        >
          {completed ? <Check size="1.5em" /> : step}
        </span>
        {title}
      </Accordion.Toggle>
      <Accordion.Collapse
        id={collapseId}
        eventKey={eventKey}
        className={styles.stepBody}
        aria-disabled={completed}
        aria-labelledby={toggleId}
      >
        <Card.Body className="position-relative">
          {completed && <div className={styles.overlay} />}
          {children}
        </Card.Body>
      </Accordion.Collapse>
    </Card>
  );
}
