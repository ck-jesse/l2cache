package com.coy.l2cache.content;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author chenck
 * @date 2020/9/2 17:46
 */
public class CustomGuavaCacheBuilderSpec {

    // 自定义 Strength
    enum Strength {WEAK, SOFT}

    /**
     * Parses a single value.
     */
    private interface ValueParser {
        void parse(CustomGuavaCacheBuilderSpec spec, String key, @Nullable String value);
    }

    /**
     * Splits each key-value pair.
     */
    private static final Splitter KEYS_SPLITTER = Splitter.on(',').trimResults();

    /**
     * Splits the key from the value.
     */
    private static final Splitter KEY_VALUE_SPLITTER = Splitter.on('=').trimResults();

    /**
     * Map of names to ValueParser.
     */
    private static final ImmutableMap<String, CustomGuavaCacheBuilderSpec.ValueParser> VALUE_PARSERS =
            ImmutableMap.<String, CustomGuavaCacheBuilderSpec.ValueParser>builder()
                    .put("initialCapacity", new CustomGuavaCacheBuilderSpec.InitialCapacityParser())
                    .put("maximumSize", new CustomGuavaCacheBuilderSpec.MaximumSizeParser())
                    .put("maximumWeight", new CustomGuavaCacheBuilderSpec.MaximumWeightParser())
                    .put("concurrencyLevel", new CustomGuavaCacheBuilderSpec.ConcurrencyLevelParser())
                    .put("weakKeys", new CustomGuavaCacheBuilderSpec.KeyStrengthParser(Strength.WEAK))
                    .put("softValues", new CustomGuavaCacheBuilderSpec.ValueStrengthParser(Strength.SOFT))
                    .put("weakValues", new CustomGuavaCacheBuilderSpec.ValueStrengthParser(Strength.WEAK))
                    .put("recordStats", new CustomGuavaCacheBuilderSpec.RecordStatsParser())
                    .put("expireAfterAccess", new CustomGuavaCacheBuilderSpec.AccessDurationParser())
                    .put("expireAfterWrite", new CustomGuavaCacheBuilderSpec.WriteDurationParser())
                    .put("refreshAfterWrite", new CustomGuavaCacheBuilderSpec.RefreshDurationParser())
                    .put("refreshInterval", new CustomGuavaCacheBuilderSpec.RefreshDurationParser())
                    .build();

    @MonotonicNonNull
    @VisibleForTesting
    Integer initialCapacity;
    @MonotonicNonNull
    @VisibleForTesting
    Long maximumSize;
    @MonotonicNonNull
    @VisibleForTesting
    Long maximumWeight;
    @MonotonicNonNull
    @VisibleForTesting
    Integer concurrencyLevel;
    @MonotonicNonNull
    @VisibleForTesting
    Strength keyStrength;
    @MonotonicNonNull
    @VisibleForTesting
    Strength valueStrength;
    @MonotonicNonNull
    @VisibleForTesting
    Boolean recordStats;
    @VisibleForTesting
    long writeExpirationDuration;
    @MonotonicNonNull
    @VisibleForTesting
    TimeUnit writeExpirationTimeUnit;
    @VisibleForTesting
    long accessExpirationDuration;
    @MonotonicNonNull
    @VisibleForTesting
    TimeUnit accessExpirationTimeUnit;
    @VisibleForTesting
    long refreshDuration;
    @MonotonicNonNull
    @VisibleForTesting
    TimeUnit refreshTimeUnit;
    /**
     * Specification; used for toParseableString().
     */
    private final String specification;

    private CustomGuavaCacheBuilderSpec(String specification) {
        this.specification = specification;
    }

    /**
     * Creates a CacheBuilderSpec from a string.
     *
     * @param cacheBuilderSpecification the string form
     */
    public static CustomGuavaCacheBuilderSpec parse(String cacheBuilderSpecification) {
        CustomGuavaCacheBuilderSpec spec = new CustomGuavaCacheBuilderSpec(cacheBuilderSpecification);
        if (!cacheBuilderSpecification.isEmpty()) {
            for (String keyValuePair : KEYS_SPLITTER.split(cacheBuilderSpecification)) {
                List<String> keyAndValue = ImmutableList.copyOf(KEY_VALUE_SPLITTER.split(keyValuePair));
                checkArgument(!keyAndValue.isEmpty(), "blank key-value pair");
                checkArgument(
                        keyAndValue.size() <= 2,
                        "key-value pair %s with more than one equals sign",
                        keyValuePair);

                // Find the ValueParser for the current key.
                String key = keyAndValue.get(0);
                CustomGuavaCacheBuilderSpec.ValueParser valueParser = VALUE_PARSERS.get(key);
                checkArgument(valueParser != null, "unknown key %s", key);

                String value = keyAndValue.size() == 1 ? null : keyAndValue.get(1);
                valueParser.parse(spec, key, value);
            }
        }

        return spec;
    }

