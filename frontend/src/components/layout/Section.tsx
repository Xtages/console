import React, {Children, FC, ReactNode} from 'react';
import * as ReactIs from 'react-is';
import {IconProps} from 'react-feather';
import cx from 'classnames';
import {UseQueryLoaderElement, UseQueryLoaderElementProps} from 'components/layout/UseQueryLoaderElement';

interface SectionTitleProps {
  /** Optional Icon for the section */
  icon?: FC<IconProps>;

  /** Title of the section */
  title: ReactNode;

  /** Optional subtible for the section */
  subtitle?: ReactNode;

  /** Whether to user a smaller font */
  small?: boolean;
}

/** A [Section] title. Only one may appear per section. */
export function SectionTitle({
  icon,
  title,
  subtitle,
  small = false,
}: SectionTitleProps) {
  const iconEl = icon && React.createElement(icon);
  return (
    <div className="row mx-n2">
      <div className="d-flex align-items-center mb-4">
        <div className="d-flex">
          {iconEl
                && (
                <div className={cx({h4: !small, h5: small}, 'mb-0')}>
                  {iconEl}
                </div>
                )}
          <div className="col">
            {small && <h2 className="h5 mb-0">{title}</h2>}
            {!small && <h1 className="h4 mb-0">{title}</h1>}
            {subtitle
                    && (
                    <p className="text-muted mb-0">
                      {subtitle}
                    </p>
                    )}
          </div>
        </div>
      </div>
    </div>
  );
}

interface SectionProps {
  /**
   * Children for the section. Only 0 or 1 [SectionTitle] may appear here.
   * The rest of the children will will be wrapped in a `<div class="row">`.
   * */
  children: ReactNode | ReactNode[];

  last?: boolean;

  className?: string;
}

/** A section in a page */
export function Section({
  children,
  className = '',
  last = false,
}: SectionProps) {
  let sectionTitle: ReactNode | undefined;
  const restOfChildren: ReactNode[] = [];
  Children.forEach(children, (child) => {
    if (ReactIs.isElement(child) && child.type === SectionTitle) {
      if (!sectionTitle) {
        sectionTitle = child;
      } else {
        throw Error('At most one SectionTitle can be present');
      }
    } else {
      restOfChildren.push(child);
    }
  });
  return (
    <>
      {sectionTitle}
      <div className={`row mx-n2 ${className}`}>
        {restOfChildren}
      </div>
      {!last
      && <hr />}
    </>
  );
}

export type LoadIndicatingSectionProps<D = unknown, E = unknown> =
    UseQueryLoaderElementProps<D, E> & SectionProps;

/**
 * A {@link Section} that will take an {@link useQuery} result and consistently show a loading
 * indicator, or error message or finally call {@link children} with the result of the operation.
 */
export function LoadIndicatingSection<D = unknown, E = unknown>({
  queryResult,
  children,
  errorHandler = undefined,
  ...props
}: LoadIndicatingSectionProps<D, E>) {
  return (
    <UseQueryLoaderElement queryResult={queryResult} errorHandler={errorHandler}>
      {(axiosResponse) => (
        <Section {...props}>{children(axiosResponse)}</Section>
      )}
    </UseQueryLoaderElement>
  );
}
