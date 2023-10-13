package com.dtcookie.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dtcookie.util.Streams;


public final class SubProcess extends Thread {

    private static final Logger log = LogManager.getLogger(SubProcess.class);

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private final boolean isInstrumented;
    private final String purpose;
    private final String clusterID;
    private final String virtualHostName;

    private Process process;

    public SubProcess(String clusterID, String purpose, String virtualHostName, boolean isInstrumented) {
        this.purpose = purpose;
        this.clusterID = clusterID;
        this.virtualHostName = virtualHostName;
        this.isInstrumented = isInstrumented;
    }

    public void execute() throws IOException {

        String environmentPrefix = InetAddress.getLocalHost().getHostName();

        Properties allEnvVars = new Properties();
        try (InputStream in = SubProcess.class.getClassLoader().getResourceAsStream("jvm.environment.properties")) {
            allEnvVars.load(in);
        }
        String purposePrefix = purpose + ".";
        Properties envVars = new Properties();
        for (Object oKey : allEnvVars.keySet()) {
            String key = (String)oKey;
            if (key.equals("ENVIRONMENT")) {
                environmentPrefix = allEnvVars.get(key).toString();
            }
        }
        for (Object oKey : allEnvVars.keySet()) {
            String key = (String)oKey;
            if (key.startsWith(purposePrefix)) {
                envVars.put(key.toString().substring(purposePrefix.length()), allEnvVars.get(oKey).toString().replace("${ENVIRONMENT}", environmentPrefix));
            }
        } 

        Properties allJvmOptions = new Properties();
        try (InputStream in = SubProcess.class.getClassLoader().getResourceAsStream("jvm.options.properties")) {
            allJvmOptions.load(in);
        }
        
        Properties jvmOptions = new Properties();
        for (Object oKey : allJvmOptions.keySet()) {
            String key = (String)oKey;
            if (key.startsWith(purposePrefix)) {
                jvmOptions.put(key, allJvmOptions.get(oKey).toString().replace("${ENVIRONMENT}", environmentPrefix));
            }
        }

        // jvmOptions.put(UUID.randomUUID().toString(), "-Djdk.net.hosts.file=hosts");

        String classPath = System.getProperty("java.class.path");
        String javaHome = System.getProperty("java.home");
        File javaExecutable = new File(new File(new File(javaHome), "bin"), "javaw.exe");
        if (!javaExecutable.exists()) {
            javaExecutable = new File(new File(new File(javaHome), "bin"), "java");
        }
        ArrayList<String> command = new ArrayList<String>();
        command.add(javaExecutable.getAbsolutePath());
        File tmpDir = new File(".tmp");
        tmpDir.mkdirs();        
       
        ArrayList<String> options = new ArrayList<>();
        for (Object jvmOption : jvmOptions.values()) {
            options.add(jvmOption.toString());
        }
        options.add("-cp");
        options.add(classPath);

        Path temp = Files.createTempFile(tmpDir.toPath(), "opts_", ".argfile");
        try (OutputStream fos = new FileOutputStream(temp.toAbsolutePath().toString())) {
            Streams.copy(String.join(" ", options).getBytes(StandardCharsets.UTF_8), fos);
        }
        
        String arg = "@"+temp.toAbsolutePath().toString();
        command.add(arg);
        // command.add(classPath);
        command.add(BootStrap.class.getName());

        log.info(String.join(" ", command));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(new File(".").getAbsoluteFile());
        builder.environment().put("DT_DEBUGFLAGS", "debugDisableJavaInstrumentationNative=" + (!isInstrumented));
        builder.environment().put("DT_TAGS", "environment=" + environmentPrefix);
        for (Object key : envVars.keySet()) {
            // System.out.println("---- " + key.toString() + ": " + envVars.get(key).toString());    
            builder.environment().put(key.toString(), envVars.get(key).toString());
        }            

        builder.environment().put("DEMO_PURPOSE", purpose);
        builder.environment().put("DT_CLUSTER_ID", environmentPrefix + "-" + clusterID);
        builder.environment().put("DT_LOCALTOVIRTUALHOSTNAME", environmentPrefix + "-" + virtualHostName);        

        synchronized (this) {
            this.process = builder.start();
        }

        executorService.submit(new StreamGobbler(process.getInputStream(), System.out::println));
        executorService.submit(new StreamGobbler(process.getErrorStream(), System.err::println));

        Runtime.getRuntime().addShutdownHook(this);
    }

    @Override
    public void run() {
        synchronized (this) {
            if (this.process != null) {
                this.process.destroy();
            }
        }
    }
}
