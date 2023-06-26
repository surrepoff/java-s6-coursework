package org.surrepoff.stat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.sql.Timestamp;

public class DataBase {
    private int sm_id;

    public Connection connectToDB(ServerMonitorInfo sm_info) {
        Connection connection;

        String URL = "jdbc:postgresql://127.0.0.1:5432/sys_stat.db";
        String USERNAME = sm_info.database_username;
        String PASSWORD = sm_info.database_password;

        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);

            if (connection != null) {
                System.out.println("YES");
            } else {
                System.out.println("NO");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return connection;
    }

    public void checkDB(Connection connection) {
        if (checkNoTable(connection, "sm_data"))
            createSMTable(connection);

        if (checkNoTable(connection, "ram_data"))
            createRAMTable(connection);

        if (checkNoTable(connection, "cpu_data"))
            createTable(connection, "cpu_data");

        if (checkNoTable(connection, "memory_data"))
            createTable(connection, "memory_data");

        if (checkNoTable(connection, "ping_data"))
            createTable(connection, "ping_data");

        if (checkNoTable(connection, "net_int_data"))
            createNetIntTable(connection);
    }

    public void addSMInfo(Connection connection, ServerMonitorInfo sm_info) {
        addDataToSMTable(connection, sm_info);
        sm_id = lastIndexFromTable(connection, "sm_data");

        if (sm_info.get_cpu)
            addDataToCPUTable(connection, sm_info);

        if (sm_info.get_ram)
            addDataToRAMTable(connection, sm_info);

        if (sm_info.get_memory)
            addDataToMemoryTable(connection, sm_info);

        if (sm_info.get_ping)
            addDataToPingTable(connection, sm_info);

        if (sm_info.get_net_int)
            addDataToNetIntTable(connection, sm_info);
    }

    private boolean checkNoTable(Connection connection, String table_name) {
        Statement statement;
        ResultSet resultSet;
        boolean exist = false;
        try {
            String query = String.format("SELECT EXISTS (SELECT * FROM pg_tables WHERE tablename = '%s');", table_name);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                exist = resultSet.getBoolean("exists");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return !exist;
    }

    private void createSMTable(Connection connection) {
        Statement statement;
        try {
            String query = "CREATE TABLE sm_data (id SERIAL, time TIMESTAMP, get_cpu BOOLEAN, get_ram BOOLEAN, get_memory BOOLEAN, get_ping BOOLEAN, get_net_int BOOLEAN, primary key(id));";
            statement = connection.createStatement();
            statement.executeUpdate(query);
            System.out.println("TABLE CREATED: sm_data");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createRAMTable(Connection connection) {
        Statement statement;
        try {
            String query = "CREATE TABLE ram_data (id SERIAL, sm_id INTEGER, value REAL, primary key(id));";
            statement = connection.createStatement();
            statement.executeUpdate(query);
            System.out.println("TABLE CREATED: ram_data");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createNetIntTable(Connection connection) {
        Statement statement;
        try {
            String query = "CREATE TABLE net_int_data (id SERIAL, sm_id INTEGER, name VARCHAR(200), value_snt INTEGER, value_rcv INTEGER, primary key(id));";
            statement = connection.createStatement();
            statement.executeUpdate(query);
            System.out.println("TABLE CREATED: net_int_data");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createTable(Connection connection, String table_name) {
        Statement statement;
        try {
            String query = String.format("CREATE TABLE %s (id SERIAL, sm_id INTEGER, name VARCHAR(200), value REAL, primary key(id));", table_name);
            statement = connection.createStatement();
            statement.executeUpdate(query);
            System.out.printf("TABLE CREATED: %s\n", table_name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addDataToSMTable(Connection connection, ServerMonitorInfo sm_info) {
        Statement statement;
        Date date = new Date();
        Timestamp timestamp = new Timestamp(date.getTime());
        try {
            String query = String.format("INSERT INTO sm_data (time, get_cpu, get_ram, get_memory, get_ping, get_net_int) values('%s', %b, %b, %b, %b, %b);", timestamp, sm_info.get_cpu, sm_info.get_ram, sm_info.get_memory, sm_info.get_ping, sm_info.get_net_int);
            statement = connection.createStatement();
            statement.executeUpdate(query);
            System.out.println("DATA ADD TO sm_data");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addDataToRAMTable(Connection connection, ServerMonitorInfo sm_info) {
        Statement statement;
        try {
            String query = String.format("INSERT INTO ram_data (sm_id, value) values(%d, %.2f);", sm_id, sm_info.result_ram);
            statement = connection.createStatement();
            statement.executeUpdate(query);
            System.out.println("DATA ADD TO ram_data");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addDataToCPUTable(Connection connection, ServerMonitorInfo sm_info) {
        Statement statement;
        for (int i = 0; i < sm_info.result_cpu.size(); i++) {
            try {
                String query = String.format("INSERT INTO cpu_data (sm_id, name, value) values(%d, 'CPU%d', %.2f);", sm_id, i, sm_info.result_cpu.get(i));
                statement = connection.createStatement();
                statement.executeUpdate(query);
                System.out.println("DATA ADD TO cpu_data");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void addDataToMemoryTable(Connection connection, ServerMonitorInfo sm_info) {
        Statement statement;
        for (int i = 0; i < sm_info.get_memory_name.size(); i++) {
            try {
                String query = String.format("INSERT INTO memory_data (sm_id, name, value) values(%d, '%s', %.2f);", sm_id, sm_info.get_memory_name.get(i), sm_info.result_memory_load.get(i));
                statement = connection.createStatement();
                statement.executeUpdate(query);
                System.out.println("DATA ADD TO memory_data");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addDataToPingTable(Connection connection, ServerMonitorInfo sm_info) {
        Statement statement;
        for (int i = 0; i < sm_info.get_ping_site.size(); i++) {
            try {
                String query = String.format("INSERT INTO ping_data (sm_id, name, value) values(%d, '%s', %.2f);", sm_id, sm_info.get_ping_site.get(i), sm_info.result_ping_time.get(i));
                statement = connection.createStatement();
                statement.executeUpdate(query);
                System.out.println("DATA ADD TO ping_data");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void addDataToNetIntTable(Connection connection, ServerMonitorInfo sm_info) {
        Statement statement;
        for (int i = 0; i < sm_info.get_net_int_name.size(); i++) {
            try {
                String query = String.format("INSERT INTO net_int_data (sm_id, name, value_snt, value_rcv) values(%d, '%s', %d, %d);", sm_id, sm_info.get_net_int_name.get(i), sm_info.result_net_int_snt.get(i), sm_info.result_net_int_rcv.get(i));
                statement = connection.createStatement();
                statement.executeUpdate(query);
                System.out.println("DATA ADD TO net_int_data");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private int lastIndexFromTable(Connection connection, String table_name) {
        Statement statement;
        ResultSet resultSet;
        int index = 0;
        try {
            String query = String.format("SELECT id FROM %s ORDER BY id DESC LIMIT 1;", table_name);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                index = resultSet.getInt("id");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return index;
    }
}
