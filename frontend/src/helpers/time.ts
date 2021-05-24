import {format, formatDuration, formatRelative, intervalToDuration} from 'date-fns';

/**
 * Takes two timestamps in milliseconds and formats them using [formatDuration]. If [endInMillis]
 * is `null` or `undefined` [defaultWhenNoEnd] will returned instead.
 */
export function durationString({
  startInMillis,
  endInMillis,
  defaultWhenNoEnd = '',
}: {
  startInMillis: number,
  endInMillis: number | undefined | null,
  defaultWhenNoEnd?: string,
}) {
  if (!endInMillis) {
    return defaultWhenNoEnd;
  }
  return formatDuration(intervalToDuration({
    start: startInMillis,
    end: endInMillis,
  }));
}

/**
 * Formats a [timestampInMillis] with format `MMM d, yyyy hh:mm:ss a`.
 *
 * Output example `Jan 4, 2021 06:10:12 AM`.
 */
export function formatDateTimeFull(timestampInMillis: number) {
  return format(timestampInMillis, 'MMM d, yyyy hh:mm:ss a');
}

/**
 * Formats a [timestampInMillis] as relative to [Date.now()].
 *
 * Output example: `today at 11:03 AM`
 */
export function formatDateTimeRelativeToNow(timestampInMillis: number) {
  return formatRelative(timestampInMillis, Date.now());
}
