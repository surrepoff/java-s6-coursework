package org.surrepoff.stat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerMonitor {
    private final ServerMonitorInfo sm_info;

    ServerMonitor() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        if (isWindows) {
            System.out.println("ERROR: IT'S WINDOWS");
            System.exit(1);
        }

        sm_info = new ServerMonitorInfo();
    }

    public void loadConfig() throws IOException {
        System.getProperties().load(ClassLoader.getSystemResourceAsStream("config.properties"));
        sm_info.get_cpu = Boolean.parseBoolean(System.getProperty("get.cpu"));
        sm_info.get_ram = Boolean.parseBoolean(System.getProperty("get.ram"));
        sm_info.get_memory = Boolean.parseBoolean(System.getProperty("get.memory"));
        sm_info.get_ping = Boolean.parseBoolean(System.getProperty("get.ping"));
        sm_info.get_net_int = Boolean.parseBoolean(System.getProperty("get.net_int"));
        sm_info.get_time_s = Integer.parseInt(System.getProperty("get.time_s"));

        String[] parts;

        parts = System.getProperty("get.memory.name").split(";");
        for (String part : parts){
            if (part.length() > 0)
                sm_info.get_memory_name.add(part);
        }

        parts = System.getProperty("get.ping.site").split(";");
        for (String part : parts){
            if (part.length() > 0)
                sm_info.get_ping_site.add(part);
        }

        parts = System.getProperty("get.net_int.name").split(";");
        for (String part : parts){
            if (part.length() > 0)
                sm_info.get_net_int_name.add(part);
        }
    }

    public void run() throws IOException {
        loadConfig();

        while (true) {
            if (sm_info.get_cpu)
                getLoadCPUThreads();

            if (sm_info.get_ram)
                getLoadRAM();

            if (sm_info.get_memory)
                getLoadMemory();

            if (sm_info.get_ping)
                for (String site : sm_info.get_ping_site)
                    getTimePing(site);

            if (sm_info.get_net_int)
                getLoadNetworkInterface();

            try {
                Thread.sleep(sm_info.get_time_s * 1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void getLoadCPUThreads() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "top -b -1 -n 1 -w 200");

        builder.redirectErrorStream(true);

        Process p = builder.start();

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

        Pattern pattern_cpu = Pattern.compile("%Cpu\\d+");
        Pattern pattern_cpu_value = Pattern.compile("\\d{1,3}.\\d{1,2} id");

        ArrayList<Float> load_cpu = new ArrayList<>();

        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }

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

        sm_info.result_cpu = load_cpu;
    }

    public void getLoadRAM() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "free");

        builder.redirectErrorStream(true);

        Process p = builder.start();

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

        Pattern pattern_mem = Pattern.compile("Mem:");
        Pattern pattern_value = Pattern.compile("\\d+");

        float load_ram = 0;

        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }

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

        sm_info.result_ram = load_ram;
    }

    public void getTimePing(String address) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "ping -c 3 " + address);

        builder.redirectErrorStream(true);

        Process p = builder.start();

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

        Pattern pattern_loss = Pattern.compile("\\d{1,3}%");
        Pattern pattern_time = Pattern.compile("\\d+.\\d*");

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
                        if (matcher_time.find())
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
            time = -1;

        sm_info.result_ping_time.add(time);
    }

    public void getLoadMemory() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "df");

        builder.redirectErrorStream(true);

        Process p = builder.start();

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

        Pattern pattern_name = Pattern.compile("/[^ \\n]*");
        Pattern pattern_value = Pattern.compile(" \\d+");

        ArrayList<String> load_mem_name = new ArrayList<>();
        ArrayList<Float> load_mem = new ArrayList<>();

        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }

            Matcher matcher_name = pattern_name.matcher(line);
            boolean find_name = false;
            String name = "";
            float value;

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
            for (String get_name : sm_info.get_memory_name)
            {
                if (Objects.equals(load_name, get_name))
                    System.out.printf("%s = %.2f %%\n", get_name, value);
            }
        }

        for (String get_name : sm_info.get_memory_name)
        {
            boolean find = false;
            for (int i = 0; i < load_mem_name.size(); i++) {
                String load_name = load_mem_name.get(i);
                Float value = load_mem.get(i);

                if (Objects.equals(load_name, get_name)) {
                    find = true;
                    sm_info.result_memory_load.add(value);
                    break;
                }
            }

            if (!find)
                sm_info.result_memory_load.add((float) -1);
        }
    }

    public void getLoadNetworkInterface() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "ifconfig");

        builder.redirectErrorStream(true);

        Process p = builder.start();

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

        Pattern pattern_name = Pattern.compile(".+: ");
        Pattern pattern_value = Pattern.compile(" \\d+ ");

        ArrayList<String> net_int_name = new ArrayList<>();
        ArrayList<Integer> net_int_rcv = new ArrayList<>();
        ArrayList<Integer> net_int_snt = new ArrayList<>();

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
            String name;

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
            for (String get_name : sm_info.get_net_int_name) {
                if (Objects.equals(load_name, get_name))
                    System.out.printf("%s = %d %d\n", get_name, rcv, snt);
            }
        }

        for (String get_name : sm_info.get_net_int_name)
        {
            boolean find = false;
            for (int i = 0; i < net_int_name.size(); i++) {
                String load_name = net_int_name.get(i);
                Integer rcv = net_int_rcv.get(i);
                Integer snt = net_int_snt.get(i);

                if (Objects.equals(load_name, get_name)) {
                    find = true;
                    sm_info.result_net_int_rcv.add(rcv);
                    sm_info.result_net_int_snt.add(snt);
                    break;
                }
            }

            if (!find) {
                sm_info.result_net_int_rcv.add(-1);
                sm_info.result_net_int_snt.add(-1);
            }
        }
    }
}
