package org.surrepoff.stat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerMonitor {
    private boolean get_cpu;
    private boolean get_ram;
    private boolean get_memory;
    private final ArrayList<String> get_memory_name;
    private boolean get_ping;
    private final ArrayList<String> get_ping_site;
    private boolean get_net_int;
    private final ArrayList<String> get_net_int_name;
    private int get_time_s;

    ServerMonitor() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        if (isWindows) {
            System.out.println("ERROR: IT'S WINDOWS");
            System.exit(1);
        }

        get_memory_name = new ArrayList<String>();
        get_ping_site = new ArrayList<String>();
        get_net_int_name = new ArrayList<String>();
    }

    public void loadConfig() throws IOException {
        System.getProperties().load(ClassLoader.getSystemResourceAsStream("config.properties"));
        get_cpu = Boolean.parseBoolean(System.getProperty("get.cpu"));
        get_ram = Boolean.parseBoolean(System.getProperty("get.ram"));
        get_memory = Boolean.parseBoolean(System.getProperty("get.memory"));
        get_ping = Boolean.parseBoolean(System.getProperty("get.ping"));
        get_net_int = Boolean.parseBoolean(System.getProperty("get.net_int"));
        get_time_s = Integer.parseInt(System.getProperty("get.time_s"));

        String[] parts;

        parts = System.getProperty("get.memory.name").split(";");
        for (String part : parts){
            if (part.length() > 0)
                get_memory_name.add(part);
        }

        parts = System.getProperty("get.ping.site").split(";");
        for (String part : parts){
            if (part.length() > 0)
                get_ping_site.add(part);
        }

        parts = System.getProperty("get.net_int.name").split(";");
        for (String part : parts){
            if (part.length() > 0)
                get_net_int_name.add(part);
        }
    }

    public void run() throws IOException {
        loadConfig();

        while (true)
        {
            if (get_cpu)
                getLoadCPUThreads();

            if (get_ram)
                getLoadRAM();

            if (get_memory)
                getLoadMemory();

            if (get_ping)
                for (String site : get_ping_site)
                    getTimePing(site);

            if (get_net_int)
                getLoadNetworkInterface();

            try{
                Thread.sleep(get_time_s * 1000L);
            }catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public int getLoadCPUThreads() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "top -b -1 -n 1 -w 200");

        builder.redirectErrorStream(true);

        Process p = builder.start();

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

        Pattern pattern_cpu = Pattern.compile("%Cpu\\d{1,}");
        Pattern pattern_cpu_value = Pattern.compile("\\d{1,3}.\\d{1,2} id");

        ArrayList<Float> load_cpu = new ArrayList<Float>();

        int number_of_lines = 0;

        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }

            number_of_lines++;

            Matcher matcher_cpu = pattern_cpu.matcher(line);

            if (matcher_cpu.find()) {
                Matcher matcher_cpu_value = pattern_cpu_value.matcher(line);
                while (matcher_cpu_value.find()) {
                    load_cpu.add(100 - Float.parseFloat(line.substring(matcher_cpu_value.start(), matcher_cpu_value.end() - 3)));
                }
            }
            //System.out.println(line);
        }

        //System.out.println(number_of_lines);

        int i = 0;
        for (float f : load_cpu) {
            System.out.printf("CPU%d = %.2f %%\n", i, f);
            i++;
        }

        return 0;
    }

    public float getLoadRAM() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "free");

        builder.redirectErrorStream(true);

        Process p = builder.start();

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

        Pattern pattern_mem = Pattern.compile("Mem:");
        Pattern pattern_value = Pattern.compile("\\d{1,}");

        float load_ram = 0;
        int number_of_lines = 0;

        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }

            number_of_lines++;

            Matcher matcher_mem = pattern_mem.matcher(line);

            if (matcher_mem.find()) {
                Matcher matcher_value = pattern_value.matcher(line);

                int total = 1;
                if (matcher_value.find()) {
                    total = Integer.parseInt(line.substring(matcher_value.start(), matcher_value.end()));
                }

                int used = 1;
                if (matcher_value.find()) {
                    used = Integer.parseInt(line.substring(matcher_value.start(), matcher_value.end()));
                }

                load_ram = ((float) used / total) * 100;
            }

            //System.out.println(line);
        }

        //System.out.println(number_of_lines);

        load_ram = (float) Math.round(load_ram * 100) / 100;

        System.out.printf("RAM = %.2f %%\n", load_ram);

        return load_ram;
    }

    public float getTimePing(String address) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "ping -c 3 " + address);

        builder.redirectErrorStream(true);

        Process p = builder.start();

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

        Pattern pattern_loss = Pattern.compile("\\d{1,3}%");
        Pattern pattern_time = Pattern.compile("\\d{1,}.\\d{0,}");

        int number_of_lines = 0;
        boolean found_loss = false;
        int packet_loss = 0;
        float time = -1;

        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }

            number_of_lines++;

            if (number_of_lines >= 2) {
                if (!found_loss) {
                    Matcher matcher_loss = pattern_loss.matcher(line);

                    if (matcher_loss.find()) {
                        found_loss = true;
                        packet_loss = Integer.parseInt(line.substring(matcher_loss.start(), matcher_loss.end() - 1));
                    }
                }

                if (found_loss) {
                    Matcher matcher_time = pattern_time.matcher(line);

                    if (matcher_time.find()) {
                        matcher_time.find();
                        time = Float.parseFloat(line.substring(matcher_time.start(), matcher_time.end()));
                    }
                }
            }

            //System.out.println(line);
        }

        time = (float) Math.round(time * 100) / 100;

        //System.out.println(number_of_lines);
        System.out.printf("Ping to %s\n", address);
        System.out.printf("Packet loss = %d %%\n", packet_loss);
        System.out.printf("Time = %.2f ms\n", time);

        if (packet_loss == 100)
            return -1;

        return time;
    }

    public float getLoadMemory() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "df");

        builder.redirectErrorStream(true);

        Process p = builder.start();

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

        Pattern pattern_name = Pattern.compile("/[^ \\n]{0,}");
        Pattern pattern_value = Pattern.compile(" \\d{1,}");

        ArrayList<String> load_mem_name = new ArrayList<String>();
        ArrayList<Float> load_mem = new ArrayList<Float>();

        int number_of_lines = 0;

        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }

            number_of_lines++;

            Matcher matcher_name = pattern_name.matcher(line);
            boolean find_name = false;
            String name = "";
            float value = -1;

            while (matcher_name.find()) {
                find_name = true;
                name = line.substring(matcher_name.start(), matcher_name.end());
            }

            if (find_name) {
                Matcher matcher_value = pattern_value.matcher(line);

                int total = 1;
                if (matcher_value.find()) {
                    total = Integer.parseInt(line.substring(matcher_value.start() + 1, matcher_value.end()));
                }

                int used = 1;
                if (matcher_value.find()) {
                    used = Integer.parseInt(line.substring(matcher_value.start() + 1, matcher_value.end()));
                }

                value = ((float) used / total) * 100;
                value = (float) Math.round(value * 100) / 100;

                load_mem_name.add(name);
                load_mem.add(value);
            }

            //System.out.println(line);
        }

        for (int i = 0; i < load_mem_name.size(); i++) {
            String load_name = load_mem_name.get(i);
            Float value = load_mem.get(i);
            for (String get_name : get_memory_name)
            {
                if (Objects.equals(load_name, get_name))
                    System.out.printf("%s = %.2f %%\n", get_name, value);
            }
        }

        return 0;
    }

    public float getLoadNetworkInterface() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "ifconfig");

        builder.redirectErrorStream(true);

        Process p = builder.start();

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

        Pattern pattern_name = Pattern.compile(".{1,}: ");
        Pattern pattern_value = Pattern.compile(" \\d{1,} ");

        ArrayList<String> net_int_name = new ArrayList<String>();
        ArrayList<Integer> net_int_rcv = new ArrayList<Integer>();
        ArrayList<Integer> net_int_snt = new ArrayList<Integer>();

        int number_of_lines = 0;
        int find_name_line = -101;
        boolean find_name = false;

        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }

            number_of_lines++;

            Matcher matcher_name = pattern_name.matcher(line);
            String name = "";

            while (matcher_name.find()) {
                find_name = true;
                name = line.substring(matcher_name.start(), matcher_name.end() - 2);
                find_name_line = number_of_lines;
                net_int_name.add(name);
            }

            Matcher matcher_value = pattern_value.matcher(line);

            if (find_name && number_of_lines == find_name_line + 4) {
                int received = 1;
                while (matcher_value.find()) {
                    received = Integer.parseInt(line.substring(matcher_value.start() + 1, matcher_value.end() - 1));
                }
                net_int_rcv.add(received);
            }

            if (find_name && number_of_lines == find_name_line + 6) {
                int sent = 1;
                while (matcher_value.find()) {
                    sent = Integer.parseInt(line.substring(matcher_value.start() + 1, matcher_value.end() - 1));
                }
                net_int_snt.add(sent);
            }

            //System.out.println(line);
        }


        for (int i = 0; i < net_int_name.size(); i++) {
            String load_name = net_int_name.get(i);
            Integer rcv = net_int_rcv.get(i);
            Integer snt = net_int_snt.get(i);
            for (String get_name : get_net_int_name) {
                if (Objects.equals(load_name, get_name))
                    System.out.printf("%s = %d %d\n", get_name, rcv, snt);
            }
        }

        return 0;
    }
}
