import {differenceInMilliseconds,
  differenceInSeconds,
  format,
  formatRelative,
  intervalToDuration} from 'date-fns';

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
  const start = new Date(startInMillis);
  const end = new Date(endInMillis);
  if (differenceInSeconds(end, start) === 0 && differenceInMilliseconds(end, start) >= 0) {
    return '< 1 sec';
  }

  const duration = intervalToDuration({
    start: startInMillis,
    end: endInMillis,
  });
  const result = [];
  if (duration.years) {
    result.push(`${duration.years} years`);
  }
  if (duration.months) {
    result.push(`${duration.months} months`);
  }
  if (duration.weeks) {
    result.push(`${duration.weeks} weeks`);
  }
  if (duration.days) {
    result.push(`${duration.weeks} days`);
  }
  if (duration.hours) {
    result.push(`${duration.hours} hrs`);
  }
  if (duration.minutes) {
    result.push(`${duration.minutes} min`);
  }
  if (duration.seconds) {
    result.push(`${duration.seconds} sec`);
  }
  return result.join(', ');
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
 * Formats a [timestampInMillis] with format `MM/dd/yy hh:mm:ss a`.
 *
 * Output example `01/04/21 06:10:12 AM`.
 */
export function formatDateTimeMed(timestampInMillis: number) {
  return format(timestampInMillis, 'MM/dd/yy hh:mm:ss a');
}

/**
 * Formats a [timestampInMillis] as relative to [Date.now()].
 *
 * Output example: `today at 11:03 AM`
 */
export function formatDateTimeRelativeToNow(timestampInMillis: number) {
  return formatRelative(timestampInMillis, Date.now());
}
