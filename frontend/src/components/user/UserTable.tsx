import React from 'react';
import {User} from 'gen/api';
import {UserName} from 'components/user/UserName';

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
              <UserName
                name={user.name}
                className="h6 mb-0 text-sm"
                placeholderValue="Not provided"
                placeholderClassName="h6 mb-0 text-sm font-italic text-muted"
              />
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
