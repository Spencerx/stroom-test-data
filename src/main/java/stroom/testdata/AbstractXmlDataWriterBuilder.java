package stroom.testdata;

import stroom.testdata.fields.Field;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class AbstractXmlDataWriterBuilder {
    private Optional<String> namespace = Optional.empty();
    private String rootElementName = "records";
    String recordElementName = "record";

    public AbstractXmlDataWriterBuilder namespace(final String namespace) {
        this.namespace = Optional.of(namespace);
        return this;
    }

    public AbstractXmlDataWriterBuilder rootElementName(final String rootElementName) {
        this.rootElementName = rootElementName;
        return this;
    }

    public AbstractXmlDataWriterBuilder recordElementName(final String recordElementName) {
        this.recordElementName = recordElementName;
        return this;
    }

    public DataWriter<String> build() {
        //return our mapping function which conforms to the DataWriter interface
        return this::mapRecords;
    }

    private Function<Record, String> getDataMapper(final List<Field> fields) {
        final String recordFormatStr = buildRecordFormatString(fields);

        return record -> {
            String[] valuesArr = new String[record.getValues().size()];
            record.getValues().toArray(valuesArr);
            return String.format(recordFormatStr, (Object[]) valuesArr);
        };
    }

    protected abstract String buildRecordFormatString(List<Field> fields);

    Stream<String> mapRecords(Stream<Record> recordStream) {
        final Function<Record, String> dataMapper = getDataMapper(fields);

        final Stream<String> dataStream = recordStream.map(dataMapper);
        final String namespaceAtr = namespace
                .map(namespace -> String.format(" xmlns=\"%s\"", namespace))
                .orElse("");

        final String xmlDeclaration = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        final String openRootElm = String.format("<%s%s>", rootElementName, namespaceAtr);
        final String closeRootElm = String.format("</%s>", rootElementName);

        final Stream<String> headerStream = Stream.of(xmlDeclaration, openRootElm);
        final Stream<String> footerStream = Stream.of(closeRootElm);

        //have to force stream to sequential to ensure header and footer go at the
        //top and bottom respectively.
        //TODO it would probably be better to improve the DataWrite interface to expose methods
        //to get the header/footer, then the data stream can be done in parallel
        return Stream.concat(Stream.concat(headerStream, dataStream), footerStream).sequential();
    }
}
