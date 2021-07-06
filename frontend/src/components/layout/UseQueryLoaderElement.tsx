import {UseInfiniteQueryResult, UseQueryResult} from 'react-query/types/react/types';
import React, {ReactNode} from 'react';
import {Spinner} from 'react-bootstrap';
import {InfiniteData} from 'react-query';

export type UseQueryLoaderElementProps<D = unknown, E = unknown> = {
  /** The result from an {@link useQuery} call. */
  queryResult: UseQueryResult<D, E>;

  /**
   * A function that takes {@link T} (ie {@link AxiosResponse}) and returns a
   * {@link ReactNode}.
   */
  children: ((axiosResponse: D) => ReactNode);

  /**
   * A function that if passed, will return a {@link ReactNode} to replace the default error
   * message rendered by {@link UseQueryLoaderElement}. If it returns `undefined` the default
   * error will be rendered.
   */
  errorHandler?: ((error: E) => ReactNode);
};

/**
 * An element that will take an {@link useQuery} result and consistently show a loading
 * indicator, or error message or finally call {@link children} with the result of the operation.
 */
export function UseQueryLoaderElement<D = unknown, E = unknown>({
  queryResult,
  children,
  errorHandler = undefined,
}: UseQueryLoaderElementProps<D, E>) {
  const {
    isLoading,
    isIdle,
    isError,
    error,
  } = queryResult;

  let content: string | ReactNode;
  if (isLoading) {
    content = (
      <div className="mx-auto py-5">
        <Spinner animation="grow" role="status" variant="dark-secondary">
          <span className="sr-only">Loading...</span>
        </Spinner>
      </div>
    );
  } else if (isIdle) {
    content = '';
  } else if (isError) {
    if (errorHandler) {
      content = errorHandler(error!!);
      if (!content) {
        content = `An error has occurred: ${error}`;
      }
    } else {
      content = `An error has occurred: ${error}`;
    }
  } else {
    content = children(queryResult.data!!);
  }
  return <>{content}</>;
}

export type UseInfiniteQueryLoaderElementProps<D = unknown, E = unknown> = {
  /** The result from an {@link useInfiniteQuery} call. */
  queryResult: UseInfiniteQueryResult<D, E>;

  /**
   * A function that takes an {@link InfiniteData<D>} (ie {@link InfiniteData<AxiosResponse>}) and
   * returns a {@link ReactNode}.
   */
  children: ((axiosResponse: InfiniteData<D>) => ReactNode);

  /**
   * A function that if passed, will return a {@link ReactNode} to replace the default error
   * message rendered by {@link UseInfiniteQueryLoaderElement}. If it returns `undefined` the
   * default error will be rendered.
   */
  errorHandler?: ((error: E) => ReactNode);
};

/**
 * An element that will take an {@link useInfiniteQuery} result and consistently show a
 * loading indicator, or error message or finally call {@link children} with the result of the
 * operation.
 */
export function UseInfiniteQueryLoaderElement<D = unknown, E = unknown>({
  queryResult,
  children,
  errorHandler = undefined,
}: UseInfiniteQueryLoaderElementProps<D, E>) {
  const {
    isLoading,
    isIdle,
    isError,
    error,
  } = queryResult;

  let content: string | ReactNode;
  if (isLoading) {
    content = (
      <div className="mx-auto py-5">
        <Spinner animation="grow" role="status" variant="dark-secondary">
          <span className="sr-only">Loading...</span>
        </Spinner>
      </div>
    );
  } else if (isIdle) {
    content = '';
  } else if (isError) {
    if (errorHandler) {
      content = errorHandler(error!!);
      if (!content) {
        content = `An error has occurred: ${error}`;
      }
    } else {
      content = `An error has occurred: ${error}`;
    }
  } else {
    content = children(queryResult.data!!);
  }
  return <>{content}</>;
}
