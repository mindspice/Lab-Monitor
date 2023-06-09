package com.mindspice.networking;


import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.mindspice.Settings;
import com.mindspice.networking.packets.Handshake;
import com.mindspice.networking.packets.NetInfo;
import com.mindspice.networking.packets.Request;
import com.mindspice.networking.packets.Update;
import com.mindspice.system.DiskInfo;
import com.mindspice.system.SysInfo;
import com.mindspice.util.Utils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;

public class StatusClient {
    Client client = new Client();
    Settings settings = Settings.get();

    public StatusClient() {
        var kryo = client.getKryo();
        kryo.register(double[].class);
        kryo.register(String[].class);
        kryo.register(Handshake.class);
        kryo.register(DiskInfo.class);
        kryo.register(NetInfo.class);
        kryo.register(Request.class);
        kryo.register(Update.class);
        kryo.register(DiskInfo.class);
        kryo.register(DiskInfo[].class);

        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof Update update) {
                    switch (update.request) {
                        case INFO -> client.sendTCP(SysInfo.getInstance().getNetInfo());
                        case BATTERY -> {
                            if (!settings.doShutDown || settings.isNUTClient) {
                                return;
                            }
                            if ((settings.isShutDownPct ? update.batteryPct : update.runtime)
                                    <= settings.shutDownThreshold) {
                                Utils.shutdown();
                            }
                        }
                    }
                }
            }
        });

        client.start();
    }

    public boolean connect(){
        try {
            client.connect(5000, settings.statusHostAddr, settings.statusHostPort);
        } catch (IOException e) {
            return false;
        }
        client.sendTCP(new Handshake(
                settings.clientName,
                SysInfo.getInstance().getSystemInfo(),
                SysInfo.getInstance().getMACList().toArray(new String[0]),
                settings.doWakeOnLan,
                (settings.doShutDown && !settings.isNUTClient),
                Settings.get().wakeUpThreshold)
        );

        return true;
    }



    public boolean isConnected() {
        return client.isConnected();
    }
}
