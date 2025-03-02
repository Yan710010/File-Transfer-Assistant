package yan;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
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

    /**
     * 获取本机host
     *
     * @return 本机ip
     * @throws UnknownHostException 如果无法获取ip地址
     */
    public static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // 遍历所有的网络接口
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // 在所有的接口下再遍历IP
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {// 排除loopback类型地址
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException(
                    "Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }
}