package com.yourname.got.logic.model;

import java.io.Serializable;

public class Position implements Serializable {
    protected static final long serialVersionUID = -5374670953810941394L;

    private int x, y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;

    }
    public Position(){
    }

    public int getX() { return this.x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return this.y; }
    public void setY(int y) { this.y = y; }
    public  int  distance(Position origin){
        System.out.println(this+"->"+origin);
        return Math.abs(x-origin.getX())+ Math.abs(y-origin.getY());
    }
    public  Double  realDistance(Position origin){

        return Math.pow((Math.pow((x-origin.getX()),2)+ Math.pow((y-origin.getY()),2)),0.5);
    }


    @Override
    public String toString() {
        return "Position{" + "x=" + x + ", y=" + y + '}';
    }
}

