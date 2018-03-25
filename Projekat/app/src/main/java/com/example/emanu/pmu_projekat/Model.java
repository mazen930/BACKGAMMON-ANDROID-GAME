package com.example.emanu.pmu_projekat;

import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by emanu on 3/7/2018.
 */

public class Model {
    public static final String WAITING_FOR_MOVE = "WAITING_FOR_MOVE";
    public static final String WAITING_FOR_ROLL = "WAITING_FOR_ROLL";
    private Paint black = new Paint();
    private Paint white = new Paint();
    private MyImageView myImageView;
    private int imageViewHeight;
    private int imageViewWidth;

    private Player player1;
    private Player player2;
    private Player playerOnTurn;

    private Checker currentChecker;

    private Dice dice1;
    private Dice dice2;
    private Dice dice3;
    private Dice dice4;
    private List<Integer> availableMoves;
    private String gameStatus;
    private List<Triangle> availablePositions;
    private List<Checker> sourceTriangle;
    private int sourceTriangleIndex;
    private int destinationTriangleIndex;
    private int oldX;
    private int oldY;
    private boolean doubles;
    private List<Dice> dices = new ArrayList<>();
    private int usedDices;
    private Player opponent;

    public static final int SHAKE_TIMEOUT = 500;
    public static final int SPEED_THRESHOLD = 600;
    public static final int DIFFTIME_THRESHOLD = 100;
    private long lastTime = 0;
    private float lastX;
    private float lastY;
    private float lastZ;
    private long lastShakeTime;

    private MediaPlayer mediaPlayer;

    private int BLABLA;

    public Model(String player1name, String player2name, boolean isPlayer1Black, boolean isComputer) {
        black.setColor(Color.BLACK);
        white.setColor(Color.WHITE);
        player1 = new Human(player1name, isPlayer1Black ? black : white);
        if(isComputer)
            player2 = new Computer(player2name, isPlayer1Black ? white : black);
        else
            player2 = new Human(player2name, isPlayer1Black ? white : black);
        playerOnTurn = player2;
        gameStatus = WAITING_FOR_ROLL;

        dices.add(dice3 = new Dice(true, true));
        dices.add(dice4 = new Dice(false, true));
        dices.add(dice1 = new Dice(true, false));
        dices.add(dice2 = new Dice(false, false));

        mediaPlayer = MediaPlayer.create(StartedGameActivity.getAppContext(), R.raw.shaking_dice);
        mediaPlayer.setLooping(true);
    }

    public int selectChecker(int x, int y){
        List<List<Checker>> triangles = playerOnTurn.getTriangles();

        if(playerOnTurn.eatenCheckers.size() == 0){
            for(int i = 0; i < 24; i++){
                if(triangles.get(i).size() > 0){
                    int topCheckerIndex = triangles.get(i).size();
                    Checker topChecker = triangles.get(i).get(topCheckerIndex - 1);
                    if(distance(x, topChecker.getX(), y, topChecker.getY()) <= Checker.RADIUS){
                        currentChecker = topChecker;
                        sourceTriangle = triangles.get(i);
                        sourceTriangleIndex = i;
                        oldX = topChecker.getX();
                        oldY = topChecker.getY();
                        return i;
                    }
                }
            }
        }
        else{
            Checker topChecker = playerOnTurn.eatenCheckers.get(playerOnTurn.eatenCheckers.size() - 1);
            if(distance(x, topChecker.getX(), y, topChecker.getY()) <= Checker.RADIUS){
                currentChecker = topChecker;
                sourceTriangle = playerOnTurn.eatenCheckers;
                if(playerOnTurn.isWhite())
                    sourceTriangleIndex = -1;
                else
                    sourceTriangleIndex = 24;
                oldX = topChecker.getX();
                oldY = topChecker.getY();
                return sourceTriangleIndex;
            }
        }
        return -1;
    }

