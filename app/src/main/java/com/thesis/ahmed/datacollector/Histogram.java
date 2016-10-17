package com.thesis.ahmed.datacollector;

import android.graphics.Bitmap;

/**
 * Created by Ahmed on 10/16/2016.
 */

public class Histogram {
        int[] hist;
        int pixelCount = 0;
        Histogram(Bitmap bmp){
            hist = new int[256];
            int height = bmp.getHeight();
            int width = bmp.getWidth();
            int px;
            int avgPixVal;
            for(int h = 0; h < height; h += (height / 50)){
                for(int w = 0; w < width; w += (width / 50)){
                    px = bmp.getPixel(w,h);
                    avgPixVal =(((px & 0xFF) +
                            ((px >> 8) & 0xFF)+
                            ((px >> 16) & 0xFF))/3);
                    hist[avgPixVal] += 1;
                    pixelCount += 1;
                }
            }
        }

        public int percentageLessThanVal(int val){
            assert val <= 256;
            int sum = 0;
            for(int i = 0; i < val; i++){
                sum += hist[i];
            }
            return sum*100/pixelCount;
        }

        public String toString(){
            String s = "[";
            for(int b : hist){
                s+=b+",";
            }
            s+="]";
            return s;
        }
}
