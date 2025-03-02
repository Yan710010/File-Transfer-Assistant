package yan.ui;

import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import yan.Config;
import yan.Connection;

/**
 * 用以显示客户端文件发送页面的面板
 */
public class ClientPane extends Pane {
    public ClientPane() {
        super();
        setBackground(Background.fill(Color.GREEN));
        // 标题
        Label titleLabel = new Label("接收文件");
        titleLabel.setFont(Font.font(20));
        titleLabel.setTextFill(Color.BLACK);
        titleLabel.setLayoutX(20);

        // host输入
        TextField hostTextField = new TextField("localhost");
        hostTextField.setPromptText("填写文件发送者处显示的ip地址");
        Tooltip hostTooltip = new Tooltip("填写文件发送者处显示的ip地址");
        hostTooltip.setShowDelay(Duration.seconds(.5));
        hostTextField.setTooltip(hostTooltip);
        hostTextField.setLayoutX(20);
        hostTextField.setLayoutY(50);
        hostTextField.setPrefWidth(200);
        hostTextField.setPrefHeight(20);
        hostTextField.setFont(Font.font(18));
        hostTextField.setOnMouseExited(event -> {
            if (hostTextField.getText().isEmpty()) {
                titleLabel.requestFocus();
            }
        });// 文本框为空时移除焦点

        // port输入
        TextField portTextField = new TextField("" + Config.port);
        portTextField.setPromptText("" + Config.port);
        Tooltip portTooltip = new Tooltip("填写文件发送者使用的端口");
        portTooltip.setShowDelay(Duration.seconds(.5));
        portTextField.setTooltip(portTooltip);
        portTextField.setLayoutX(300);
        portTextField.setLayoutY(50);
        portTextField.setPrefWidth(80);
        portTextField.setPrefHeight(20);
        portTextField.setFont(Font.font(18));
        portTextField.setOnMouseExited(event -> {
            if (portTextField.getText().isEmpty()) {
                titleLabel.requestFocus();
            }
        });// 同上喵

        // 文件接收按钮
        Button fileReceiveButton = new Button("接收文件");
        fileReceiveButton.setLayoutX(20);
        fileReceiveButton.setLayoutY(100);
        fileReceiveButton.setPrefWidth(100);
        fileReceiveButton.setPrefHeight(50);

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

        // 接收文件
        fileReceiveButton.setOnAction(event -> {
            // 警告
            Alert alert = null;
            // 获取所填写的数据
            String host = hostTextField.getText();
            int port = -1;
            try {
                if (portTextField.getText().isEmpty()) port = Config.port;
                else port = Integer.parseInt(portTextField.getText());
                if (port < 0 || port > 65535) throw new IllegalArgumentException();
            } catch (NumberFormatException e) {
                // 表示数字格式有误
                alert = new Alert(Alert.AlertType.ERROR, "端口格式有误!\n是否使用默认端口发送?", ButtonType.OK, ButtonType.CANCEL);
            } catch (IllegalArgumentException e) {
                // 表示数字范围错误
                port = -1;
                alert = new Alert(Alert.AlertType.ERROR, "端口超出范围!(0-65535)\n是否使用默认端口进行发送?", ButtonType.OK, ButtonType.CANCEL);
            }
            // 如果错误
            if (alert != null) {
                Alert finalAlert = alert;
                alert.setOnCloseRequest(event1 -> {
                    if (finalAlert.getResult() == ButtonType.OK) {
                        portTextField.setText("" + Config.port);
                        fileReceiveButton.fire();
                    }
                });
                alert.show();
                return;
            }

            // 准备从给定的host和port接收文件
            // 连接
            if (!Connection.client(host, port)) {
                // 表示连接失败
                new Alert(Alert.AlertType.ERROR, "无法连接至目标: " + host + ":" + port + "\n" +
                        "请检查地址是否正确", ButtonType.OK).show();
                return;
            }
            // ui变化
            fileReceiveButton.setDisable(true);
            progressBar.setVisible(true);
            progressLabel.setVisible(true);
            // 进度标签文字显示
            progressLabel.textProperty().unbind();
            progressLabel.setText("等待连接");
            // 当接收开始时将标签绑定至进度
            Connection.clientTask.setOnRunning(event1 -> {
                progressBar.progressProperty().bind(Connection.clientTask.progressProperty());
                progressLabel.textProperty().bind(Connection.clientTask.messageProperty());
            });
            // 当接收结束时
            EventHandler<WorkerStateEvent> onEnd = event1 -> {
                // ui更新
                fileReceiveButton.setDisable(false);
                progressBar.setVisible(false);
                progressLabel.setVisible(false);
                // 显示提示
                if (Connection.clientTask.getState() == Worker.State.SUCCEEDED)
                    new Alert(Alert.AlertType.INFORMATION, "文件接收完毕", ButtonType.OK).show();
                else new Alert(Alert.AlertType.ERROR, "文件接收失败!", ButtonType.OK);
            };
            Connection.clientTask.setOnSucceeded(onEnd);
            Connection.clientTask.setOnFailed(onEnd);
        });

        getChildren().addAll(titleLabel, hostTextField, portTextField, fileReceiveButton, progressBar, progressLabel);
    }
}
