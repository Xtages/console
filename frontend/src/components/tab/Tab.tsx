import React, {Children, ReactElement, ReactNode, useState} from 'react';
import ReactIs from 'react-is';
import cx from 'classnames';
import styles from './Tab.module.scss';

export interface TabProps {
  /** Id for the tab, most be unique amongst tabs. */
  id: string;

  /** Whether the tab is initially active. Only one tab may be active at a time */
  active?: boolean;

  /** The title for the tab */
  title: ReactNode | string;

  /** Tab content */
  children: ReactNode | ReactNode[];
}

export function Tab({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  id,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  active = false,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  title,
  children,
}: TabProps) {
  return (
    <>
      {children}
    </>
  );
}

export interface TabsProps {
  children: ReactElement | ReactElement[];
}

export function Tabs({children}: TabsProps) {
  const childArray = ensureAllChildrenAreTabs(children);
  ensureAtMostThereIsOnlyOneActiveTab(childArray);
  ensureAllTabsHaveUniqueIds(childArray);
  // Assume that the first tab is active
  const initialActiveTabId = childArray
    .filter((tab) => tab.props.active)
    ?.shift()?.props?.id || childArray[0].props.id;
  const [activeTabId, setActiveTabId] = useState(initialActiveTabId);
  return (
    <>
      <ul className={`nav nav-tabs ${styles.navTabs}`} role="tablist">
        {Children.map(children, (tab) => {
          const isActive = tab.props.id === activeTabId;
          return (
            <li className={`nav-item ${styles.navItem}`} role="presentation">
              <a
                className={cx('nav-link', styles.navLink, {
                  active: isActive,
                  [`${styles.active}`]: isActive,
                })}
                id={`${tab.props.id}-tab`}
                href={`#${tab.props.id}`}
                role="tab"
                aria-controls={tab.props.id}
                onClick={(e) => {
                  e.preventDefault();
                  setActiveTabId(tab.props.id);
                }}
              >
                {tab.props.title}
              </a>
            </li>
          );
        })}
      </ul>
      <div className={`tab-content ${styles.tabContent}`}>
        {Children.map(children, (tab) => {
          const isActive = tab.props.id === activeTabId;
          return (
            <div
              className={cx('tab-pane', 'fade', {
                active: isActive,
                show: isActive,
                [`${styles.active}`]: isActive,
              })}
              id={tab.props.id}
              role="tabpanel"
              aria-labelledby="$id-tab"
            >
              {tab}
            </div>
          );
        })}
      </div>
    </>
  );
}

function ensureAllChildrenAreTabs(children: ReactNode | ReactNode[]): ReactElement[] {
  const array = Children.toArray(children);
  if (!array.every((tab) => ReactIs.isElement(tab) && tab.type === Tab)) {
    throw Error('Children of Tabs must of type Tab');
  }
  return array as ReactElement[];
}

function ensureAtMostThereIsOnlyOneActiveTab(childArray: ReactElement[]) {
  if (childArray.map((tab) => tab.props.active)
    .filter((active) => active).length > 1) {
    throw Error('At most one Tab can be active');
  }
}

function ensureAllTabsHaveUniqueIds(childArray: ReactElement[]) {
  // If the size of the set of tab ids is != from the length of tabs then there are duplicate ids.
  if ((new Set(childArray.map((tab) => tab.props.id)).size !== childArray.length)
  ) {
    throw Error('All tabs must have an unique id set');
  }
}
