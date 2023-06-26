package org.surrepoff.stat;

import java.util.ArrayList;

public class ServerMonitorInfo {
    public boolean get_cpu;
    public ArrayList<Float> result_cpu;
    public boolean get_ram;
    public float result_ram;
    public boolean get_memory;
    public ArrayList<String> get_memory_name;
    public ArrayList<Float> result_memory_load;
    public boolean get_ping;
    public ArrayList<String> get_ping_site;
    public ArrayList<Float> result_ping_time;
    public boolean get_net_int;
    public ArrayList<String> get_net_int_name;
    public ArrayList<Integer> result_net_int_rcv;
    public ArrayList<Integer> result_net_int_snt;
    public int get_time_s;
    public String database_username;
    public String database_password;

    ServerMonitorInfo(){
        get_memory_name = new ArrayList<>();
        get_ping_site = new ArrayList<>();
        get_net_int_name = new ArrayList<>();

        result_cpu = new ArrayList<>();
        result_memory_load = new ArrayList<>();
        result_ping_time = new ArrayList<>();
        result_net_int_rcv = new ArrayList<>();
        result_net_int_snt = new ArrayList<>();
    }
}
