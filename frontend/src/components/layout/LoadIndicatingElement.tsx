import {UseQueryResult} from 'react-query/types/react/types';
import React, {ReactNode} from 'react';
import {Spinner} from 'react-bootstrap';

export type LoadIndicatingElementProps<D = unknown, E = unknown> = {
  /** The result from an {@link useQuery} call. */
  queryResult: UseQueryResult<D, E>;

  /**
   * A function that takes {@link T} (ie {@link AxiosResponse}) and returns a
   * {@link ReactNode}.
   */
  children: ((axiosResponse: D, errorHandled: boolean) => ReactNode);

  /**
   * A function that if passed, will return a {@link ReactNode} to replace the default error
   * message rendered by {@link LoadingIndicatingElement}. If it returns `undefined` the default
   * error will be rendered.
   */
  errorHandler?: ((error: E) => ReactNode);
};

/**
 * A {@link Section} that will take an {@link useQuery} result and consistently show a loading
 * indicator, or error message or finally call {@link children} with the result of the operation.
 */
export function LoadIndicatingElement<D = unknown, E = unknown>({
  queryResult,
  children,
  errorHandler = undefined,
}: LoadIndicatingElementProps<D, E>) {
  const {
    isLoading,
    error,
  } = queryResult;

  let errorHandled = false;
  let content: string | ReactNode;
  if (isLoading) {
    content = (
      <div className="mx-auto py-5">
        <Spinner animation="grow" role="status" variant="dark-secondary">
          <span className="sr-only">Loading...</span>
        </Spinner>
      </div>
    );
  } else if (error) {
    if (errorHandler) {
      content = errorHandler(error);
      if (content) {
        errorHandled = true;
      } else {
        content = `An error has occurred: ${error}`;
      }
    }
  } else {
    content = children(queryResult.data!!, errorHandled);
  }
  return (<>content</>);
}
