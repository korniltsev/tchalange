package ru.korniltsev.telegram.core.app;

import android.content.Context;
import android.text.format.DateFormat;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Locale;

public class Formatters {
    public final ThreadLocal<PeriodFormatter> DURATION_FORMATTER = new ThreadLocal<PeriodFormatter>(){
        @Override
        protected PeriodFormatter initialValue() {
            return new PeriodFormatterBuilder()
                    .printZeroAlways()
                    .minimumPrintedDigits(1).appendMinutes()
                    .appendSeparator(":")
                    .minimumPrintedDigits(2).printZeroAlways()
                    .appendSeconds()
                    .toFormatter();
        }
    };

    public final ThreadLocal<DateTimeFormatter> TIME_FORMATTER = new ThreadLocal<DateTimeFormatter>(){
        @Override
        protected DateTimeFormatter initialValue() {
            return DateTimeFormat.forPattern(hour24 ? "HH:mm" : "h:mm a")
                    .withLocale(Locale.US);
        }
    };

    public final ThreadLocal<DateTimeFormatter> DATE_FORMATTER = new ThreadLocal<DateTimeFormatter>(){
        @Override
        protected DateTimeFormatter initialValue() {
            return DateTimeFormat.forPattern("dd.MM.yy")
                    .withLocale(Locale.US);

        }
    };

    final Context appCtx;
    final boolean hour24;
    public Formatters(Context appCtx) {
        this.appCtx = appCtx;
        hour24 = DateFormat.is24HourFormat(appCtx);

    }
}
