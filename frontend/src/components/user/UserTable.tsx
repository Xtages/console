import React from 'react';
import {User} from 'gen/api';
import {NAME_NOT_SPECIFIED} from 'hooks/useAuth';

export type UserTableProps = {
  users: User[],
};

/**
 * Table listing users.
 */
export function UserTable({users}: UserTableProps) {
  return (
    <table className="table table-cards align-items-center">
      <thead>
        <tr>
          <th scope="col" style={{maxWidth: '300px'}}>Name</th>
          <th scope="col">Email</th>
          <th scope="col" style={{maxWidth: '200px'}}>Status</th>
        </tr>
      </thead>
      <tbody>
        {users.map((user) => (
          <tr key={user.id}>
            <th scope="row">
              <span className="h6 mb-0 text-sm"><UserName user={user} /></span>
            </th>
            <td>
              <a href={`mailto:${user.username}`} target="_blank" rel="noreferrer">{user.username}</a>
            </td>
            <td className="text-capitalize">
              {user.status.toLowerCase()}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function UserName({user} : {user: User}) {
  if (user.name === NAME_NOT_SPECIFIED) {
    return (<span className="font-italic text-muted">Not provided</span>);
  }
  return <>user.name</>;
}
