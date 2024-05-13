package th.co.bkkps.utils;

import android.os.Environment;

import java.io.File;

import de.mindpipe.android.logging.log4j.LogConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public class ALogger {

    public static Logger getLogger(Class clazz) {
        final LogConfigurator logConfigurator = new LogConfigurator();

        String path = File.separator + "sdcard" + File.separator + "bpsedc.log";
        File dir = new File(path);
        if(dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(dir, children[i]).delete();
            }
        }

        //String path = FinancialApplication.getApp().getFilesDir() + File.separator + "bpsedc.log";
        logConfigurator.setFileName(path);
        logConfigurator.setRootLevel(Level.ALL);
        logConfigurator.setLevel("org.apache", Level.ALL);
        logConfigurator.setUseFileAppender(true);
        logConfigurator.setFilePattern("%d %-5p [%c{2}]-[%L] %m%n");
        logConfigurator.setMaxFileSize(1024 * 1024 * 50); //50MB
        logConfigurator.setImmediateFlush(true);
        logConfigurator.configure();
        Logger log = Logger.getLogger(clazz.getSimpleName());
        return log;
    }
}