    /**
     * Returns a CacheBuilderSpec that will prevent caching.
     */
    public static CustomGuavaCacheBuilderSpec disableCaching() {
        // Maximum size of zero is one way to block caching
        return CustomGuavaCacheBuilderSpec.parse("maximumSize=0");
    }

    /**
     * Returns a CacheBuilder configured according to this instance's specification.
     */
    public CacheBuilder<Object, Object> toCacheBuilder() {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        if (initialCapacity != null) {
            builder.initialCapacity(initialCapacity);
        }
        if (maximumSize != null) {
            builder.maximumSize(maximumSize);
        }
        if (maximumWeight != null) {
            builder.maximumWeight(maximumWeight);
        }
        if (concurrencyLevel != null) {
            builder.concurrencyLevel(concurrencyLevel);
        }
        if (keyStrength != null) {
            switch (keyStrength) {
                case WEAK:
                    builder.weakKeys();
                    break;
                default:
                    throw new AssertionError();
            }
        }
        if (valueStrength != null) {
            switch (valueStrength) {
                case SOFT:
                    builder.softValues();
                    break;
                case WEAK:
                    builder.weakValues();
                    break;
                default:
                    throw new AssertionError();
            }
        }
        if (recordStats != null && recordStats) {
            builder.recordStats();
        }
        if (writeExpirationTimeUnit != null) {
            builder.expireAfterWrite(writeExpirationDuration, writeExpirationTimeUnit);
        }
        if (accessExpirationTimeUnit != null) {
            builder.expireAfterAccess(accessExpirationDuration, accessExpirationTimeUnit);
        }
        if (refreshTimeUnit != null) {
            builder.refreshAfterWrite(refreshDuration, refreshTimeUnit);
        }

        return builder;
    }

    /**
     * Returns a string that can be used to parse an equivalent {@code CacheBuilderSpec}. The order
     * and form of this representation is not guaranteed, except that reparsing its output will
     * produce a {@code CacheBuilderSpec} equal to this instance.
     */
    public String toParsableString() {
        return specification;
    }

    /**
     * Returns a string representation for this CacheBuilderSpec instance. The form of this
     * representation is not guaranteed.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).addValue(toParsableString()).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                initialCapacity,
                maximumSize,
                maximumWeight,
                concurrencyLevel,
                keyStrength,
                valueStrength,
                recordStats,
                durationInNanos(writeExpirationDuration, writeExpirationTimeUnit),
                durationInNanos(accessExpirationDuration, accessExpirationTimeUnit),
                durationInNanos(refreshDuration, refreshTimeUnit));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CustomGuavaCacheBuilderSpec)) {
            return false;
        }
        CustomGuavaCacheBuilderSpec that = (CustomGuavaCacheBuilderSpec) obj;
        return Objects.equal(initialCapacity, that.initialCapacity)
                && Objects.equal(maximumSize, that.maximumSize)
                && Objects.equal(maximumWeight, that.maximumWeight)
                && Objects.equal(concurrencyLevel, that.concurrencyLevel)
                && Objects.equal(keyStrength, that.keyStrength)
                && Objects.equal(valueStrength, that.valueStrength)
                && Objects.equal(recordStats, that.recordStats)
                && Objects.equal(
                durationInNanos(writeExpirationDuration, writeExpirationTimeUnit),
                durationInNanos(that.writeExpirationDuration, that.writeExpirationTimeUnit))
                && Objects.equal(
                durationInNanos(accessExpirationDuration, accessExpirationTimeUnit),
                durationInNanos(that.accessExpirationDuration, that.accessExpirationTimeUnit))
                && Objects.equal(
                durationInNanos(refreshDuration, refreshTimeUnit),
                durationInNanos(that.refreshDuration, that.refreshTimeUnit));
    }

    /**
     * Converts an expiration duration/unit pair into a single Long for hashing and equality. Uses
     * nanos to match CacheBuilder implementation.
     */
    private static @Nullable Long durationInNanos(long duration, @Nullable TimeUnit unit) {
        return (unit == null) ? null : unit.toNanos(duration);
    }

