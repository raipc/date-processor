package com.axibase.date;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import static com.axibase.date.DatetimeProcessorUtil.toMillis;

class DatetimeProcessorIso8601 implements DatetimeProcessor {
    private final int fractionsOfSecond;
    private final ZoneOffsetType zoneOffsetType;
    private final ZoneId localZoneId = ZoneId.systemDefault();

    DatetimeProcessorIso8601(int fractionsOfSecond, ZoneOffsetType zoneOffsetType) {
        this.fractionsOfSecond = fractionsOfSecond;
        this.zoneOffsetType = zoneOffsetType;
    }

    @Override
    public long parseMillis(String datetime) {
        return DatetimeProcessorUtil.parseIso8601AsOffsetDateTime(datetime, 'T').toInstant().toEpochMilli();
    }

    @Override
    public long parseMillis(String datetime, ZoneId zoneId) {
        return toMillis(DatetimeProcessorUtil.parseIso8601AsZonedDateTime(datetime, 'T', zoneId, zoneOffsetType));
    }

    @Override
    public ZonedDateTime parse(String datetime) {
        return DatetimeProcessorUtil.parseIso8601AsZonedDateTime(datetime, 'T', localZoneId, zoneOffsetType);
    }

    @Override
    public ZonedDateTime parse(String datetime, ZoneId zoneId) {
        return DatetimeProcessorUtil.parseIso8601AsZonedDateTime(datetime, 'T', zoneId, zoneOffsetType);
    }

    @Override
    public String print(long timestamp) {
        return DatetimeProcessorUtil.printIso8601(timestamp, 'T', localZoneId, zoneOffsetType, fractionsOfSecond);
    }

    @Override
    public String print(long timestamp, ZoneId zoneId) {
        return DatetimeProcessorUtil.printIso8601(timestamp, 'T', zoneId, zoneOffsetType, fractionsOfSecond);
    }

    @Override
    public DatetimeProcessor withLocale(Locale locale) {
        return this;
    }

    @Override
    public boolean canParse(String date) {
        return DatetimeProcessorUtil.checkExpectedMilliseconds(date, fractionsOfSecond) && DatetimeProcessor.super.canParse(date);
    }
}
