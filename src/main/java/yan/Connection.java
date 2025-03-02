package yan;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class Connection {

    // 收发线程
    public static Thread serverThread, clientThread;

    // 收发任务进度
    public static Task<Void> serverTask, clientTask;
    public static ServerSocket server;

    // 收发数量和总量
    public static int count_in, count_out, n_in, n_out;


    public static boolean server(int port, File... files) {
        // 创建socket
        server = null;
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            // 说明无法使用该端口
        }
        if (server == null) {
            Main.logger.log(Level.WARNING, "[服务端]无法创建连接于" + port);
            new Alert(Alert.AlertType.ERROR, "端口已被占用!请尝试切换端口进行发送", ButtonType.CANCEL).show();
            return false;
        }

        // 监视进度
        serverTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(500);
                updateProgress(FileIO.outProgress, FileIO.outSize);
                long lastProgress = FileIO.outProgress;
                // 持续检测
                while (!isCancelled() && count_out < n_out) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {// 检查任务是否中断
                        if (isCancelled()) break;
                    }
                    updateProgress(FileIO.outProgress, FileIO.outSize);
                    updateMessage(String.format("%-80s%s/s",
                            String.format("进度:%s/%s", Main.byteFormat(FileIO.outProgress), Main.byteFormat(FileIO.outSize)),
                            Main.byteFormat((FileIO.outProgress - lastProgress) * 5))
                    );
                    lastProgress = FileIO.outProgress;
                }
                updateProgress(FileIO.outProgress, FileIO.outSize);
                succeeded();
                return null;
            }
        };

        // 创建新线程以接收文件
        ServerSocket finalServer = server;
        serverThread = new Thread(() -> {
            // 等待连接
            try {
                Socket socket = finalServer.accept();
                // 启动进度检测
                Thread 服务端进度监听 = new Thread(serverTask, "服务端进度监听");
                服务端进度监听.setDaemon(true);
                服务端进度监听.start();
                // 信息
                DataOutputStream dataOUT = new DataOutputStream(socket.getOutputStream());
                // 获取所有需要传输的文件
                List<FilePack> filePacks = new ArrayList<>();
                n_out = 0;
                for (File f : files) {
                    filePacks.add(new FilePack(f));
                    n_out += filePacks.getLast().files.size();
                }

                // 发送需要传输的文件数量
                dataOUT.writeInt(n_out);
                dataOUT.flush();
                // 发送各个文件
                count_out = 0;
                filePacks.forEach(filePack -> {
                    // 对于文件,直接发送
                    if (!filePack.isDir) {
                        FileIO.file2data(filePack.files.getFirst(), dataOUT);
                        count_out++;
                    }
                    // 对于文件夹,特殊发送
                    else {
                        for (File file : filePack.files) {
                            // 获取相对文件夹根目录的位置
                            String path = file.getPath().substring(filePack.root.length() + 1);
                            // 文件长度
                            long size = file.length();
                            try {
                                FileIO.fin2data(path, size, new FileInputStream(file), dataOUT);
                            } catch (FileNotFoundException e) {
                                Main.warning("发送文件\"" + path + "\"时失败(无法创建输入流)!");
                            }
                            count_out++;
                        }
                    }
                });
                // 关闭
                socket.close();
                finalServer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "服务端文件发送线程");
        serverThread.start();

        return true;
    }

    /**
     * 连接至指定的服务器并接收文件
     *
     * @param host 指定的服务端ip
     * @param port 服务端开放连接的端口
     * @return 是否成功连接
     */
    public static boolean client(String host, int port) {
        // 连接
        Socket socket = null;
        try {
            socket = new Socket(host, port);
        } catch (IOException e) {
            // 无法连接
            Main.warning("无法连接至服务端!");
        }
        if (socket == null) {
            return false;
        }

        // 监视进度
        clientTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(500);
                updateProgress(FileIO.inProgress, FileIO.inSize);
                long lastProgress = FileIO.inProgress;
                // 持续检测
                while (!isCancelled() && count_in < n_in) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        if (isCancelled()) break;
                    }
                    // 刷新进度
                    updateProgress(FileIO.inProgress, FileIO.inSize);
                    // 刷新进度信息
                    updateMessage(String.format("%-80s%s/s",
                            String.format("进度:%s/%s", Main.byteFormat(FileIO.inProgress), Main.byteFormat(FileIO.inSize)),
                            Main.byteFormat((FileIO.inProgress - lastProgress) * 5)));
                    lastProgress = FileIO.inProgress;
                }
                updateProgress(FileIO.inProgress, FileIO.inSize);
                succeeded();
                return null;
            }
        };
        // 新线程
        Socket finalSocket = socket;
        clientThread = new Thread(() -> {
            try {
                DataInputStream dataIN = new DataInputStream(finalSocket.getInputStream());
                // 接收文件数量
                n_in = dataIN.readInt();
                // 接收文件
                for (count_in = 0; count_in < n_in; count_in++)
                    FileIO.data2file(dataIN);
                finalSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "客户端接收线程");
        // 接收数据
        clientThread.start();
        // 接收进度
        new Thread(clientTask).start();

        return true;
    }

    public static void stop() {
        // 当程序结束时调用
        if (serverThread != null)
            serverThread.interrupt();
        if (server != null)
            try {
                server.close();
            } catch (IOException e) {
            }
    }
}

/**
 * 存储需要发送的文件信息
 */
class FilePack {

    public List<File> files = new ArrayList<>();

    public String root = "";

    public boolean isDir;

    public FilePack(File file) {
        // 初始化文件列表
        addFile(file);
        isDir = file.isDirectory();
        // 获取文件根目录
        root = file.getParent();
    }

    private void addFile(File file) {
        // 如果是文件就向列表中添加
        if (file.isFile()) files.add(file);
            // 如果是文件夹就递归添加
        else for (File listFile : Objects.requireNonNull(file.listFiles())) addFile(listFile);
    }
}