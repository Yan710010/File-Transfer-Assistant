package yan;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    // 全局日志发送
    public static Logger logger = Logger.getGlobal();

    public static void main(String[] args) {
        // 启动App
        App.launch(App.class, args);
    }

    public static void info(String text) {
        logger.log(Level.INFO, text);
    }

    public static void warning(String text) {
        logger.log(Level.WARNING, text);
    }

    /**
     * 将字节数转换为合适单位的文字
     *
     * @param n 以字节数表示的文件大小
     * @return 以B/KB/MB/GB表示的文件大小
     */
    public static String byteFormat(long n) {
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        double a = n;
        int i;
        for (i = 0; a >= 1024 && i < units.length - 1; i++) a /= 1024;
        return String.format("%.1f%s", a, units[i]);
    }
}