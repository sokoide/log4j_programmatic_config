package com.sokoide;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.*;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.IOException;
import java.nio.charset.Charset;

public class Main {
    static {
        try {
            setupMinimumLogger();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //private static Logger logger;
    private static final  Logger logger = LogManager.getLogger(Main.class);

    private static void setupMinimumLogger() throws IOException {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        // console
        AppenderComponentBuilder console = builder.newAppender("stdout", "Console");

        // flow
        FilterComponentBuilder flow = builder.newFilter("MarkerFilter", Filter.Result.ACCEPT, Filter.Result.DENY);
        flow.addAttribute("marker", "FLOW");
        console.add(flow);

        // layout
        LayoutComponentBuilder standard = builder.newLayout("PatternLayout");
        standard.addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable");

        console.add(standard);

        // build
        builder.add(console);

        // loggers
        RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.ERROR);
        rootLogger.add(builder.newAppenderRef("stdout"));
        builder.add(rootLogger);

        builder.writeXmlConfiguration(System.out);

        Configurator.initialize(builder.build());
    }

    private static void test1() throws IOException {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        // console
        AppenderComponentBuilder console = builder.newAppender("stdout", "Console");

        // file
        AppenderComponentBuilder file = builder.newAppender("log", "File");
        file.addAttribute("fileName", "java3.log");

        // rolling file
        AppenderComponentBuilder rollingFile
                = builder.newAppender("rolling", "RollingFile");
        rollingFile.addAttribute("fileName", "rolling.log");
        rollingFile.addAttribute("filePattern", "rolling-%i.log");

        // flow
        FilterComponentBuilder flow = builder.newFilter("MarkerFilter", Filter.Result.ACCEPT, Filter.Result.DENY);
        flow.addAttribute("marker", "FLOW");
        console.add(flow);

        // layout
        LayoutComponentBuilder standard = builder.newLayout("PatternLayout");
        standard.addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable");

        console.add(standard);
        file.add(standard);
        rollingFile.add(standard);

        // policy
        ComponentBuilder triggeringPolicies = builder.newComponent("Policies")
                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy")
                        .addAttribute("size", "1K"));
        rollingFile.addComponent(triggeringPolicies);

        // strategy
        ComponentBuilder strategy = builder.newComponent("DefaultRolloverStrategy")
                .addAttribute("max", "1");
        rollingFile.addComponent(strategy);

        // build
        builder.add(console);
        builder.add(file);
        builder.add(rollingFile);

        // loggers
        RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.ERROR);
        rootLogger.add(builder.newAppenderRef("stdout"));
        builder.add(rootLogger);

        LoggerComponentBuilder l = builder.newLogger("com", Level.ERROR);
        l.add(builder.newAppenderRef("log"));
        l.add(builder.newAppenderRef("rolling"));
        l.addAttribute("additivity", false);
        builder.add(l);

        builder.writeXmlConfiguration(System.out);

