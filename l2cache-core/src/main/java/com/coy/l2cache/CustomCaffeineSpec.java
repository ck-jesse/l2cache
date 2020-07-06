package com.coy.l2cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.errorprone.annotations.FormatMethod;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * 扩展原生的CaffeineSpec
 *
 * @author chenck
 * @date 2020/5/12 19:41
 */
public class CustomCaffeineSpec {

    enum Strength {WEAK, SOFT}

    public static final int UNSET_INT = -1;

    static final String SPLIT_OPTIONS = ",";
    static final String SPLIT_KEY_VALUE = "=";

    final String specification;

    int initialCapacity = UNSET_INT;
    long maximumWeight = UNSET_INT;
    long maximumSize = UNSET_INT;
    boolean recordStats;

    @Nullable Strength keyStrength;
    @Nullable Strength valueStrength;

    long expireAfterAccessDuration = UNSET_INT;
    @Nullable TimeUnit expireAfterAccessTimeUnit;

    long expireAfterWriteDuration = UNSET_INT;
    @Nullable TimeUnit expireAfterWriteTimeUnit;

    long refreshAfterWriteDuration = UNSET_INT;
    @Nullable TimeUnit refreshAfterWriteTimeUnit;

    private CustomCaffeineSpec(String specification) {
        this.specification = requireNonNull(specification);
    }

