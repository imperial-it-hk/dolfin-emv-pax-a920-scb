package com.pax.pay.uart;

public interface CommConnInterface {
    byte [] Read();
    int Write (byte [] data_buf);
    //void Connect(ConnectionInterface Listener);
    boolean Connect(ConnectionInterface Listener);
    boolean Connect();
    boolean Disconnect();

}
