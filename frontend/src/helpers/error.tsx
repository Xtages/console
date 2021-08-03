import React, {Component, ErrorInfo, ReactNode} from 'react';
import {buildAnalytics} from 'service/AnalyticsService';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {hasError: false};
  }

  public static getDerivedStateFromError(ignoreError: Error): State {
    return {hasError: true};
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    const analytics = buildAnalytics();
    analytics.track('Uncaught error', {
      ...error,
      ...errorInfo,
    }).then(() => null);
  }

  public render() {
    const {hasError} = this.state;
    if (hasError) {
      return (
        <div className="text-center">
          <h1>An unexpected error occurred. Please reload the page.</h1>
        </div>
      );
    }

    const {props} = this;
    return props.children;
  }
}
