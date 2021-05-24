import React, {Children, ReactNode} from 'react';
import ReactIs from 'react-is';
import {Section} from './Section';
import NavBar from '../nav/NavBar';

/**
 * A page component that will render a [NavBar] at the top.
 * [children] can only be [Section]s.
 */
export default function Page({children} : {children: ReactNode | ReactNode[]}) {
  Children.forEach(children, (child) => {
    if (!ReactIs.isElement(child) || child.type !== Section) {
      throw Error('All children of Page must be Sections');
    }
  });
  return (
    <>
      <header className="pb-4">
        <NavBar />
      </header>
      <div className="slice slice-sm bg-section-secondary">
        <div className="container">
          <div className="row justify-content-center">
            <div className="col-lg-12">
              {children}
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
