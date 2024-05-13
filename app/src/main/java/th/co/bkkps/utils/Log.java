package th.co.bkkps.utils;

import com.github.tony19.timber.loggly.LogglyTree;
import com.pax.edc.BuildConfig;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.utils.UtilPermissions;
import com.pax.pay.utils.Utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.os.Build;

public final class Log {

    public final static int V = 0;
    public final static int D = 1;
    public final static int I = 2;
    public final static int W = 3;
    public final static int E = 4;

    private Log() {}

    public interface Printer {
        public void print(int level, String tag, String msg);
    }

    private static class SystemOutPrinter implements Printer {
        private final static String[] LEVELS = new String[]{"V", "D", "I", "W", "E"};
        public void print(int level, String tag, String msg) {
            System.out.println(LEVELS[level] + "/" + tag + ": " + msg);
        }
    }

    private static class AndroidPrinter implements Printer {

        private final static String[] METHOD_NAMES = new String[]{"v", "d", "i", "w", "e"};

        private final Class<?> mLogClass;
        private final Method[] mLogMethods;
        private final boolean mLoaded;

        public AndroidPrinter() {
            Class logClass = null;
            boolean loaded = false;
            mLogMethods = new Method[METHOD_NAMES.length];
            try {
                logClass = Class.forName("android.util.Log");
                for (int i = 0; i < METHOD_NAMES.length; i++) {
                    mLogMethods[i] = logClass.getMethod(METHOD_NAMES[i], String.class, String.class);
                }
                loaded = true;
            } catch (NoSuchMethodException|ClassNotFoundException e) {
                // Ignore
            }
            mLogClass = logClass;
            mLoaded = loaded;
        }

        public void print(int level, String tag, String msg) {
            try {
                if (mLoaded) {
                    mLogMethods[level].invoke(null, tag, msg);
                }
            } catch (InvocationTargetException|IllegalAccessException e) {
                // Ignore
            }
        }
    }

    private static class Log4JPrinter implements Printer {
        public void print(int level, String tag, String msg) {
            if(UtilPermissions.hasPermissions(FinancialApplication.getApp(), WRITE_EXTERNAL_STORAGE)) {
                Logger logger = ALogger.getLogger(Log.class);
                switch (level) {
                    case 0:
                        logger.log(Level.TRACE, tag + ':' + msg);
                        break;
                    case 1:
                        logger.log(Level.DEBUG, tag + ':' + msg);
                        break;
                    case 2:
                        logger.log(Level.INFO, tag + ':' + msg);
                        break;
                    case 3:
                        logger.log(Level.WARN, tag + ':' + msg);
                        break;
                    case 4:
                        logger.log(Level.ERROR, tag + ':' + msg);
                        break;
                }
            }
            else
            {
                ANDROID.print(level, tag, msg);
            }
        }
    }


    private static class LogglyPrinter implements Printer {

        public void print(int level, String tag, String msg) {
            Timber.tag(tag);
            switch (level) {
                case 0:
                    Timber.v(msg);
                    break;
                case 1:
                    Timber.d(msg);
                    break;
                case 2:
                    Timber.i(msg);
                    break;
                case 3:
                    Timber.w(msg);
                    break;
                case 4:
                    Timber.e(msg);
                    break;
            }
        }
    }



    public final static SystemOutPrinter SYSTEM = new SystemOutPrinter();
    public final static AndroidPrinter ANDROID = new AndroidPrinter();
    public final static Log4JPrinter LOG4JPRINTER = new Log4JPrinter();
    public final static LogglyPrinter LOGGLYPRINTER = new LogglyPrinter();

    private final static Map<String, String> mTags = new ConcurrentHashMap<>();

    private static String[] mUseTags = new String[]{"tag", "TAG"};
    private static boolean mUseFormat = false;
    private static int mMinLevel = V;

    private static Set<Printer> mPrinters = new CopyOnWriteArraySet<>();

    static {
        /*
        if (ANDROID.mLoaded) {
            usePrinter(ANDROID, true);
        } else {
            usePrin=ter(SYSTEM, true);

        }
         */

        // Log to file
        if (Utils.isSandboxBuild())
            usePrinter(LOG4JPRINTER, true);

        if (BuildConfig.FLAVOR.toUpperCase().equals("SANDBOX")) {
            AddLog4J();
        }

    }

    public static synchronized Log useTags(String[] tags) {
        mUseTags = tags;
        return null;
    }

    public static synchronized Log level(int minLevel) {
        mMinLevel = minLevel;
        return null;
    }

    public static synchronized Log useFormat(boolean yes) {
        mUseFormat = yes;
        return null;
    }

    public static synchronized Log usePrinter(Printer p, boolean on) {
        if (on) {
            mPrinters.add(p);
        } else {
            mPrinters.remove(p);
        }
        return null;
    }