    public void findSpots(int sourceIndex){

        if(playerOnTurn != player1)
            opponent = player1;
        else
            opponent = player2;

        availablePositions = new ArrayList<>();
        for (int i = 0; i < availableMoves.size(); i++){
            if(playerOnTurn.getColor().getColor() == Color.WHITE){
                if((availableMoves.get(i) + sourceIndex) < 24 && opponent.getTriangles().get(availableMoves.get(i) + sourceIndex).size() < 2){
                    Triangle newPosition = new Triangle(availableMoves.get(i) + sourceIndex, imageViewHeight, imageViewWidth);
                    availablePositions.add(newPosition);
                }
            }
            else{
                if((sourceIndex - availableMoves.get(i)) >= 0 && opponent.getTriangles().get(sourceIndex - availableMoves.get(i)).size() < 2){
                    Triangle newPosition = new Triangle(sourceIndex - availableMoves.get(i), imageViewHeight, imageViewWidth);
                    availablePositions.add(newPosition);
                }
            }
        }
    }

    public void onActionDown(int x, int y) {
//        if(gameStatus.equals(WAITING_FOR_MOVE)){

            int sourceTriangleIndex = selectChecker(x, y);
            if(currentChecker == null) return;


            findSpots(sourceTriangleIndex);
//        }
    }

    public void onActionMove(int x, int y) {
        if(currentChecker != null){
            currentChecker.setX(x);
            currentChecker.setY(y);
        }
    }

    public void onActionUp(int x, int y){
        if(currentChecker != null){
            currentChecker.bearOff(0, BLABLA++);
            currentChecker = null;
        }
        if(currentChecker != null){
            for (int i = 0; i < availablePositions.size(); i++) {
                Triangle triangle = availablePositions.get(i);
                int x1 = triangle.getX1();
                int y1 = triangle.getY1();
                int x2 = triangle.getX2();
                int y2 = triangle.getY2();
                int x3 = triangle.getX3();
                int y3 = triangle.getY3();
                if(isInAvailableSpot(x, y, x1, y1, x2, y2, x3, y3)){
                    opponent.eatOpponentsChecker(triangle.getTriangleIndex());
                    sourceTriangle.remove(currentChecker); //remove from old triangle
                    int position = playerOnTurn.getTriangles().get(triangle.getTriangleIndex()).size(); //position index in new triangle
                    playerOnTurn.getTriangles().get(triangle.getTriangleIndex()).add(currentChecker); //add checker to new triangle
                    currentChecker.setOnPosition(imageViewHeight, imageViewWidth, triangle.getTriangleIndex(), position); //change x,y coords
                    destinationTriangleIndex = triangle.getTriangleIndex();
                    updateAvailableMoves();
                    break;
                }
                else{
                    currentChecker.setX(oldX);
                    currentChecker.setY(oldY);
                }
            }

            if(availablePositions.size() == 0){
                currentChecker.setX(oldX);
                currentChecker.setY(oldY);
            }

            availablePositions = null;
            currentChecker = null;
        }
    }

    public void updateAvailableMoves(){
        int positionDifference = Math.abs(destinationTriangleIndex - sourceTriangleIndex);

        int moveToDelete = -1;
        for(int i = 0; i < availableMoves.size(); i++){
            if(availableMoves.get(i).intValue() == positionDifference){
                moveToDelete = i;
                break;
            }
        }

        if(doubles){
            int max = availableMoves.size();
            for(int i = 0; i <= moveToDelete; i++){ //depending on what move to delete, result is the same number of used dices
                dices.get(usedDices++).incOpacity();
            }
            while((moveToDelete + 1) > 0){
                availableMoves.remove(max - 1);
                max--;
                moveToDelete--;
            }
        }
        else{
            if(availableMoves.size() == 3){
                switch (moveToDelete){
                    case 0:
                        availableMoves.remove(2);
                        availableMoves.remove(0);
                        dice1.incOpacity();
                    break;
                    case 1:
                        availableMoves.remove(2);
                        availableMoves.remove(1);
                        dice2.incOpacity();
                        break;
                    case 2:
                        availableMoves.remove(2);
                        availableMoves.remove(1);
                        availableMoves.remove(0);
                        dice1.incOpacity();
                        dice2.incOpacity();
                        break;
                }
            }
            else{ //size == 1
                availableMoves.remove(0);
                dice1.incOpacity();
                dice2.incOpacity();
            }
        }
    }

