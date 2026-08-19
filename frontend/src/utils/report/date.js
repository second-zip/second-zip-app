const isValidDateParts = (year, month, day) => {
  if (![year, month, day].every(Number.isInteger)) return false;

  const date = new Date(year, month - 1, day);

  return (
    date.getFullYear() === year &&
    date.getMonth() === month - 1 &&
    date.getDate() === day
  );
};

const getReportDateParts = (value) => {
  if (!value) return null;

  if (Array.isArray(value)) {
    const [year, month, day] = value.map(Number);

    return isValidDateParts(year, month, day) ? { year, month, day } : null;
  }

  if (typeof value === 'object' && !(value instanceof Date)) {
    const year = Number(value.year);
    const month = Number(value.monthValue ?? value.month);
    const day = Number(value.dayOfMonth ?? value.day);

    return isValidDateParts(year, month, day) ? { year, month, day } : null;
  }

  if (typeof value === 'string') {
    const match = value.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/);

    if (match) {
      const [, yearValue, monthValue, dayValue] = match;
      const year = Number(yearValue);
      const month = Number(monthValue);
      const day = Number(dayValue);

      return isValidDateParts(year, month, day) ? { year, month, day } : null;
    }
  }

  const parsedDate = new Date(value);

  if (Number.isNaN(parsedDate.getTime())) return null;

  return {
    year: parsedDate.getFullYear(),
    month: parsedDate.getMonth() + 1,
    day: parsedDate.getDate(),
  };
};

export const formatReportDate = (value) => {
  const date = getReportDateParts(value);

  return date ? `${date.year}. ${date.month}. ${date.day}` : '-';
};

export const toReportDateTime = (value) => {
  const date = getReportDateParts(value);

  if (!date) return undefined;

  const month = String(date.month).padStart(2, '0');
  const day = String(date.day).padStart(2, '0');

  return `${date.year}-${month}-${day}`;
};

export const toReportTimestamp = (value) => {
  if (!value) return 0;

  let timestamp;

  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] =
      value.map(Number);

    timestamp = new Date(
      year,
      month - 1,
      day,
      hour,
      minute,
      second,
    ).getTime();
  } else if (typeof value === 'object' && !(value instanceof Date)) {
    timestamp = new Date(
      Number(value.year),
      Number(value.monthValue ?? value.month) - 1,
      Number(value.dayOfMonth ?? value.day),
      Number(value.hour ?? 0),
      Number(value.minute ?? 0),
      Number(value.second ?? 0),
    ).getTime();
  } else {
    timestamp = new Date(value).getTime();
  }

  return Number.isNaN(timestamp) ? 0 : timestamp;
};
