package com.wxy.reversi;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class GameUI extends Application {
    // 懒得改了，代码写得太烂
    // todo 分离client，鼠标落在可下的位置时改变棋盘颜色
    public ChessBoard reversi;
    private Pane pchess;
    private StackPane ppaint;
    // 用户函数repaint()，在上面画棋盘和棋子
    private Socket skt;
    private DataInputStream input;
    private DataOutputStream output;

    private int xo, yo;
    private boolean waiting;
    // 用于等待用户下棋
    private int curplayer;

    private TextField Tplayer, Twhite, Tblack, Taddr, Tport;
    private Button start, reset;
    private TextArea Message;

    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;

        reversi = new ChessBoard();
        waiting = true;
        curplayer = 0;

        ppaint = new StackPane();
        pchess = new ChessPane();
        ppaint.getChildren().add(new LinePane());
        ppaint.getChildren().add(pchess);

        ppaint.setOnMousePressed(e -> {
            if (!reversi.Check() || reversi.GetPlayer() != curplayer) {
                return;
            }
            // 如果游戏结束或者不是我的回合则点击棋盘没用
            int x = (int) e.getX();
            int y = (int) e.getY();
            if (x >= 0 && x <= 400 && y >= 0 && y <= 400) {
                x = x / 50;
                y = y / 50;
                // 返回1证明坐标验证有效
                if (PressChess(x, y) == 1) {
                    waiting = false;
                    xo = x;
                    yo = y;
                }
            }
        });

        Tplayer = new TextField("白棋");
        Twhite = new TextField("2");
        Tblack = new TextField("2");
        Taddr = new TextField("localhost");
        Tport = new TextField("8000");
        Tplayer.setPrefColumnCount(4);
        Tplayer.setEditable(false);
        Twhite.setPrefColumnCount(4);
        Twhite.setEditable(false);
        Tblack.setPrefColumnCount(4);
        Tblack.setEditable(false);
        Taddr.setPrefColumnCount(6);
        Tport.setPrefColumnCount(4);
        start = new Button("连接");
        reset = new Button("重置");
        GridPane gp = new GridPane();
        gp.setAlignment(Pos.CENTER);
        gp.setPadding(new Insets(5, 5, 5, 5));
        gp.setHgap(10);
        gp.setVgap(10);
        gp.add(new Label("当前:"), 0, 0);
        gp.add(Tplayer, 1, 0);
        gp.add(new Label("白棋:"), 0, 1);
        gp.add(Twhite, 1, 1);
        gp.add(new Label("黑棋:"), 0, 2);
        gp.add(Tblack, 1, 2);
        gp.add(new Label("IP:"), 0, 3);
        gp.add(Taddr, 1, 3);
        gp.add(new Label("端口:"), 0, 4);
        gp.add(Tport, 1, 4);
        gp.add(start, 0, 5);
        gp.add(reset, 1, 5);

        start.setOnAction(e -> {
            if (curplayer != 0) {
                Message.appendText("游戏已经开始！\n");
                return;
            }
            connectToServer();
        });
        reset.setOnAction(e -> {
            if (reversi.Check()) {
                Message.appendText("游戏尚未结束，不能重启游戏\n");
                return;
            }
            try {
                skt.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            reversi.Reset(1);
            primaryStage.setTitle("黑白棋");
            curplayer = 0;
            repaint();
            try {
                input.close();
                output.close();
                skt.close();
            } catch (IOException ex) {
                Message.appendText(ex.toString());
            }
        });
        VBox bvbox = new VBox();
        bvbox.setSpacing(5);
        bvbox.setAlignment(Pos.CENTER);
        Message = new TextArea("欢迎来到黑白棋游戏\n");
        Message.setEditable(false);
        Message.setPrefColumnCount(10);
        Message.setPrefRowCount(6);
        Message.setWrapText(true);
        bvbox.getChildren().addAll(gp, new ScrollPane(Message));

        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(10, 10, 10, 10));
        pane.setCenter(ppaint);
        pane.setRight(bvbox);

        Scene scene = new Scene(pane, 600, 420);
        primaryStage.setTitle("黑白棋");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void repaint() {
        Platform.runLater(() -> {
            ppaint.getChildren().remove(pchess);
            pchess = new ChessPane();
            ppaint.getChildren().add(pchess);
            if (reversi.GetPlayer() == 1) {
                Tplayer.setText("白棋");
            } else {
                Tplayer.setText("黑棋");
            }
        });
    }

    private int PressChess(int x, int y) {
        if (!reversi.PlaceChess(x, y)) {
            Message.appendText("不能下在当前位置\n");
            return 0;
        }
        repaint();
        int i = reversi.GetBlack();
        int j = reversi.GetWhite();
        Platform.runLater(() -> {
            Tblack.setText(i + "");
            Twhite.setText(j + "");
        });
        if (!reversi.Check()) {
            String txt;
            if (i > j) {
                txt = "游戏结束，黑方胜利";
            } else if (i < j) {
                txt = "游戏结束，白方胜利";
            } else {
                txt = "游戏结束，平局胜利";
            }
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle(txt);
                alert.setHeaderText(null);
                alert.setContentText(String.format("黑棋%d个，白棋%d个\n", i, j));
                alert.show();
            });
        }
        return 1;
    }

    private void connectToServer() {
        try {
            skt = new Socket(Taddr.getText(), Integer.parseInt(Tport.getText()));
            input = new DataInputStream(skt.getInputStream());
            output = new DataOutputStream(skt.getOutputStream());
        } catch (Exception ex) {
            Platform.runLater(() -> {
                Message.appendText(ex.toString());
            });
        }
        new Thread(() -> {
            try {
                int p = input.readInt();
                if (p == 1) {
                    curplayer = 1;
                    Platform.runLater(() -> {
                        stage.setTitle("用户1：白棋");
                        Message.appendText("连接成功，等待另一用户\n");
                    });
                    input.readInt();
                    Platform.runLater(() -> {
                        Message.appendText("玩家2连接成功，游戏开始\n");
                    });
                } else if (p == -1) {
                    curplayer = -1;
                    Platform.runLater(() -> {
                        stage.setTitle("用户2：黑棋");
                        Message.appendText("连接成功，等待玩家1开始\n");
                    });
                }
                reversi.Start();
                while (reversi.Check()) {
                    if (reversi.GetPlayer() == curplayer) {
                        // 只有在我的回合才能移动棋子(发送坐标)
                        waitForPlayerAction();
                        sendMove();
                    }
                    //todo add lock on value xo,yo
                    if (reversi.GetPlayer() != curplayer) {
                        // 只要是对方的回合就要一直等待坐标
                        receiveInfoFromServer();
                    }
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Message.appendText(ex.toString());
                });
            }
        }).start();
    }

    private void waitForPlayerAction() throws InterruptedException {
        // 等待当前用户下棋
        while (waiting) {
            Thread.sleep(100);
        }
        waiting = true;
    }

    private void sendMove() throws IOException {
        output.writeInt(xo);
        output.writeInt(yo);
        Platform.runLater(() -> {
            Message.appendText(String.format("发送坐标(%d,%d)\n", xo, yo));
        });
    }

    private void receiveInfoFromServer() throws IOException {
        int r = input.readInt();
        int c = input.readInt();
        Platform.runLater(() -> {
            Message.appendText(String.format("收到坐标(%d,%d)\n", r, c));
        });
        PressChess(r, c);
    }

    class LinePane extends Pane {
        // 画棋盘
        public LinePane() {
            Rectangle r = new Rectangle(0, 0, 400, 400);
            r.setStroke(Color.BLACK);
            r.setFill(Color.GREY);
            getChildren().add(r);
            for (int i = 1; i < 8; i++) {
                Line line1 = new Line(0, i * 50, 400, i * 50);
                Line line2 = new Line(i * 50, 0, i * 50, 400);
                line1.setStroke(Color.WHITE);
                line2.setStroke(Color.WHITE);
                getChildren().addAll(line1, line2);
            }
        }
    }

    class ChessPane extends Pane {
        // 画棋子
        public ChessPane() {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    Circle c = new Circle(25 + i * 50, 25 + j * 50, 20);
                    if (reversi.GetChess(i, j) == -1) {
                        c.setFill(Color.BLACK);
                    } else if (reversi.GetChess(i, j) == 1) {
                        c.setFill(Color.WHITE);
                    } else {
                        continue;
                    }
                    getChildren().add(c);
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}