package th.co.bkkps.amexapi;

import th.co.bkkps.bps_amexapi.TransAPIFactory;

public class AmexTransAPI {
    private static AmexTransAPI instance;
    private final AmexTransProcess process;

    public AmexTransAPI() {
        this.process = new AmexTransProcess(TransAPIFactory.createTransAPI());
    }

    public synchronized static AmexTransAPI getInstance() {
        if (instance == null) {
            instance = new AmexTransAPI();
        }
        return instance;
    }

    public AmexTransProcess getProcess() {
        return process;
    }
}