    public boolean isInAvailableSpot(int px, int py, int x1, int y1, int x2, int y2, int x3, int y3){
        double w1 = (x1*(y3 - y1) + (py - y1)*(x3 - x1) - px*(y3 - y1))/((y2 - y1)*(x3 - x1) - (x2 - x1)*(y3 - y1));
        double w2 = (py - y1 - w1*(y2 - y1))/(y3 - y1);

        return w1 >= 0 && w1 >= 0 && (w1 + w2) <= 1;

//        int as_x = px-x1;
//        int as_y = py-y1;
//
//        boolean s_ab = (x2-x1)*as_y-(y2-y1)*as_x > 0;
//
//        if((x3-x1)*as_y-(y3-y1)*as_x > 0 == s_ab) return false;
//
//        if((x3-x2)*(py-y2)-(y3-y2)*(px-x2) > 0 != s_ab) return false;

//        return true;
    }

    private double distance(int x1, int x2, int y1, int y2){
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public void roll(){
        switchPlayerOnTurn();
        dice3.setShow(false);
        dice4.setShow(false);
        dice1.roll();
        dice2.roll();
        dice1.setShow(true);
        dice2.setShow(true);
        doubles = false;
        availableMoves = new ArrayList<>();
        usedDices = 0;
        for(int i = 0; i < 4; i++){
            dices.get(i).resetOpacity();
        }
        if(dice1.getValue() == dice2.getValue()){ //when player gets doubles, that counts as 4x
            availableMoves.add(dice1.getValue());
            availableMoves.add(dice1.getValue() * 2);
            availableMoves.add(dice2.getValue() * 3);
            availableMoves.add(dice2.getValue() * 4);
            doubles = true;
            dice3.setValue(dice1.getValue());
            dice3.setShow(true);
            dice4.setValue(dice1.getValue());
            dice4.setShow(true);
        }
        else{
            availableMoves.add(dice1.getValue());
            availableMoves.add(dice2.getValue());
            availableMoves.add(dice1.getValue() + dice2.getValue());
        }
    }

    public void switchPlayerOnTurn(){
        if(playerOnTurn == player1)
            playerOnTurn = player2;
        else
            playerOnTurn = player1;
    }

    public void onNewValues(float values[], long time){
        long currentTime = android.os.SystemClock.uptimeMillis();

        if(currentTime - lastTime > DIFFTIME_THRESHOLD){
            long diffTime = currentTime - lastTime;
            lastTime = currentTime;
            float speed = Math.abs(values[0] + values[1] + values[2] - lastX - lastY - lastZ) / diffTime * 10000;

            if(speed > SPEED_THRESHOLD){
                if(!mediaPlayer.isPlaying())
                    mediaPlayer.start();

                lastShakeTime = currentTime;
            }

            lastX = values[0];
            lastY = values[1];
            lastZ = values[2];
        }

        if(currentTime - lastShakeTime > SHAKE_TIMEOUT){ //prestanak muckanja
            if(mediaPlayer.isPlaying()){
                mediaPlayer.pause();
                roll();
                myImageView.invalidate();
                return;
            }
        }
    }

    public void setHeightAndWidth(int h, int w) {
        this.imageViewHeight = h;
        this.imageViewWidth = w;
        player1.setHeightAndWidth(h, w);
        player2.setHeightAndWidth(h, w);
        dice1.setHeightAndWidth(h, w);
        dice2.setHeightAndWidth(h, w);
        dice3.setHeightAndWidth(h, w);
        dice4.setHeightAndWidth(h, w);
        player1.initCheckers();
        player2.initCheckers();

    }

    public List<Checker> getPlayer1Checkers(){
        return player1.getCheckers();
    }

    public List<Checker> getPlayer2Checkers(){
        return player2.getCheckers();
    }

    public MyImageView getMyImageView() {
        return myImageView;
    }

    public void setMyImageView(MyImageView myImageView) {
        this.myImageView = myImageView;
    }

    public Dice getDice1() {
        return dice1;
    }

    public Dice getDice2() {
        return dice2;
    }

    public Dice getDice3() {
        return dice3;
    }

    public Dice getDice4() {
        return dice4;
    }

    public List<Triangle> getAvailablePositions() {
        return availablePositions;
    }
}