package corf.desktop.tools.filebuilder;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import corf.base.common.KeyValue;
import corf.base.text.LineSeparator;
import corf.base.io.FileSystemUtils;
import corf.base.text.CSV;
import corf.desktop.tools.common.Param;
import corf.desktop.tools.common.Param.Type;
import corf.desktop.tools.filebuilder.Generator.Options;

import java.io.File;
import java.io.StringWriter;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class GeneratorTest {

    static final Options STANDARD_OPTS = new Options(UTF_8, LineSeparator.UNIX.getCharacters(), false, false);
    static final File TMP_FILE = FileSystemUtils.createTempFile().toFile();
    static final String DATA = """
                               10,11,12
                               20,21,22
                               30,31,32
                               """;

    @Test
    public void testHeaderAndFooterPlaceholdersReplaced() throws Exception {
        var template = Template.create("test", "${_csv0}");
        template.setHeader("header ${param1} ${_csv0}");
        template.setFooter("footer ${param2} ${_csv0}");
        template.setParams(Set.of(
                new TestParam("param1", Type.CONSTANT, null, "value1"),
                new TestParam("param2", Type.CONSTANT, null, "value2")
        ));

        var generator = new Generator(template, CSV.from(DATA), TMP_FILE, STANDARD_OPTS);
        StringWriter out = new StringWriter();
        generator.generate(out);

        String[] result = out.toString().split(template.getLineSeparator().getCharacters());

        assertThat(result).hasSize(5);
        assertThat(result[0]).isEqualTo("header value1 ${_csv0}");
        assertThat(result[4]).isEqualTo("footer value2 ${_csv0}");
    }

    @Test
    public void testPatternPlaceholdersReplaced() throws Exception {
        var template = Template.create("test", "${param} ${_csv0} ${_csv2}");
        template.setParams(Set.of(
                new TestParam("param", Type.CONSTANT, null, "value")
        ));

        var generator = new Generator(template, CSV.from(DATA), TMP_FILE, STANDARD_OPTS);
        StringWriter out = new StringWriter();
        generator.generate(out);

        String[] result = out.toString().split(template.getLineSeparator().getCharacters());

        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo("value 10 12");
        assertThat(result[1]).isEqualTo("value 20 22");
        assertThat(result[2]).isEqualTo("value 30 32");
    }

    @Test
    public void testDelimiterSupported() throws Exception {
        var template = Template.create("test", "{ foo: \"${_csv0}\" }");
        template.setHeader("[");
        template.setFooter("]");
        template.setDelimiter(",");
        template.setParams(Set.of(
                new TestParam("param", Type.CONSTANT, null, "value")
        ));

        var generator = new Generator(template, CSV.from(DATA), TMP_FILE, STANDARD_OPTS);
        StringWriter out = new StringWriter();
        generator.generate(out);

        String[] result = out.toString().split(template.getLineSeparator().getCharacters());

        assertThat(result).hasSize(5);
        assertThat(result[0]).isEqualTo("[");
        assertThat(result[1]).isEqualTo("{ foo: \"10\" },");
        assertThat(result[2]).isEqualTo("{ foo: \"20\" },");
        assertThat(result[3]).isEqualTo("{ foo: \"30\" }");
        assertThat(result[4]).isEqualTo("]");
    }

    @Test
    public void testAutoGeneratedParamsUpdatedAtEveryIteration() throws Exception {
        var template = Template.create("test", "${param1} ${param2}");
        template.setParams(Set.of(
                new TestParam("param1", Type.PASSWORD, null, null),
                new TestParam("param2", Type.TIMESTAMP, null, null)
        ));

        var generator = new Generator(template, CSV.from(DATA), TMP_FILE, STANDARD_OPTS);
        StringWriter out = new StringWriter();
        generator.generate(out);

        String[] result = out.toString().split(template.getLineSeparator().getCharacters());

        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo("password/0 timestamp/0");
        assertThat(result[1]).isEqualTo("password/1 timestamp/1");
        assertThat(result[2]).isEqualTo("password/2 timestamp/2");
    }

    @Test
    public void testIndexParamsSupported() throws Exception {
        var template = Template.create("test", "foo/${_index0} bar/${_index1}");

        var generator = new Generator(template, CSV.from(DATA), TMP_FILE, STANDARD_OPTS);
        StringWriter out = new StringWriter();
        generator.generate(out);

        String[] result = out.toString().split(template.getLineSeparator().getCharacters());

        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo("foo/0 bar/1");
        assertThat(result[1]).isEqualTo("foo/1 bar/2");
        assertThat(result[2]).isEqualTo("foo/2 bar/3");
    }

    ///////////////////////////////////////////////////////////////////////////

    public static class TestParam extends Param {

        private int counter = 0;

        public TestParam(String name, Type type, @Nullable String option, @Nullable String value) {
            super(name, type, option, value);
        }

        @Override
        public KeyValue<String, String> resolve() {
            var value = isAutoGenerated() ? (getType() + "/" + counter).toLowerCase() : getValue();
            var kv = new KeyValue<>(getName(), value);
            counter++;
            return kv;
        }
    }
}
