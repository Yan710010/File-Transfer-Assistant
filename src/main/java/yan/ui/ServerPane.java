package yan.ui;

import javafx.animation.TranslateTransition;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.effect.Glow;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import yan.Config;
import yan.Connection;
import yan.Main;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class ServerPane extends Pane {
    public ServerPane() {
        super();
        setBackground(Background.fill(Color.AQUA));
        // 标题
        Label titleLabel = new Label("发送文件");
        titleLabel.setFont(Font.font(20));
        titleLabel.setTextFill(Color.BLACK);
        titleLabel.setLayoutX(20);

        // 文件选择
        TextArea fileArea = new TextArea();
        fileArea.setPromptText("将文件拖拽至此 或右键选择文件/文件夹 或手动填写位置");// 提示文字
        fileArea.setLayoutX(20);
        fileArea.setLayoutY(50);
        fileArea.setPrefWidth(400);
        fileArea.setPrefHeight(50);
        fileArea.setOnContextMenuRequested(Event::consume);// 取消默认的右键编辑菜单
        fileArea.setOnDragOver(event -> {
            // 如果拖动来源不是自身
            if (event.getGestureSource() != fileArea) {
                // 允许进行移动和拷贝
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                event.consume();
            }
        });// 当拖动进入文本框时允许拖放
        fileArea.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            // 判断类型
            if (dragboard.hasFiles()) {
                // 如果是文件则设置为文件路径
                fileArea.setText(dragboard.getFiles().getFirst().getPath());
            } else if (dragboard.getString() != null) {
                fileArea.setText(dragboard.getString());
            }
            event.consume();
        });// 拖放文件或者文本
        fileArea.setOnMouseExited(event -> {
            if (fileArea.getText().isEmpty()) {
                titleLabel.requestFocus();
            }
        });// 若为空,在鼠标移出时失去焦点


        // 发送文件的文本框右键时出现的选择菜单
        ContextMenu fileAreaContextMenu = new ContextMenu();
        // 清空选择
        MenuItem item_clear = new MenuItem("清空已选文件");
        item_clear.setOnAction(event -> fileArea.clear());
        // 分割线
        Separator separator = new Separator(Orientation.HORIZONTAL);// 横向
        separator.setPrefHeight(5);
        // 选择文件
        MenuItem item_chooseFile = new MenuItem("选择文件");
        item_chooseFile.setOnAction(e -> {
            // 弹出文件选择器
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择需要发送的文件");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));// 以用户目录为起始
//            File file = fileChooser.showOpenDialog(getScene().getWindow());
            List<File> files = fileChooser.showOpenMultipleDialog(getScene().getWindow());
            if (files != null) {// 如果选择的文件非空
                // 向文本框中添加文件
                StringBuilder text = new StringBuilder(fileArea.getText());
                // 如果没有换行的话就换个行
                if (!text.isEmpty() && !text.toString().endsWith("\n")) text.append("\n");
                for (File file : files) text.append(file.getPath()).append("\n");
                fileArea.setText(text.toString());
            }
        });
        // 选择文件夹
        MenuItem item_chooseDir = new MenuItem("选择文件夹");
        item_chooseDir.setOnAction(e -> {
            // 弹出文件夹选择器
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("选择需要发送的文件夹");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File dir = directoryChooser.showDialog(getScene().getWindow());
            // 如果选择的文件夹非空
            if (dir != null) {
                // 添加
                String text = fileArea.getText();
                if (text.isEmpty() || text.endsWith("\n"))
                    fileArea.setText(text + dir.getPath());
                else
                    fileArea.setText(text + "\n" + dir.getPath());
            }
        });

        fileAreaContextMenu.getItems().addAll(item_clear, item_chooseFile, item_chooseDir);
        fileArea.setContextMenu(fileAreaContextMenu);

        // 文件发送端口
        TextField portEditor = new TextField("" + Config.port);
        portEditor.setPromptText("" + Config.port);
        portEditor.setLayoutX(20);
        portEditor.setLayoutY(120);
        portEditor.setPrefWidth(80);
        portEditor.setPrefHeight(50);

        // 文件发送按钮
        Button fileSendButton = new Button("发送文件");
        fileSendButton.setLayoutX(220);
        fileSendButton.setLayoutY(120);
        fileSendButton.setPrefWidth(100);
        fileSendButton.setPrefHeight(50);

        // 进度条
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setLayoutX(20);
        progressBar.setLayoutY(180);
        progressBar.setPrefWidth(400);
        progressBar.setPrefHeight(10);
        progressBar.setVisible(false);// 初始不显示
        // 进度文字
        Label progressLabel = new Label();
        progressLabel.setLayoutX(20);
        progressLabel.setLayoutY(190);
        progressLabel.setVisible(false);

        // 显示本地ip
        Label ipLabel = new Label("点击以显示本地ip");
        ipLabel.setLayoutX(20);
        ipLabel.setLayoutY(350);
        ipLabel.setFont(Font.font(15));
        ipLabel.setTextFill(Color.BLACK);
        ipLabel.setEffect(new Glow(1));
        ipLabel.setOnMouseClicked(event -> {
            // 跳一下
            TranslateTransition tt = new TranslateTransition(Duration.seconds(0.06), ipLabel);
            tt.setFromY(0);
            tt.setToY(-20);
            tt.setCycleCount(2);
            tt.setAutoReverse(true);
            tt.play();
            try {
                InetAddress address = Main.getLocalHostLANAddress();
                ipLabel.setText(String.format("主机名称: %-40s IP地址: %s", address.getHostName(), address.getHostAddress()));
            } catch (UnknownHostException e) {
                ipLabel.setText("无法获取ip地址,请检查网络连接");
            }
        }); // 获取本地ip

        // 发送文件
        fileSendButton.setOnAction(event -> {
            // 检查文件是否存在
            List<File> files = new ArrayList<>();
            // 问题列表
            List<String> errorPaths = new ArrayList<>();
            // 获取文件列表
            fileArea.getText().lines().forEach(s -> {
                if (s.isEmpty()) return;
                File f = new File(s);
                // 判断存在
                if (f.exists())
                    files.add(f);
                else
                    errorPaths.add(s);
            });

            // 如果有不存在的
            if (!errorPaths.isEmpty()) {
                // 提示文件不存在
                StringBuilder error = new StringBuilder("下列文件不存在:\n");
                errorPaths.forEach(s -> error.append(s).append("\n"));
                new Alert(Alert.AlertType.ERROR, error.toString(), ButtonType.CLOSE).show();
                return;
            }

            // 对于发送端口的检查
            // 提示
            Alert alert = null;
            // 检查填写端口是否正确
            int port = -1;
            try {
                if (portEditor.getText().isEmpty()) port = Config.port;
                else port = Integer.parseInt(portEditor.getText());
                if (port < 0 || port > 65535) {
                    throw new IllegalArgumentException();
                }
            } catch (NumberFormatException e) {
                // 说明格式错误
                alert = new Alert(Alert.AlertType.ERROR, "端口格式错误!\n是否使用默认端口进行发送?", ButtonType.OK, ButtonType.CANCEL);
            } catch (IllegalArgumentException e) {
                port = -1;
                alert = new Alert(Alert.AlertType.ERROR, "端口超出范围!(0-65535)\n是否使用默认端口进行发送?", ButtonType.OK, ButtonType.CANCEL);
            }
            // 如果有问题
            if (alert != null) {
                Alert finalAlert = alert;
                alert.setOnCloseRequest(event1 -> {
                    if (finalAlert.getResult() == ButtonType.OK) {
                        portEditor.setText("" + Config.port);
                        fileSendButton.fire();
                    }
                });
                alert.show();
                return;
            }

            // 怎么检查就写了这么多...

            // 开启连接
            if (!Connection.server(port, files.toArray(File[]::new))) return;
            // 成功开始连接了呢
            // 禁用发送按钮
            fileSendButton.setDisable(true);
            // 展示进度条
            progressBar.progressProperty().unbind();
            progressBar.progressProperty().bind(Connection.serverTask.progressProperty());
            progressBar.setVisible(true);
            Connection.serverTask.setOnRunning(event1 ->
                    progressLabel.textProperty().bind(Connection.serverTask.messageProperty()));// 开始运行后将将标签文字绑定至task
            Connection.serverTask.setOnSucceeded(event1 -> {
                progressBar.setVisible(false);
                progressLabel.setVisible(false);
                new Alert(Alert.AlertType.INFORMATION, "文件传输完毕").show();
                fileSendButton.setDisable(false);
            });
            Connection.serverTask.setOnFailed(event1 -> {
                progressBar.setVisible(false);
                progressLabel.setVisible(false);
                new Alert(Alert.AlertType.WARNING, "文件传输失败!").show();
                fileSendButton.setDisable(false);
            });
            // 展示进度文字
            progressLabel.setVisible(true);
            progressLabel.textProperty().unbind();
            progressLabel.setText("等待连接");
        });

        getChildren().addAll(titleLabel, fileArea, portEditor, fileSendButton, progressBar, progressLabel, ipLabel);
    }
}
