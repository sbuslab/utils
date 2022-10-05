package com.sbuslab.utils.config.logger;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class LoggerConfigurationParserTest {

    @Test
    public void correctlyParsesLoggerConfigurations() {
        final Config config = ConfigFactory.parseString(
                "sbuslab.loggers = {\n" +
                "   \"a.b.c\" = \"INFO\"\n" +
                "   \"a.b.c.Class\" = \"DEBUG\"\n" +
                "   \"b.c.d\" = \"WARN\"\n" +
                "   c.d.e.Class = \"ERROR\"\n" +
                "}\n" +
                "sbuslab.loggers.\"d.e.f\" = \"TRACE\"\n" +
                "sbuslab.loggers.d.e.f.Class = \"INFO\"\n" +
                "sbuslab.loggers.d.e.f.Class2 = \"WARN\"\n" +
                "sbuslab.loggers.e.f.g = \"DEBUG\"\n" +
                "sbuslab.loggers.\"f.g.h\" = \"INFO\"\n"
        );
        final Set<LoggerConfigurationData> result = LoggerConfigurationParser.parseLoggersConfiguration(config);
        assertEquals(
                Set.of(
                        new LoggerConfigurationData("a.b.c", "INFO"),
                        new LoggerConfigurationData("a.b.c.Class", "DEBUG"),
                        new LoggerConfigurationData("b.c.d", "WARN"),
                        new LoggerConfigurationData("c.d.e.Class", "ERROR"),
                        new LoggerConfigurationData("d.e.f", "TRACE"),
                        new LoggerConfigurationData("d.e.f.Class", "INFO"),
                        new LoggerConfigurationData("d.e.f.Class2", "WARN"),
                        new LoggerConfigurationData("e.f.g", "DEBUG"),
                        new LoggerConfigurationData("f.g.h", "INFO")
                ),
                result);
    }
}
