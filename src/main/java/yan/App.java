package yan;

import javafx.animation.FadeTransition;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;
import yan.ui.ClientPane;
import yan.ui.ServerPane;

public class App extends javafx.application.Application {

    public static boolean closing = false;

    @Override
    public void init() throws Exception {
        // TODO 读取配置文件
    }

    // 页面
    Node[] nodes = new Node[]{new ServerPane(), new ClientPane()};
    // 名称
    String[] nodeNames = new String[]{"发送文件", "接收文件"};

    // 当前显示的界面
    Node recent;

    // 用于事件传输
    static Node root;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 添加组件
        // 为整个页面布局的面板
        AnchorPane anchorPane = new AnchorPane();
        root = anchorPane;

        // 左侧切换条
        VBox tabBox = new VBox(2);
        tabBox.setPrefWidth(100);
        tabBox.setFillWidth(true);
        // 保持在左侧
        AnchorPane.setLeftAnchor(tabBox, 0.0);
        AnchorPane.setTopAnchor(tabBox, 0.0);
        AnchorPane.setBottomAnchor(tabBox, 0.0);
        // 选项
        for (int i = 0; i < nodes.length; i++) {
            // 初始化按钮
            Button button = new Button(nodeNames[i]);
            button.setPrefHeight(20);
            button.setPrefWidth(100);
            // 设置事件
            int finalI = i;
            button.setOnAction(event -> {
                if (nodes[finalI] == recent) return;
                // 当前界面淡出
                if (recent != null) {
                    FadeTransition ft = new FadeTransition(Duration.seconds(0.5), recent);
                    ft.setFromValue(1);
                    ft.setToValue(0);
                    Node last = recent;
                    ft.setOnFinished(event1 -> anchorPane.getChildren().remove(last));
                    ft.play();
                }
                // 新界面
                recent = nodes[finalI];
                anchorPane.getChildren().add(recent);
                AnchorPane.setLeftAnchor(recent, 100.0);
                AnchorPane.setRightAnchor(recent, 0.0);
                AnchorPane.setTopAnchor(recent, 0.0);
                AnchorPane.setBottomAnchor(recent, 0.0);
                FadeTransition ft = new FadeTransition(Duration.seconds(.5), recent);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            });
            // 添加
            tabBox.getChildren().add(button);
        }

        anchorPane.getChildren().add(tabBox);
        anchorPane.getChildren().add(new Line(100, 0, 100, 400));

        // 当窗口关闭时停止发送/接收
        primaryStage.setOnCloseRequest(event -> {
            // 标记
            closing = true;
            Connection.stop();
            Main.info("窗口即将关闭");
        });

        primaryStage.setScene(new Scene(anchorPane, 600, 400, Color.LIGHTGRAY));
        primaryStage.show();
    }

    public static void fireEvent(Event event) {
        Event.fireEvent(root, event);
    }
}