    public static synchronized Log v(Object msg, Object... args) {
        log(V, mUseFormat, msg, args);
        //logger.info(msg);
        return null;
    }
    public static synchronized Log d(Object msg, Object... args) {
        log(D, mUseFormat, msg, args);
        //logger.info(msg);
        return null;
    }
    public static synchronized Log i(Object msg, Object... args) {
        log(I, mUseFormat, msg, args);
        //logger.info(msg);
        return null;
    }
    public static synchronized Log w(Object msg, Object... args) {
        log(W, mUseFormat, msg, args);
        //logger.info(msg);
        return null;
    }
    public static synchronized Log e(Object msg, Object... args) {
        log(E, mUseFormat, msg, args);
        //logger.info(msg);
        return null;
    }

    private static void log(int level, boolean fmt, Object msg, Object... args) {
        if (level < mMinLevel) {
            return;
        }
        String tag = tag();
        if (mUseTags.length > 0 && tag.equals(msg)) {
            if (args.length > 1) {
                print(level, tag, format(fmt, args[0], Arrays.copyOfRange(args, 1, args.length)));
            } else {
                print(level, tag, format(fmt, (args.length > 0 ? args[0] : "")));
            }
        } else {
            print(level, tag, format(fmt, msg, args));
        }
    }

    private static String format(boolean fmt, Object msg, Object... args) {
        Throwable t = null;
        if (args == null) {
            // Null array is not supposed to be passed into this method, so it must
            // be a single null argument
            args = new Object[]{null};
        }
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            t = (Throwable) args[args.length - 1];
            args = Arrays.copyOfRange(args, 0, args.length - 1);
        }
        if (fmt && msg instanceof String) {
            String head = (String) msg;
            if (head.indexOf('%') != -1) {
                return String.format(head, args);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(msg == null ? "null" : msg.toString());
        for (Object arg : args) {
            sb.append("\t");
            sb.append(arg == null ? "null" : arg.toString());
        }
        if (t != null) {
            sb.append("\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            sb.append(sw.toString());
        }
        return sb.toString();
    }

    final static int MAX_LOG_LINE_LENGTH = 4000;

    private static void print(int level, String tag, String msg) {
        for (String line : msg.split("\\n")) {
            do {
                int splitPos = Math.min(MAX_LOG_LINE_LENGTH, line.length());
                for (int i = splitPos-1; line.length() > MAX_LOG_LINE_LENGTH && i >= 0; i--) {
                    if (" \t,.;:?!{}()[]/\\".indexOf(line.charAt(i)) != -1) {
                        splitPos = i;
                        break;
                    }
                }
                splitPos = Math.min(splitPos + 1, line.length());
                String part = line.substring(0, splitPos);
                line = line.substring(splitPos);

                for (Printer p : mPrinters) {
                    p.print(level, tag, part);
                }
                if (!BuildConfig.BUILD_TYPE.equalsIgnoreCase(Constants.BUILD_TYPE_RELEASE))
                    System.out.println(part);
            } while (line.length() > 0);
        }
    }

    private final static Pattern ANONYMOUS_CLASS = Pattern.compile("\\$\\d+$");
    private final static int STACK_DEPTH = 4;
    private static String tag() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length < STACK_DEPTH) {
            throw new IllegalStateException
                    ("Synthetic stacktrace didn't have enough elements: are you using proguard?");
        }
        String className = stackTrace[STACK_DEPTH-1].getClassName();
        String tag = mTags.get(className);
        if (tag != null) {
            return tag;
        }

        try {
            Class<?> c = Class.forName(className);
            for (String f : mUseTags) {
                try {
                    Field field = c.getDeclaredField(f);
                    if (field != null) {
                        field.setAccessible(true);
                        Object value = field.get(null);
                        if (value instanceof String) {
                            mTags.put(className, (String) value);
                            return (String) value;
                        }
                    }
                } catch (NoSuchFieldException|IllegalAccessException|
                        IllegalStateException|NullPointerException e) {
                    //Ignore
                }
            }
        } catch (ClassNotFoundException e) { /* Ignore */ }

        // Check class field useTag, if exists - return it, otherwise - return the generated tag
        Matcher m = ANONYMOUS_CLASS.matcher(className);
        if (m.find()) {
            className = m.replaceAll("");
        }
        return className.substring(className.lastIndexOf('.') + 1);
    }

    public static void AddLoggly(){
        //Log to server
        final String LOGGLY_TOKEN = "05ed5b10-0475-4586-88a1-324702d39c00";
        Timber.plant(new LogglyTree(LOGGLY_TOKEN));
        usePrinter(LOGGLYPRINTER, true);
    }

    public static void RemoveLoggly(){
        usePrinter(LOGGLYPRINTER, false);
    }

    public static void AddLog4J(){
        //Log to server
        usePrinter(LOG4JPRINTER, true);
    }

    public static void RemoveLog4J(){
        usePrinter(LOG4JPRINTER, false);
    }

    public static void deleteLog(){
        String path = File.separator + "sdcard";
        File folder = new File(path);

        final File[] files = folder.listFiles( new FilenameFilter() {
            @Override
            public boolean accept( final File dir,
                                   final String name ) {
                return name.matches( "bpsedc.*" );
            }
        } );
        for ( final File file : files ) {
            if ( !file.delete() ) {
                System.err.println( "Can't remove " + file.getAbsolutePath() );
            }
        }
    }

}

