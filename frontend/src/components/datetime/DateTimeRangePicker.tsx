import React, {ChangeEvent, useState} from 'react';
import {Col, Form} from 'react-bootstrap';
import {isAfter, isBefore, isEqual, lightFormat, set} from 'date-fns';
import {ChevronsLeft, ChevronsRight, Minus} from 'react-feather';
import styles from './DateTimeRangePicker.module.scss';

/**
 * A range of date times. Both limits are inclusive and can be the same.
 */
export type DateTimeRange = {
  startDateTime: Date,
  endDateTime: Date,
};

export type DateTimeRangePickerProps = DateTimeRange & {
  onDateTimeRangeChange?: (dateTimeRange: DateTimeRange) => void;
};

/**
 * Renders a DateTime range picker. {@link DateTimeRangePickerProps.startDateTime} and
 * {@link DateTimeRangePickerProps.endDateTime} might be the same date or `startDateTime` must be
 * before `endDateTime`.
 */
export function DateTimeRangePicker({
  endDateTime,
  startDateTime,
  onDateTimeRangeChange,
}: DateTimeRangePickerProps) {
  const [dateRange, setDateRange] = useState<DateTimeRange>({startDateTime, endDateTime});
  const [prevValue, setPrevValue] = useState<DateTimeRange>({startDateTime, endDateTime});

  if (prevValue.startDateTime !== startDateTime || prevValue.endDateTime !== endDateTime) {
    setDateRange({startDateTime, endDateTime});
    setPrevValue({startDateTime, endDateTime});
  }

  function handleStartDateChange(e: ChangeEvent<HTMLInputElement>) {
    const newStartDate = setDateOnDateTime(dateRange.startDateTime, e.target.value);
    if (isBeforeOrEqual(newStartDate, dateRange.endDateTime)) {
      changeDateRange({
        startDateTime: newStartDate,
        endDateTime: dateRange.endDateTime,
      });
    }
  }

  function handleEndDateChange(e: ChangeEvent<HTMLInputElement>) {
    const newEndDate = setDateOnDateTime(dateRange.endDateTime, e.target.value);
    if (isAfterOrEqual(newEndDate, dateRange.startDateTime)) {
      changeDateRange({
        startDateTime: dateRange.startDateTime,
        endDateTime: newEndDate,
      });
    }
  }

  function handleStartTimeChange(e: ChangeEvent<HTMLInputElement>) {
    const newStartDate = setTimeOnDateTime(dateRange.startDateTime, e.target.value);
    if (isBeforeOrEqual(newStartDate, dateRange.endDateTime)) {
      changeDateRange({
        startDateTime: newStartDate,
        endDateTime: dateRange.endDateTime,
      });
    }
  }

  function handleEndTimeChange(e: ChangeEvent<HTMLInputElement>) {
    const newEndDate = setTimeOnDateTime(dateRange.endDateTime, e.target.value);
    if (isAfterOrEqual(newEndDate, dateRange.startDateTime)) {
      changeDateRange({
        startDateTime: dateRange.startDateTime,
        endDateTime: newEndDate,
      });
    }
  }

  function changeDateRange(newDateRange: DateTimeRange) {
    setDateRange(newDateRange);
    if (onDateTimeRangeChange) {
      onDateTimeRangeChange(newDateRange);
    }
  }

  return (
    <Form.Row className={styles.dateRangePicker}>
      <Col sm="auto" className="form-inline">
        <Form.Control
          className="form-control-xs mr-1"
          type="date"
          name="startDate"
          value={formatDate(dateRange.startDateTime)}
          max={formatDate(dateRange.endDateTime)}
          min="2021-01-01"
          onChange={handleStartDateChange}
        />
        <Form.Control
          className="form-control-xs"
          type="time"
          name="startTime"
          value={formatTime(dateRange.startDateTime)}
          onChange={handleStartTimeChange}
        />
      </Col>
      <Col sm="auto">
        <ChevronsLeft size="1.1em" />
        <Minus size="1.1em" />
        <ChevronsRight size="1.1em" />
      </Col>
      <Col sm="auto" className="form-inline">
        <Form.Control
          className="form-control-xs mr-1"
          type="date"
          name="endDate"
          value={formatDate(dateRange.endDateTime)}
          max={formatDate(dateRange.endDateTime)}
          min={formatDate(dateRange.startDateTime)}
          onChange={handleEndDateChange}
        />
        <Form.Control
          className="form-control-xs"
          type="time"
          name="endTime"
          value={formatTime(dateRange.endDateTime)}
          onChange={handleEndTimeChange}
        />
      </Col>
    </Form.Row>
  );
}

function formatTime(date: Date) {
  return lightFormat(date, 'HH:mm');
}

function formatDate(date: Date) {
  return lightFormat(date, 'yyyy-MM-dd');
}

function setTimeOnDateTime(dateTime: Date, time: string) {
  const timeParts = time.split(':').map(parseIntDecimal);
  return set(dateTime, {
    hours: timeParts[0],
    minutes: timeParts[1],
  });
}

function setDateOnDateTime(dateTime: Date, date: string) {
  const dateParts = date.split('-').map(parseIntDecimal);
  return set(dateTime, {
    year: dateParts[0],
    month: dateParts[1] - 1, // in js months start at 0
    date: dateParts[2],
  });
}

const isBeforeOrEqual = (
  date: Date,
  dateToCompare: Date,
) => isEqual(date, dateToCompare) || isBefore(date, dateToCompare);

const isAfterOrEqual = (
  date: Date,
  dateToCompare: Date,
) => isEqual(date, dateToCompare) || isAfter(date, dateToCompare);

const parseIntDecimal = (str: string) => Number.parseInt(str, 10);
