package com.thesis.ahmed.datacollector;

/**
 * Created by Ahmed on 10/2/2016.
 */

public class Position {
    public boolean hand = false;
    public boolean pocket = false;
    public boolean moving = false;
    public boolean flat = false;
    public boolean face_down = false;
    public boolean ear = false;

    @Override
    public String toString(){
        String s = "";
        if (hand) s+="hand\n";
        if (pocket) s+="pocket\n";
        if (moving) s+="moving\n";
        if (flat) s+="flat\n";
        if (face_down) s+="face down\n";
        if (ear) s+="ear\n";

        return s;
    }
}