    /**
     * Returns a {@link Caffeine} builder configured according to this specification.
     *
     * @return a builder configured to the specification
     */
    public Caffeine<Object, Object> toBuilder() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (initialCapacity != UNSET_INT) {
            builder.initialCapacity(initialCapacity);
        }
        if (maximumSize != UNSET_INT) {
            builder.maximumSize(maximumSize);
        }
        if (maximumWeight != UNSET_INT) {
            builder.maximumWeight(maximumWeight);
        }
        if (keyStrength != null) {
            requireState(keyStrength == Strength.WEAK);
            builder.weakKeys();
        }
        if (valueStrength != null) {
            if (valueStrength == Strength.WEAK) {
                builder.weakValues();
            } else if (valueStrength == Strength.SOFT) {
                builder.softValues();
            } else {
                throw new IllegalStateException();
            }
        }
        if (expireAfterAccessTimeUnit != null) {
            builder.expireAfterAccess(expireAfterAccessDuration, expireAfterAccessTimeUnit);
        }
        if (expireAfterWriteTimeUnit != null) {
            builder.expireAfterWrite(expireAfterWriteDuration, expireAfterWriteTimeUnit);
        }
        if (refreshAfterWriteTimeUnit != null) {
            builder.refreshAfterWrite(refreshAfterWriteDuration, refreshAfterWriteTimeUnit);
        }
        if (recordStats) {
            builder.recordStats();
        }
        return builder;
    }

    /**
     * Creates a CaffeineSpec from a string.
     *
     * @param specification the string form
     * @return the parsed specification
     */
    @SuppressWarnings("StringSplitter")
    public static @NonNull CustomCaffeineSpec parse(@NonNull String specification) {
        CustomCaffeineSpec spec = new CustomCaffeineSpec(specification);
        for (String option : specification.split(SPLIT_OPTIONS)) {
            spec.parseOption(option.trim());
        }
        return spec;
    }

    /**
     * Parses and applies the configuration option.
     */
    void parseOption(String option) {
        if (option.isEmpty()) {
            return;
        }

        @SuppressWarnings("StringSplitter")
        String[] keyAndValue = option.split(SPLIT_KEY_VALUE);
        requireArgument(keyAndValue.length <= 2,
                "key-value pair %s with more than one equals sign", option);

        String key = keyAndValue[0].trim();
        String value = (keyAndValue.length == 1) ? null : keyAndValue[1].trim();

        configure(key, value);
    }

    /**
     * Configures the setting.
     */
    void configure(String key, @Nullable String value) {
        switch (key) {
            case "initialCapacity":
                initialCapacity(key, value);
                return;
            case "maximumSize":
                maximumSize(key, value);
                return;
            case "maximumWeight":
                maximumWeight(key, value);
                return;
            case "weakKeys":
                weakKeys(value);
                return;
            case "weakValues":
                valueStrength(key, value, Strength.WEAK);
                return;
            case "softValues":
                valueStrength(key, value, Strength.SOFT);
                return;
            case "expireAfterAccess":
                expireAfterAccess(key, value);
                return;
            case "expireAfterWrite":
                expireAfterWrite(key, value);
                return;
            case "refreshAfterWrite":
                refreshAfterWrite(key, value);
                return;
            case "recordStats":
                recordStats(value);
                return;
            default:
                throw new IllegalArgumentException("Unknown key " + key);
        }
    }

    /**
     * Configures the initial capacity.
     */
    void initialCapacity(String key, @Nullable String value) {
        requireArgument(initialCapacity == UNSET_INT,
                "initial capacity was already set to %,d", initialCapacity);
        initialCapacity = parseInt(key, value);
    }

    /**
     * Configures the maximum size.
     */
    void maximumSize(String key, @Nullable String value) {
        requireArgument(maximumSize == UNSET_INT,
                "maximum size was already set to %,d", maximumSize);
        requireArgument(maximumWeight == UNSET_INT,
                "maximum weight was already set to %,d", maximumWeight);
        maximumSize = parseLong(key, value);
    }

    /**
     * Configures the maximum size.
     */
    void maximumWeight(String key, @Nullable String value) {
        requireArgument(maximumWeight == UNSET_INT,
                "maximum weight was already set to %,d", maximumWeight);
        requireArgument(maximumSize == UNSET_INT,
                "maximum size was already set to %,d", maximumSize);
        maximumWeight = parseLong(key, value);
    }

    /**
     * Configures the keys as weak references.
     */
    void weakKeys(@Nullable String value) {
        requireArgument(value == null, "weak keys does not take a value");
        requireArgument(keyStrength == null, "weak keys was already set");
        keyStrength = Strength.WEAK;
    }

    /**
     * Configures the value as weak or soft references.
     */
    void valueStrength(String key, @Nullable String value, Strength strength) {
        requireArgument(value == null, "%s does not take a value", key);
        requireArgument(valueStrength == null, "%s was already set to %s", key, valueStrength);
        valueStrength = strength;
    }

    /**
     * Configures expire after access.
     */
    void expireAfterAccess(String key, @Nullable String value) {
        requireArgument(expireAfterAccessDuration == UNSET_INT, "expireAfterAccess was already set");
        expireAfterAccessDuration = parseDuration(key, value);
        expireAfterAccessTimeUnit = parseTimeUnit(key, value);
    }

    /**
     * Configures expire after write.
     */
    void expireAfterWrite(String key, @Nullable String value) {
        requireArgument(expireAfterWriteDuration == UNSET_INT, "expireAfterWrite was already set");
        expireAfterWriteDuration = parseDuration(key, value);
        expireAfterWriteTimeUnit = parseTimeUnit(key, value);
    }

    /**
     * Configures refresh after write.
     */
    void refreshAfterWrite(String key, @Nullable String value) {
        requireArgument(refreshAfterWriteDuration == UNSET_INT, "refreshAfterWrite was already set");
        refreshAfterWriteDuration = parseDuration(key, value);
        refreshAfterWriteTimeUnit = parseTimeUnit(key, value);
    }

    /**
     * Configures the value as weak or soft references.
     */
    void recordStats(@Nullable String value) {
        requireArgument(value == null, "record stats does not take a value");
        requireArgument(!recordStats, "record stats was already set");
        recordStats = true;
    }

    /**
     * Returns a parsed int value.
     */
    static int parseInt(String key, @Nullable String value) {
        requireArgument((value != null) && !value.isEmpty(), "value of key %s was omitted", key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(
                    "key %s value was set to %s, must be an integer", key, value), e);
        }
    }

    /**
     * Returns a parsed long value.
     */
    static long parseLong(String key, @Nullable String value) {
        requireArgument((value != null) && !value.isEmpty(), "value of key %s was omitted", key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(
                    "key %s value was set to %s, must be a long", key, value), e);
        }
    }

    /**
     * Returns a parsed duration value.
     */
    static long parseDuration(String key, @Nullable String value) {
        requireArgument((value != null) && !value.isEmpty(), "value of key %s omitted", key);
        @SuppressWarnings("NullAway")
        String duration = value.substring(0, value.length() - 1);
        return parseLong(key, duration);
    }

    /**
     * Returns a parsed {@link TimeUnit} value.
     */
    static TimeUnit parseTimeUnit(String key, @Nullable String value) {
        requireArgument((value != null) && !value.isEmpty(), "value of key %s omitted", key);
        @SuppressWarnings("NullAway")
        char lastChar = Character.toLowerCase(value.charAt(value.length() - 1));
        switch (lastChar) {
            case 'd':
                return TimeUnit.DAYS;
            case 'h':
                return TimeUnit.HOURS;
            case 'm':
                return TimeUnit.MINUTES;
            case 's':
                return TimeUnit.SECONDS;
            default:
                throw new IllegalArgumentException(String.format(
                        "key %s invalid format; was %s, must end with one of [dDhHmMsS]", key, value));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof CustomCaffeineSpec)) {
            return false;
        }
        CustomCaffeineSpec spec = (CustomCaffeineSpec) o;
        return Objects.equals(initialCapacity, spec.initialCapacity)
                && Objects.equals(maximumSize, spec.maximumSize)
                && Objects.equals(maximumWeight, spec.maximumWeight)
                && Objects.equals(keyStrength, spec.keyStrength)
                && Objects.equals(valueStrength, spec.valueStrength)
                && Objects.equals(recordStats, spec.recordStats)
                && (durationInNanos(expireAfterAccessDuration, expireAfterAccessTimeUnit) ==
                durationInNanos(spec.expireAfterAccessDuration, spec.expireAfterAccessTimeUnit))
                && (durationInNanos(expireAfterWriteDuration, expireAfterWriteTimeUnit) ==
                durationInNanos(spec.expireAfterWriteDuration, spec.expireAfterWriteTimeUnit))
                && (durationInNanos(refreshAfterWriteDuration, refreshAfterWriteTimeUnit) ==
                durationInNanos(spec.refreshAfterWriteDuration, spec.refreshAfterWriteTimeUnit));
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                initialCapacity, maximumSize, maximumWeight, keyStrength, valueStrength, recordStats,
                durationInNanos(expireAfterAccessDuration, expireAfterAccessTimeUnit),
                durationInNanos(expireAfterWriteDuration, expireAfterWriteTimeUnit),
                durationInNanos(refreshAfterWriteDuration, refreshAfterWriteTimeUnit));
    }

    /**
     * Converts an expiration duration/unit pair into a single long for hashing and equality.
     */
    static long durationInNanos(long duration, @Nullable TimeUnit unit) {
        return (unit == null) ? UNSET_INT : unit.toNanos(duration);
    }

    /**
     * Returns a string that can be used to parse an equivalent {@code CaffeineSpec}. The order and
     * form of this representation is not guaranteed, except that parsing its output will produce a
     * {@code CaffeineSpec} equal to this instance.
     *
     * @return a string representation of this specification
     */
    public String toParsableString() {
        return specification;
    }

    /**
     * Returns a string representation for this {@code CaffeineSpec} instance. The form of this
     * representation is not guaranteed.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + toParsableString() + '}';
    }

    // -- 上面为原始的逻辑

    /**
     * Ensures that the argument expression is true.
     */
    @FormatMethod
    static void requireArgument(boolean expression, String template, @Nullable Object... args) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(template, args));
        }
    }

    /**
     * Ensures that the state expression is true.
     */
    static void requireState(boolean expression) {
        if (!expression) {
            throw new IllegalStateException();
        }
    }

    /**
     * 获取过期策略
     */
    public String getExpireStrategy() {
        if (expireAfterAccessTimeUnit != null) {
            return "expireAfterAccess";
        }
        if (expireAfterWriteTimeUnit != null) {
            return "expireAfterWrite";
        }
        if (refreshAfterWriteTimeUnit != null) {
            return "refreshAfterWrite";
        }
        return "";
    }

    /**
     * 获取过期时间
     */
    public long getExpireTime() {
        if (expireAfterAccessTimeUnit != null) {
            return expireAfterAccessTimeUnit.toMillis(expireAfterAccessDuration);
        }
        if (expireAfterWriteTimeUnit != null) {
            return expireAfterWriteTimeUnit.toMillis(expireAfterWriteDuration);
        }
        if (refreshAfterWriteTimeUnit != null) {
            return refreshAfterWriteTimeUnit.toMillis(refreshAfterWriteDuration);
        }
        return UNSET_INT;
    }

    public String getSpecification() {
        return specification;
    }

    public int getInitialCapacity() {
        return initialCapacity;
    }

    public long getMaximumWeight() {
        return maximumWeight;
    }

    public long getMaximumSize() {
        return maximumSize;
    }

    public boolean isRecordStats() {
        return recordStats;
    }

    public Strength getKeyStrength() {
        return keyStrength;
    }

    public Strength getValueStrength() {
        return valueStrength;
    }

    public long getExpireAfterAccessDuration() {
        return expireAfterAccessDuration;
    }

    public TimeUnit getExpireAfterAccessTimeUnit() {
        return expireAfterAccessTimeUnit;
    }

    public long getExpireAfterWriteDuration() {
        return expireAfterWriteDuration;
    }

    public TimeUnit getExpireAfterWriteTimeUnit() {
        return expireAfterWriteTimeUnit;
    }

    public long getRefreshAfterWriteDuration() {
        return refreshAfterWriteDuration;
    }

    public TimeUnit getRefreshAfterWriteTimeUnit() {
        return refreshAfterWriteTimeUnit;
    }

}