        Configurator.initialize(builder.build());
    }

    private static void testModify(){
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        // set log level for rolling
        LoggerConfig lc = config.getLoggerConfig("com");
        lc.setLevel(Level.INFO);

        System.out.println("* rolling");
        RollingFileAppender rolling = config.getAppender("rolling");
        RollingFileManager manager = rolling.getManager();
        System.out.printf("* manager.strategy: %s\n", manager.getRolloverStrategy().toString());

        // set strategy
        DefaultRolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                .withMax("3")
                .build();
        manager.setRolloverStrategy(strategy);

        // remove "com" logger
        FileAppender fa = config.getAppender("log");
        config.removeLogger("com");

        // print again
        System.out.printf("* manager.strategy: %s\n", manager.getRolloverStrategy().toString());

        ctx.updateLoggers();
    }

    private static void updateLoggerConfig() throws IOException {
        // update
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        // create appenders
        PatternLayout layout = PatternLayout.newBuilder()
                .withCharset(Charset.defaultCharset())
                .withPattern("%d [%t] %-5level: %msg%n%throwable")
                .withConfiguration(config)
                .build();
        FileAppender fa = FileAppender.newBuilder()
                .setName("FILE_APPENDER")
                .setLayout(layout)
                .withFileName("file.log")
                .build();
        fa.start();
        // Rolling File Appender
        DefaultRolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                .withMax("3")
                .build();
        SizeBasedTriggeringPolicy policy = SizeBasedTriggeringPolicy.createPolicy("1K");
        RollingFileAppender rfa = RollingFileAppender.newBuilder()
                .setName("ROLLING_FILE_APPENDER")
                .setLayout(layout)
                .withFileName("rolling.log")
                .withFilePattern("rolling-%i.log")
                .withStrategy(strategy)
                .withPolicy(policy)
                .build();
        rfa.start();

        // add appender
        AppenderRef ref = AppenderRef.createAppenderRef("ROLILNG_FILE_APPENDER",Level.INFO,null);
        AppenderRef ref2= AppenderRef.createAppenderRef("FILE_APPENDER",Level.INFO,null);
        AppenderRef[] refs = new AppenderRef[] {ref, ref2};

        LoggerConfig lc = LoggerConfig.createLogger(false, Level.INFO, "com", null, refs, null, config, null);
        lc.addAppender(fa, Level.INFO, null);
        lc.addAppender(rfa, Level.INFO, null);

        // add logger
        config.addLogger("com", lc);

        // update
        ctx.updateLoggers();
    }

//    private static void test2() {
//        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
//        final Configuration config = ctx.getConfiguration();
////        Layout layout = PatternLayout.createLayout(PatternLayout.SIMPLE_CONVERSION_PATTERN, config, null,
////                null, null, null);
//        Layout layout = PatternLayout.newBuilder()
//                .withPattern("%d [%t] %-5level: %msg%n%throwable")
//                .build();
//        Appender appender = FileAppender.createAppender("test.log", "false", "false", "File", "true",
//                "false", "false", "4000", layout, null, "false", null, config);
//        appender.start();
//        config.addAppender(appender);
//        AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);
//        AppenderRef[] refs = new AppenderRef[]{ref};
//        LoggerConfig loggerConfig = LoggerConfig.createLogger("false", Level.valueOf("info"), "org.apache.logging.log4j",
//                "true", refs, null, config, null);
//        loggerConfig.addAppender(appender, null, null);
//        config.addLogger("org.apache.logging.log4j", loggerConfig);
//        ctx.updateLoggers();
//    }
//
//    public static void test3() {
//        LoggerContext context = (LoggerContext) LogManager.getContext();
//        Configuration config = context.getConfiguration();
//
//        //PatternLayout layout= PatternLayout.createLayout("%m%n", null, null, Charset.defaultCharset(),false,false,null,null);
//        Layout layout = PatternLayout.newBuilder()
//                .withPattern("%d [%t] %-5level: %msg%n%throwable")
//                .build();
//        Appender appender = ConsoleAppender.createAppender(layout, null, null, "CONSOLE_APPENDER", null, null);
//        appender.start();
//        AppenderRef ref = AppenderRef.createAppenderRef("CONSOLE_APPENDER", null, null);
//        AppenderRef[] refs = new AppenderRef[]{ref};
//        LoggerConfig loggerConfig = LoggerConfig.createLogger(false, Level.INFO, "CONSOLE_LOGGER",
//                "com", refs, null, null, null);
//        loggerConfig.addAppender(appender, null, null);
//
//        config.addAppender(appender);
//        config.addLogger("com", loggerConfig);
//        context.updateLoggers(config);
//
//        Logger logger = LogManager.getContext().getLogger("com");
//        logger.info("HELLO_WORLD");
//    }

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("* Started");
        //test2();
        //test3();
        //logger = LogManager.getLogger(Main.class);

        logger.info("Reconfiguring");
        updateLoggerConfig();
        logger.info("Reconfigured");
        for(int i=0;i<3000;i++) {
            logger.info("Hello");
            logger.error("World");
            logger.warn("Done");
            logger.debug("Bye");
        }

        System.out.println("* Completed");
        LogManager.shutdown();
    }
}
