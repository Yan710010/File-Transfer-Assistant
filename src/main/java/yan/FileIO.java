package yan;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

public class FileIO {

    // 文件发送和接收的进度
    public static long outProgress, inProgress;
    // 当前接收文件和发送文件的长度
    public static long outSize, inSize;
    // 当前接收和发送文件的名字
    public static String outFileName = "", inFileName = "";
    // 结束标记
    public static BooleanProperty isInEnd = new SimpleBooleanProperty(false);
    public static BooleanProperty isOutEnd = new SimpleBooleanProperty(false);


    public static void file2data(File file, DataOutputStream dataOUT) {
        try {
            fin2data(file.getName(), file.length(), new FileInputStream(file), dataOUT);
        } catch (FileNotFoundException e) {
            Main.warning("无法创建文件输入流!");
            throw new RuntimeException(e);
        }
    }

    public static void fin2data(String filePath, long fileSize, FileInputStream fileIN, DataOutputStream dataOUT) {
        try {
            isOutEnd.set(false);
            // 发送名字
            outFileName = filePath;
            dataOUT.writeUTF(outFileName);
            dataOUT.flush();
            // 发送文件大小
            outSize = fileSize;
            dataOUT.writeLong(outSize);
            dataOUT.flush();
            // 发送文件内容
            Main.logger.log(Level.INFO, "[服务端]开始发送文件: " + outFileName);
            byte[] bytes = new byte[Config.buffer_size];
            int length;
            outProgress = 0;
            while ((length = fileIN.read(bytes, 0, bytes.length)) != -1) {
                // 发送
                dataOUT.write(bytes, 0, length);
                dataOUT.flush();
                outProgress += length;
                // 该停就停
                if (App.closing) break;
            }
            if (outProgress == outSize) {
                Main.logger.log(Level.INFO, "[服务端]文件\"" + outFileName + "\"发送完毕");
            } else {
                Main.warning("[服务端]文件\"" + outFileName + "\"发送中断!");
            }

            isOutEnd.set(true);
            fileIN.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void data2file(DataInputStream dataIN) {
        try {
            isInEnd.set(false);
            // 在有数据前等待
            while (dataIN.available() == 0) ;
            // 接收文件名
            inFileName = dataIN.readUTF();
            // 接收长度
            inSize = dataIN.readLong();

            // 接收文件并存储至临时文件
            File file = new File(Config.output_file_path + inFileName + ".tmp");
            // 创建文件夹
            file.getParentFile().mkdirs();
            file.createNewFile();
            FileOutputStream fileOUT = new FileOutputStream(file);

            //开始接收文件
            Main.logger.log(Level.INFO, "[客户端]开始接收文件: " + inFileName);
            byte[] bytes = new byte[Config.buffer_size];
            int length;
            inProgress = 0;
            while (inProgress < inSize) {
                length = inSize - inProgress > 1024 ? 1024 : (int) (inSize - inProgress);
                length = dataIN.read(bytes, 0, length);
                if (length == -1) break;// 数据流结束
                fileOUT.write(bytes, 0, length);
                inProgress += length;
                fileOUT.flush();
                // 该停就停
                if (App.closing) break;
            }
            fileOUT.close();
            if (inProgress == inSize) {
                // 表示接收完成
                // 改名
                // 检查是否已存在
                File toFile = new File(Config.output_file_path + inFileName);
                if (toFile.exists()) toFile.delete();// 如果文件已经存在则删除
                Files.move(file.toPath(), Paths.get(Config.output_file_path + inFileName));
                Main.logger.log(Level.INFO, "[客户端]文件接收完成: " + inFileName);
            } else {
                Main.warning("[客户端]文件接收中断! 进度:" + inProgress + "/" + inSize);
            }
            isInEnd.set(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
