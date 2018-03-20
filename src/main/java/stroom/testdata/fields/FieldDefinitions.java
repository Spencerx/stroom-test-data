package stroom.testdata.fields;

import com.google.common.base.Preconditions;
import com.google.common.reflect.ClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Holder for FieldDefinition helper methods
 */
public class FieldDefinitions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldDefinitions.class);

    //TODO Create a builder class for each method (or share builders for common args)

    //TODO change class name to ValueSuppliers

    /**
     * Stateful value supplier that supplies a value from values in sequential order
     * looping back to the beginning when it gets to the end.
     */
    public static Field sequentialValueField(final String name, final List<String> values) {
        try {
            Preconditions.checkNotNull(values);
            Preconditions.checkArgument(!values.isEmpty());
            final AtomicLoopedIntegerSequence indexSequence = new AtomicLoopedIntegerSequence(0, values.size());

            final Supplier<String> supplier = () ->
                    values.get(indexSequence.getNext());
            return new Field(name, supplier);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building sequentialValueField, %s, %s", name, e.getMessage()), e);
        }
    }

    /**
     * {@link Field} that supplies a random value from values on each call to getNext()
     */
    public static Field randomValueField(final String name, final List<String> values) {
        try {
            Preconditions.checkNotNull(values);
            Preconditions.checkArgument(!values.isEmpty());
            final Random random = new Random();
            final Supplier<String> supplier = () ->
                    values.get(random.nextInt(values.size()));
            return new Field(name, supplier);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building randomValueField, %s, %s", name, e.getMessage()), e);
        }
    }

    /**
     * @param name         Field name for use in the header
     * @param format       A {@link String:format} compatible format containing a single
     *                     placeholder, e.g. "user-%s" or "user-%03d"
     * @param maxNumberExc A random number between 0 (inclusive) and maxNumberExc (exclusive) will
     *                     replace the %s in the format string
     * @return A complete {@link Field}
     */
    public static Field randomNumberedValueField(final String name,
                                                 final String format,
                                                 final int maxNumberExc) {
        try {
            Preconditions.checkNotNull(format);
            Preconditions.checkArgument(maxNumberExc > 0);

            final Random random = new Random();
            final Supplier<String> supplier = () ->
                    String.format(format, random.nextInt(maxNumberExc));
            return new Field(name, supplier);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building randomNumberedValueField, %s, %s", name, e.getMessage()), e);
        }
    }

    /**
     * Returns numbered values where the value is defined by a format and the number increases
     * sequentially and loops back round when it hits endEx.
     *
     * @param name     Field name for use in the header
     * @param format   A {@link String:format} compatible format containing a single
     *                 placeholder, e.g. "user-%s" or "user-%03d"
     * @param startInc The lowest value to use in the string format (inclusive)
     * @param endExc   The highest value to use in the string format (exclusive)
     *                 replace the %s in the format string
     * @return A complete {@link Field}
     */
    public static Field sequentiallyNumberedValueField(final String name,
                                                       final String format,
                                                       final int startInc,
                                                       final int endExc) {
        try {
            Preconditions.checkNotNull(format);

            final AtomicLoopedLongSequence numberSequence = new AtomicLoopedLongSequence(
                    startInc,
                    endExc);

            final Supplier<String> supplier = () ->
                    String.format(format, numberSequence.getNext());

            return new Field(name, supplier);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building sequentiallyNumberedValueField, %s, %s", name, e.getMessage()), e);
        }
    }

    /**
     * A field that produces sequential integers starting at startInc (inclusive).
     * If endExc (exclusive) is reached it will loop back round to startInc.
     */
    public static Field sequentialNumberField(final String name,
                                              final long startInc,
                                              final long endExc) {

        try {
            final AtomicLoopedLongSequence numberSequence = new AtomicLoopedLongSequence(
                    startInc,
                    endExc);

            final Supplier<String> supplier = () ->
                    Long.toString(numberSequence.getNext());

            return new Field(name, supplier);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building sequentialValueField, %s, %s", name, e.getMessage()), e);
        }
    }

    /**
     * A field that produces integers between startInc (inclusive) and endExc (exclusive)
     */
    public static Field randomNumberField(final String name,
                                          final int startInc,
                                          final int endExc) {

        try {
            Preconditions.checkArgument(endExc > startInc);

            return new Field(
                    name,
                    () -> Integer.toString(buildRandomNumberSupplier(startInc, endExc).getAsInt()));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building sequentialValueField, %s, %s", name, e.getMessage()), e);
        }
    }

    /**
     * A field that produces IP address conforming to [0-9]{1-3}\.[0-9]{1-3}\.[0-9]{1-3}\.[0-9]{1-3}
     */
    public static Field randomIpV4Field(final String name) {

        try {
            final IntSupplier intSupplier = buildRandomNumberSupplier(0, 256);

            final Supplier<String> supplier = () ->
                    String.format("%d.%d.%d.%d",
                            intSupplier.getAsInt(),
                            intSupplier.getAsInt(),
                            intSupplier.getAsInt(),
                            intSupplier.getAsInt());
            return new Field(name, supplier);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building randomIpV4Field, %s, %s", name, e.getMessage()), e);
        }
    }

    /**
     * A field to produce a sequence of random datetime values within a defined time range.
     * The formatter controls the output format.
     *
     * @param formatStr Format string conforming to the format expected by {@link DateTimeFormatter}
     */
    public static Field randomDateTimeField(final String name,
                                            final LocalDateTime startDateInc,
                                            final LocalDateTime endDateExc,
                                            final String formatStr) {
        try {
            Preconditions.checkNotNull(startDateInc);
            Preconditions.checkNotNull(endDateExc);
            Preconditions.checkNotNull(formatStr);
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(formatStr);
            return randomDateTimeField(name, startDateInc, endDateExc, dateTimeFormatter);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building randomDateTimeField, %s, %s", name, e.getMessage()), e);
        }
    }

    /**
     * A field to produce a sequence of random datetime values within a defined time range.
     * The formatter controls the output format.
     */
    public static Field randomDateTimeField(final String name,
                                            final LocalDateTime startDateInc,
                                            final LocalDateTime endDateExc,
                                            final DateTimeFormatter formatter) {
        try {
            Preconditions.checkNotNull(startDateInc);
            Preconditions.checkNotNull(endDateExc);
            Preconditions.checkNotNull(formatter);
            Preconditions.checkArgument(endDateExc.isAfter(startDateInc));

            final long millisBetween = endDateExc.toInstant(ZoneOffset.UTC).toEpochMilli()
                    - startDateInc.toInstant(ZoneOffset.UTC).toEpochMilli();

            final Supplier<String> supplier = () -> {
                try {
                    final long randomDelta = (long) (Math.random() * millisBetween);
                    final LocalDateTime dateTime = startDateInc.plus(randomDelta, ChronoUnit.MILLIS);
                    return dateTime.format(formatter);
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Time range is too large, maximum allowed: %s",
                            Duration.ofMillis(Integer.MAX_VALUE).toString()), e);
                }
            };
            return new Field(name, supplier);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building randomDateTimeField, %s, %s", name, e.getMessage()), e);
        }
    }

    /**
     * A field to produce a sequence of datetime values with a constant delta based on
     * a configured start datetime and delta. The formatter controls the output format.
     */
    public static Field sequentialDateTimeField(final String name,
                                                final LocalDateTime startDateInc,
                                                final Duration delta,
                                                final String formatStr) {
        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(formatStr);
            return sequentialDateTimeField(name, startDateInc, delta, dateTimeFormatter);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(String.format("Error building sequentialDateTimeField, %s, %s", name, e.getMessage()), e);
        }
    }
    /**
     * A field to produce a sequence of datetime values with a constant delta based on
     * a configured start datetime and delta. The formatter controls the output format.
     */
    public static Field sequentialDateTimeField(final String name,
                                                final LocalDateTime startDateInc,
                                                final Duration delta,
                                                final DateTimeFormatter formatter) {
        try {
            final AtomicReference<LocalDateTime> lastValueRef = new AtomicReference<>(startDateInc);

            final Supplier<String> supplier = () -> {
                try {
                    return lastValueRef.getAndUpdate(lastVal -> lastVal.plus(delta))
                            .format(formatter);
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Time range is too large, maximum allowed: %s",
                            Duration.ofMillis(Integer.MAX_VALUE).toString()));
                }
            };
            return new Field(name, supplier);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building sequentialDateTimeField, %s, %s", name, e.getMessage()), e);
        }
    }

    /**
     * A field that produces a new random UUID on each call to getNext()
     */
    public static Field uuidField(final String name) {
        try {
            return new Field(name, () -> UUID.randomUUID().toString());
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building uuidField, %s, %s", name, e.getMessage()), e);
        }
    }

    /**
     * A field populated with a random number (between minCount and maxCount) of
     * words separated by ' '. The words are picked at random from all the class
     * names in the 'java' package on the classpath
     */
    public static Field randomClassNamesField(final String name,
                                              final int minCount,
                                              final int maxCount) {
        try {
            final List<String> classNames;
            try {
                classNames = ClassNamesListHolder.getClassNames();
            } catch (Exception e) {
                throw new RuntimeException(String.format("Error getting class names list, %s", e.getMessage()), e);
            }

            Preconditions.checkNotNull(classNames);
            Preconditions.checkArgument(!classNames.isEmpty(),
                    "classNames cannot be empty, something has gone wrong finding the class names");

            return randomWordsField(name, minCount, maxCount, classNames);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building randomClassNamesField, %s, %s", name, e.getMessage()), e);
        }
    }

    /**
     * A field populated with a random number (between minCount and maxCount) of
     * words separated by ' ' as picked randomly from wordList
     */
    public static Field randomWordsField(final String name,
                                         final int minCount,
                                         final int maxCount,
                                         final List<String> wordList) {
        try {
            Preconditions.checkArgument(minCount >= 0);
            Preconditions.checkArgument(maxCount >= minCount);
            Preconditions.checkNotNull(wordList);
            Preconditions.checkArgument(wordList.size() > 0,
                    "wordList must have size greater than zero, size %s",
                    wordList.size());

            final Random random = new Random();

            Supplier<String> supplier = () -> {
                int wordCount = random.nextInt(maxCount - minCount + 1) + minCount;
                return IntStream.rangeClosed(0, wordCount)
                        .boxed()
                        .map(i -> wordList.get(random.nextInt(wordList.size())))
                        .collect(Collectors.joining(" "))
                        .replaceAll("(^\\s+|\\s+$)", "") //remove leading/trailing spaces
                        .replaceAll("\\s\\s+", " "); //replace multiple spaces with one
            };

            return new Field(name, supplier);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building randomWordsField, %s, %s", name, e.getMessage()), e);
        }
    }


    private static IntSupplier buildRandomNumberSupplier(final int startInc,
                                                         final int endExc) {
        try {
            Preconditions.checkArgument(endExc > startInc);

            final Random random = new Random();
            final int delta = endExc - startInc;

            return () ->
                    random.nextInt(delta) + startInc;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error building randomNumberSupplier, %s", e.getMessage()), e);
        }
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Holder class for the static class names list to allow for lazy initialisation
     */
    private static class ClassNamesListHolder {
        private static List<String> classNames;

        static final List<String> NOT_FOUND_LIST = Collections.singletonList("ERROR_NO_CLASS_NAMES_FOUND");

        static {
            //lazy initialisation
            classNames = generateList();
//            System.out.println("ClassNames size: " + classNames.size());
        }

        static List<String> getClassNames() {
            return classNames;
        }

        private static List<String> generateList() {

            try {
                final ClassLoader loader = Thread.currentThread().getContextClassLoader();

                List<String> classNames = ClassPath.from(loader).getAllClasses().stream()
                        .filter(classInfo -> classInfo.getPackageName().startsWith("java."))
                        .map(ClassPath.ClassInfo::getSimpleName)
                        .collect(Collectors.toList());

                if (classNames.isEmpty()) {
                    return NOT_FOUND_LIST;
                } else {
                    return classNames;
                }
            } catch (Exception e) {
                LOGGER.error("Error reading classloader", e);
                return NOT_FOUND_LIST;
            }
        }
    }
}