    /**
     * Base class for parsing integers.
     */
    abstract static class IntegerParser implements CustomGuavaCacheBuilderSpec.ValueParser {
        protected abstract void parseInteger(CustomGuavaCacheBuilderSpec spec, int value);

        @Override
        public void parse(CustomGuavaCacheBuilderSpec spec, String key, String value) {
            checkArgument(value != null && !value.isEmpty(), "value of key %s omitted", key);
            try {
                parseInteger(spec, Integer.parseInt(value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        format("key %s value set to %s, must be integer", key, value), e);
            }
        }
    }

    /**
     * Base class for parsing integers.
     */
    abstract static class LongParser implements CustomGuavaCacheBuilderSpec.ValueParser {
        protected abstract void parseLong(CustomGuavaCacheBuilderSpec spec, long value);

        @Override
        public void parse(CustomGuavaCacheBuilderSpec spec, String key, String value) {
            checkArgument(value != null && !value.isEmpty(), "value of key %s omitted", key);
            try {
                parseLong(spec, Long.parseLong(value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        format("key %s value set to %s, must be integer", key, value), e);
            }
        }
    }

    /**
     * Parse initialCapacity
     */
    static class InitialCapacityParser extends CustomGuavaCacheBuilderSpec.IntegerParser {
        @Override
        protected void parseInteger(CustomGuavaCacheBuilderSpec spec, int value) {
            checkArgument(
                    spec.initialCapacity == null,
                    "initial capacity was already set to ",
                    spec.initialCapacity);
            spec.initialCapacity = value;
        }
    }

    /**
     * Parse maximumSize
     */
    static class MaximumSizeParser extends CustomGuavaCacheBuilderSpec.LongParser {
        @Override
        protected void parseLong(CustomGuavaCacheBuilderSpec spec, long value) {
            checkArgument(spec.maximumSize == null, "maximum size was already set to ", spec.maximumSize);
            checkArgument(
                    spec.maximumWeight == null, "maximum weight was already set to ", spec.maximumWeight);
            spec.maximumSize = value;
        }
    }

    /**
     * Parse maximumWeight
     */
    static class MaximumWeightParser extends CustomGuavaCacheBuilderSpec.LongParser {
        @Override
        protected void parseLong(CustomGuavaCacheBuilderSpec spec, long value) {
            checkArgument(
                    spec.maximumWeight == null, "maximum weight was already set to ", spec.maximumWeight);
            checkArgument(spec.maximumSize == null, "maximum size was already set to ", spec.maximumSize);
            spec.maximumWeight = value;
        }
    }

    /**
     * Parse concurrencyLevel
     */
    static class ConcurrencyLevelParser extends CustomGuavaCacheBuilderSpec.IntegerParser {
        @Override
        protected void parseInteger(CustomGuavaCacheBuilderSpec spec, int value) {
            checkArgument(
                    spec.concurrencyLevel == null,
                    "concurrency level was already set to ",
                    spec.concurrencyLevel);
            spec.concurrencyLevel = value;
        }
    }

    /**
     * Parse weakKeys
     */
    static class KeyStrengthParser implements CustomGuavaCacheBuilderSpec.ValueParser {
        private final Strength strength;

        public KeyStrengthParser(Strength strength) {
            this.strength = strength;
        }

        @Override
        public void parse(CustomGuavaCacheBuilderSpec spec, String key, @Nullable String value) {
            checkArgument(value == null, "key %s does not take values", key);
            checkArgument(spec.keyStrength == null, "%s was already set to %s", key, spec.keyStrength);
            spec.keyStrength = strength;
        }
    }

    /**
     * Parse weakValues and softValues
     */
    static class ValueStrengthParser implements CustomGuavaCacheBuilderSpec.ValueParser {
        private final Strength strength;

        public ValueStrengthParser(Strength strength) {
            this.strength = strength;
        }

        @Override
        public void parse(CustomGuavaCacheBuilderSpec spec, String key, @Nullable String value) {
            checkArgument(value == null, "key %s does not take values", key);
            checkArgument(
                    spec.valueStrength == null, "%s was already set to %s", key, spec.valueStrength);

            spec.valueStrength = strength;
        }
    }

    /**
     * Parse recordStats
     */
    static class RecordStatsParser implements CustomGuavaCacheBuilderSpec.ValueParser {

        @Override
        public void parse(CustomGuavaCacheBuilderSpec spec, String key, @Nullable String value) {
            checkArgument(value == null, "recordStats does not take values");
            checkArgument(spec.recordStats == null, "recordStats already set");
            spec.recordStats = true;
        }
    }

