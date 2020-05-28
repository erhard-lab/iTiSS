package gedi.analyzer;

import gedi.modules.ModuleBase;
import gedi.utils.multithreading.NotifyOnFinishedRunnable;

public class ModuleWaiting extends NotifyOnFinishedRunnable {
    private long millis;
    private ModuleBase module;

    public ModuleWaiting(ModuleBase module, long millis) {
        this.millis = millis;
        this.module = module;
    }

    @Override
    public void doRun() {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public ModuleBase getModule() {
        return module;
    }
}
