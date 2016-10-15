package com.thesis.ahmed.datacollector;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageWriter;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static android.R.attr.bitmap;

/**
 * Created by Ahmed on 9/30/2016.
 */

public class CameraModule {
    CameraManager mCameraManager;
    MainActivity context;
    CameraDevice mCamera = null;
    Surface mSurface = null;
    ImageReader imgR;
    String folder = "";

    CameraModule(MainActivity context) {
        this.context = context;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public void setFolder(String folder){
        this.folder = folder;
    }

    private File createImageFile(String filename) throws IOException {
        // Create an image file name
        String imageFileName = filename + "_";
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File storageDir = new File(filepath+"/"+folder);
        if (storageDir != null) {
            Log.d("storage dir", storageDir.toString());
            storageDir.mkdirs();
        }
        File image = new File(storageDir, filename+System.currentTimeMillis()+".jpg");
        image.createNewFile();

        // Save a file: path for use with ACTION_VIEW intents
//        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    private boolean checkCamera(){
        return mCamera != null;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean openCamera(){
        Log.d("Camera Status", "in open camera");
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            return false;
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.CAMERA},
                    1);

        }
        try {
            String camID = mCameraManager.getCameraIdList()[0];

            mCameraManager.openCamera(camID, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {

                    context.updateStatus("Camera Opened");
                    Log.d("Camera Status:","opened");
                    mCamera = camera;
                    setupSurface();
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    context.updateStatus("Camera Disconnected");
                    Log.d("Camera Status:","disconnected, reopening ...");
                    mCamera = null;
                    mSurface = null;
                    openCamera();
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    context.updateStatus("Camera On Error");
                    Log.d("Camera Status:","onError");
                    mCamera = null;
                }
            }, context.handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupSurface(){
        CameraCharacteristics cc = null;
        try {
            String camID = mCameraManager.getCameraIdList()[0];
            cc = mCameraManager.getCameraCharacteristics(camID);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        StreamConfigurationMap configs = cc.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Log.d("Output formats",""+configs.getOutputFormats()[0] + " "+configs.getOutputFormats()[1] + " "+configs.getOutputFormats()[2]);

        Size s = configs.getOutputSizes(ImageFormat.JPEG)[0];
        Log.d("Size", s.getWidth()+","+s.getHeight());
        final ImageReader ir = ImageReader.newInstance(s.getWidth(), s.getHeight(), PixelFormat.RGBA_8888, 2);
        ir.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.d("ImgReader Size", ir.getWidth()+","+ir.getHeight());
                Image image = reader.acquireLatestImage();
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int offset = 0;
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * ir.getWidth();
// create bitmap
                Bitmap bitmap = Bitmap.createBitmap(ir.getWidth(), ir.getHeight(),Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
                image.close();
                Bitmap bm = Bitmap.createBitmap(bitmap, 0, ir.getHeight() - 500, 900, 500);

                try {
                    FileOutputStream fos = new FileOutputStream(createImageFile(""));
                    bm.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                };
            }
        }, context.handler);
        imgR = ir;
        mSurface = imgR.getSurface();
        setupCamera();
//        SurfaceView sv = new SurfaceView(context);
//        SurfaceHolder holder = sv.getHolder();
//        holder.setFixedSize(width, height);
//        holder.addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(SurfaceHolder holder) {
//                Log.d("Surface Status", "created");
//                mSurface = holder.getSurface();
//                assert mSurface != null;
//                setupCamera();
//            }
//
//            @Override
//            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//                Log.d("Surface Status", "Changed");
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//                Log.d("Surface Status", "Destroyed");
//            }
//        });
//        LinearLayout ll = (LinearLayout) context.findViewById(R.id.ll);
//        ll.addView(sv);
//        Log.d("Surface Status", ""+holder.isCreating());
//        Log.d("Surface Status", ""+holder.getSurface().isValid());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupCamera(){
        assert mCamera != null;
        Log.d("Camera Status", "setting up...");
        ArrayList<Surface> s = new ArrayList<Surface>();
        assert mSurface != null;
        assert mSurface.isValid();
        try {
            CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(mSurface);
            builder.set(CaptureRequest.CONTROL_AE_LOCK, false);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 6);

            final CaptureRequest request = builder.build();
            s.add(mSurface);
            Log.d("Camera Status", "Creating Capture Session...");
            mCamera.createCaptureSession(s, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        Log.d("Capture Status","Configuration Successful");
                        Log.d("AE PRECAPTURE TRIGGER", request.get(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER) + "");

                        session.capture(request, new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {

                                super.onCaptureCompleted(session, request, result);
                                context.updateStatus("Capture Successful");
                                Log.d("Capture Result", "Success");
                                mCamera.close();

                            }

                            @Override
                            public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                                super.onCaptureFailed(session, request, failure);
                                Log.d("Capture Result", "Failure");
                                context.updateStatus("Capture Failed");
                            }
                        }, context.handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.d("Capture Status","Configuration Failed");
                    mSurface = null;
                }
            }, context.handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


}