    /**
     * Base class for parsing times with durations
     */
    abstract static class DurationParser implements CustomGuavaCacheBuilderSpec.ValueParser {
        protected abstract void parseDuration(CustomGuavaCacheBuilderSpec spec, long duration, TimeUnit unit);

        @Override
        public void parse(CustomGuavaCacheBuilderSpec spec, String key, String value) {
            checkArgument(value != null && !value.isEmpty(), "value of key %s omitted", key);
            try {
                char lastChar = value.charAt(value.length() - 1);
                TimeUnit timeUnit;
                switch (lastChar) {
                    case 'd':
                        timeUnit = TimeUnit.DAYS;
                        break;
                    case 'h':
                        timeUnit = TimeUnit.HOURS;
                        break;
                    case 'm':
                        timeUnit = TimeUnit.MINUTES;
                        break;
                    case 's':
                        timeUnit = TimeUnit.SECONDS;
                        break;
                    default:
                        throw new IllegalArgumentException(
                                format(
                                        "key %s invalid format.  was %s, must end with one of [dDhHmMsS]", key, value));
                }

                long duration = Long.parseLong(value.substring(0, value.length() - 1));
                parseDuration(spec, duration, timeUnit);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        format("key %s value set to %s, must be integer", key, value));
            }
        }
    }

    /**
     * Parse expireAfterAccess
     */
    static class AccessDurationParser extends CustomGuavaCacheBuilderSpec.DurationParser {
        @Override
        protected void parseDuration(CustomGuavaCacheBuilderSpec spec, long duration, TimeUnit unit) {
            checkArgument(spec.accessExpirationTimeUnit == null, "expireAfterAccess already set");
            spec.accessExpirationDuration = duration;
            spec.accessExpirationTimeUnit = unit;
        }
    }

    /**
     * Parse expireAfterWrite
     */
    static class WriteDurationParser extends CustomGuavaCacheBuilderSpec.DurationParser {
        @Override
        protected void parseDuration(CustomGuavaCacheBuilderSpec spec, long duration, TimeUnit unit) {
            checkArgument(spec.writeExpirationTimeUnit == null, "expireAfterWrite already set");
            spec.writeExpirationDuration = duration;
            spec.writeExpirationTimeUnit = unit;
        }
    }

    /**
     * Parse refreshAfterWrite
     */
    static class RefreshDurationParser extends CustomGuavaCacheBuilderSpec.DurationParser {
        @Override
        protected void parseDuration(CustomGuavaCacheBuilderSpec spec, long duration, TimeUnit unit) {
            checkArgument(spec.refreshTimeUnit == null, "refreshAfterWrite already set");
            spec.refreshDuration = duration;
            spec.refreshTimeUnit = unit;
        }
    }

    private static String format(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }

    // -- 上面为原始的逻辑

    /**
     * 获取过期时间
     */
    public long getExpireTime() {
        // refreshAfterWrite 第一优先
        if (refreshTimeUnit != null) {
            return refreshTimeUnit.toMillis(refreshDuration);
        }
        // expireAfterWrite 第二优先
        if (writeExpirationTimeUnit != null) {
            return writeExpirationTimeUnit.toMillis(writeExpirationDuration);
        }
        if (accessExpirationTimeUnit != null) {
            return accessExpirationTimeUnit.toMillis(accessExpirationDuration);
        }
        return -1;
    }

    public Integer getInitialCapacity() {
        return initialCapacity;
    }

    public Long getMaximumSize() {
        return maximumSize;
    }

    public Long getMaximumWeight() {
        return maximumWeight;
    }

    public Integer getConcurrencyLevel() {
        return concurrencyLevel;
    }

    public Strength getKeyStrength() {
        return keyStrength;
    }

    public Strength getValueStrength() {
        return valueStrength;
    }

    public Boolean getRecordStats() {
        return recordStats;
    }

    public long getWriteExpirationDuration() {
        return writeExpirationDuration;
    }

    public TimeUnit getWriteExpirationTimeUnit() {
        return writeExpirationTimeUnit;
    }

    public long getAccessExpirationDuration() {
        return accessExpirationDuration;
    }

    public TimeUnit getAccessExpirationTimeUnit() {
        return accessExpirationTimeUnit;
    }

    public long getRefreshDuration() {
        return refreshDuration;
    }

    public TimeUnit getRefreshTimeUnit() {
        return refreshTimeUnit;
    }

    public String getSpecification() {
        return specification;
    }
}
