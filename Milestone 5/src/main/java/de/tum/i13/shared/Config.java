package de.tum.i13.shared;

import picocli.CommandLine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    @CommandLine.Option(names = "-p", description = "sets the port of the server", defaultValue = "5153")
    public int port;

    @CommandLine.Option(names = "-a", description = "which address the server should listen to", defaultValue = "127.0.0.1")
    public String listenaddr;

    @CommandLine.Option(names = "-b", description = "bootstrap broker where clients and other brokers connect first to retrieve configuration, port and ip, e.g., 192.168.1.1:5153")
    public InetSocketAddress bootstrap;

    @CommandLine.Option(names = "-d", description = "Directory for files", defaultValue = "data/")
    public Path dataDir;

    @CommandLine.Option(names = "-l", description = "Logfile", defaultValue = "echo.log")
    public Path logfile;

    @CommandLine.Option(names = "-ll", description = "Loglevel", defaultValue = "INFO")
    public String loglevel;

    @CommandLine.Option(names = "-c", description = "Sets the cachesize, e.g., 100 keys")
    public int cachesize;

    @CommandLine.Option(names = "-s", description = "Sets the cache displacement strategy, FIFO, LRU, LFU")
    public String cachedisplacement;

    @CommandLine.Option(names = "-h", description = "Displays help", usageHelp = true)
    public boolean usagehelp;

    @CommandLine.Option(names = "-pc", description = "2 Phase Commit", defaultValue = "false")
    private String consistency;

    public boolean fullConsistency;

    public static Config parseCommandlineArgs(String[] args) {
        Config cfg = new Config();
        CommandLine.ParseResult parseResult = new CommandLine(cfg).registerConverter(InetSocketAddress.class, new InetSocketAddressTypeConverter()).parseArgs(args);

        if (cfg.consistency.equals("true") || cfg.consistency.equals("false")) {
            cfg.fullConsistency = Boolean.parseBoolean(cfg.consistency);
        } else {
            CommandLine.usage(new Config(), System.out);
            System.out.println("Option consistency can only have value true or false");
            System.exit(-1);
        }

        if (!Files.exists(cfg.dataDir)) {
            try {
                Files.createDirectory(cfg.dataDir);
            } catch (IOException e) {
                System.out.println("Could not create directory");
                e.printStackTrace();
                System.exit(-1);
            }
        }

        if (!parseResult.errors().isEmpty()) {
            for (Exception ex : parseResult.errors()) {
                ex.printStackTrace();
            }

            CommandLine.usage(new Config(), System.out);
            System.exit(-1);
        }

        return cfg;
    }

    public static void printHelp() {
        CommandLine.usage(new Config(), System.out);
    }

    @Override
    public String toString() {
        return "Config{" +
                "port=" + port +
                ", listenaddr='" + listenaddr + '\'' +
                ", bootstrap=" + bootstrap +
                ", dataDir=" + dataDir +
                ", logfile=" + logfile +
                ", loglevel='" + loglevel + '\'' +
                ", cachesize=" + cachesize +
                ", cachedisplacement='" + cachedisplacement + '\'' +
                ", usagehelp=" + usagehelp +
                '}';
    }
}
