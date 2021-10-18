import {NAME_NOT_SPECIFIED} from 'hooks/useAuth';
import React from 'react';

type UserNameProps = {
  name: string;
  placeholderValue?: string;
  className?: string;
  placeholderClassName?: string;
};

/**
 * Renders {@link name} if it is specified, otherwise renders {@link placeholderValue} if it is
 * specified, otherwise renders `null`.
 */
export function UserName({
  name,
  placeholderValue,
  className,
  placeholderClassName,
}: UserNameProps) {
  if (name === NAME_NOT_SPECIFIED) {
    if (placeholderValue) {
      return (<span className={placeholderClassName}>{placeholderValue}</span>);
    }
    return null;
  }
  return <span className={className}>user.name</span>;
}
