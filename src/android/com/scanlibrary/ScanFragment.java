package com.scanlibrary;

import android.app.Activity;
import android.app.Fragment;
//import android.support.v4.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by jhansi on 29/03/15.
 */
public class ScanFragment extends Fragment {

    private Button scanButton;
    private ImageView sourceImageView;
    private FrameLayout sourceFrame;
    private PolygonView polygonView;
    private View view;
    private ProgressDialogFragment progressDialogFragment;
    private IScanner scanner;
    private Bitmap original;
    public FakeR fakeR;
    private static float container_width;
    private static float container_height;
    private static Activity cur_activity;
    public static String TAG="DOC_LOG";

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        fakeR = new FakeR(activity);
        if (!(activity instanceof IScanner)) {
            throw new ClassCastException("Activity must implement IScanner");
        }
        this.scanner = (IScanner) activity;
        cur_activity=activity;
//        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, cur_activity, baseLoaderCallback);
    }

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(cur_activity) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG,"onManagerConnected");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(fakeR.getId("layout", "sc_scan_fragment_layout"), container, false);
        init();
        return view;
    }

    public ScanFragment() {

    }

    private void init() {
        sourceImageView = (ImageView) view.findViewById(fakeR.getId("id", "sourceImageView"));
        scanButton = (Button) view.findViewById(fakeR.getId("id", "scanButton"));
        scanButton.setOnClickListener(new ScanButtonClickListener());
        sourceFrame = (FrameLayout) view.findViewById(fakeR.getId("id", "sourceFrame"));
        polygonView = (PolygonView) view.findViewById(fakeR.getId("id", "polygonView"));
        sourceFrame.post(new Runnable() {
            @Override
            public void run() {
                original = getBitmap();
                if (original != null) {
                    setBitmap(original);
                }
            }
        });
    }

    private Bitmap getBitmap() {
        Uri uri = getUri();
        try {
            Bitmap bitmap = Utils.getBitmap(getActivity(), uri);
            getActivity().getContentResolver().delete(uri, null, null);
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Uri getUri() {
        Uri uri = getArguments().getParcelable(ScanConstants.SELECTED_BITMAP);
        return uri;
    }

    private void setBitmap(Bitmap original) {
        Bitmap scaledBitmap = scaledBitmap(original, sourceFrame.getWidth(), sourceFrame.getHeight());
        sourceImageView.setImageBitmap(scaledBitmap);
        Bitmap tempBitmap = ((BitmapDrawable) sourceImageView.getDrawable()).getBitmap();
        Map<Integer, PointF> pointFs = getEdgePoints(tempBitmap);
        PointF p0=pointFs.get(0);
        PointF p1=pointFs.get(1);
        PointF p2=pointFs.get(2);
        PointF p3=pointFs.get(3);
        float width=0.0f,height=0.0f;
        container_width=scaledBitmap.getWidth();
        container_height=scaledBitmap.getHeight();
        if ( p1.x == p3.x ) {width=p1.x;container_width=p1.x;}
        if ( p2.y == p3.y ) {height=p2.y;container_height=p2.y;}

        double contourArea = ( ((p0.x*p1.y)-(p1.x*p0.y)) + ((p1.x*p3.y)-(p3.x*p1.y)) + ((p3.x*p2.y)-(p3.y*p2.x)) + ((p2.x*p0.y)-(p2.y*p0.x)) ) / 2;
//        Log.i(TAG,"New contour area="+contourArea);

        if ( contourArea < 40000 || ( (p0.x == 0.0 && p0.y== 0.0 ) && (p1.x == width && p1.y== 0.0 ) && (p2.x == 0.0 && p2.y== height ) && (p3.x == width && p3.y== height ) ) ) {
            pointFs = customDetection(tempBitmap);
        }

        polygonView.setPoints(pointFs);
        polygonView.setVisibility(View.VISIBLE);
        int padding = (int) getResources().getDimension(fakeR.getId("dimen", "scanPadding"));
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
        layoutParams.gravity = Gravity.CENTER;
        polygonView.setLayoutParams(layoutParams);
    }

    public Map<Integer, PointF> customDetection(Bitmap tempBitmap) {
        System.loadLibrary("opencv_java3");
        Mat edges=detectEdges(tempBitmap);
        List<Point> points = findCont(edges);

        Map<Integer,PointF> pointFs = new HashMap<Integer,PointF>();
        for ( int i=0;i<points.size(); i++ ) {
            float x=(float)points.get(i).x;
            float y=(float)points.get(i).y;
            pointFs.put(i,new PointF(x,y));
        }
        return pointFs;
    }

    public Mat detectEdges(Bitmap bitmap) {
        Mat rgba = new Mat();
        org.opencv.android.Utils.bitmapToMat(bitmap, rgba);

        Mat edges = new Mat(rgba.size(), CvType.CV_8UC1);
//        Imgproc.GaussianBlur(rgba, rgba, new Size(3,3),0);
//        Imgproc.threshold(rgba,rgba,150,255,Imgproc.THRESH_BINARY);
//        Imgproc.adaptiveThreshold(rgba,rgba,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,75,10);
        Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(rgba, edges, 140, 255);

        return edges;
    }

    public List<Point> findCont(Mat edges) {
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(edges,contours,hierarchy,Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find max contour area
        MatOfPoint2f approx = new MatOfPoint2f();
        Iterator<MatOfPoint> each = contours.iterator();
        double maxVal = 0;
        int i=0;
        while (each.hasNext()) {
            MatOfPoint2f temp = new MatOfPoint2f();
            MatOfPoint wrapper = each.next();
            MatOfPoint2f sec = new MatOfPoint2f(wrapper.toArray());
            double peri = Imgproc.arcLength(sec,true);
            Imgproc.approxPolyDP(sec,temp,0.02 * peri,true);
            double contourArea = Imgproc.contourArea(contours.get(i));
//            Log.i(TAG,"Contour Area "+String.valueOf(contourArea));

            if ( contourArea > maxVal && temp.toList().size() == 4 ) {
                approx=temp;
                maxVal=contourArea;
//                Log.i(TAG,"Max Contour Area "+String.valueOf(contourArea));
            }
            i++;
        }

        List<Point> points=approx.toList();

        if ( points.size() == 4 && maxVal > 40000 ) {
            Point temp = points.get(2);
            points.set(2, points.get(3));
            points.set(3, temp);
        } else {
            points=new ArrayList();
            for ( int j=0;j<4; j++ ) {
                double m=0.0,n=0.0;
                if (j==1){m=container_width;}
                if (j==2){n=container_height;}
                if (j==3){m=container_width;n=container_height;}
                Point tempP=new Point(m,n);
                points.add(tempP);
            }

            Mat lines = new Mat();
            int threshold = 120;
            double minLineSize = 100;
            double lineGap = 500;
            double theta=Math.PI / 180;
            Imgproc.HoughLinesP(edges, lines, 1, theta, threshold, minLineSize, lineGap);

//            LineSegmentDetector ls = Imgproc.createLineSegmentDetector();
//            ls.detect(edges, lines);
//            ls.drawSegments(edges,lines);


            float width=container_width,height=container_height;
            float halfw=width/2,halfh=height/2;

            List<Map> lines_arr=new ArrayList();
            Map<String,Double> distance_arr=new HashMap();
            distance_arr.put("vl",0.0);distance_arr.put("vr",0.0);distance_arr.put("hu",0.0);distance_arr.put("hl",0.0);
            int index_vl1=0,index_vr1=0,index_hu1=0,index_hl1=0;

            for (int k=0; k < lines.rows(); k++) {
                for (int l=0; l < lines.cols(); l++) {
                    double[] vec = lines.get(k, l);
                    double x1 = vec[0], y1 = vec[1], x2 = vec[2], y2 = vec[3];
                    double res=Math.pow((x2-x1),2) + Math.pow((y2-y1),2);
                    double distance=Math.sqrt(res);
                    double temp1=0,temp2=0;

//                    Log.i(TAG, "Line cols - x1=" + x1 + ", y1=" + y1+ ", x2=" + x2 + ", y2=" + y2+", distance="+distance);

                    // If line is similar to a forward slash, we need to swap second and third position respectively
                    if ( x1 < x2 && y1 > y2 && x1<halfw&&x2<halfw ) {
                        temp1=x1;x1=x2;x2=temp1;
                        temp2=y1;y1=y2;y2=temp2;
                    }

                    String plane="";
                    if ( x1<halfw&&x2<halfw ) { plane="vl"; }
                    if ( x1>halfw&&x2>halfw ) { plane="vr"; }
                    if ( y1<halfh&&y2<halfh ) { plane="hu"; }
                    if ( y1>halfh&&y2>halfh ) { plane="hl"; }

                    if ( plane == "" ) continue;
                    if ( distance_arr.get(plane) == 0.0 ) {
                        distance_arr.put(plane,distance);
                        Map<Point,Point> lines_obj=new HashMap();
                        lines_obj.put(new Point(x1,y1),new Point(x2,y2));
                        lines_arr.add(lines_obj);
                        int index=lines_arr.size()-1;
                        if ( plane=="vl" ) {
                            index_vl1=index;
                        } else if ( plane=="vr" ) {
                            index_vr1=index;
                        } else if ( plane=="hu" ) {
                            index_hu1=index;
                        } else if ( plane=="hl" ) {
                            index_hl1=index;
                        }
//                        Log.i(TAG,"Inserted plane value first time for plane="+plane+", index_vl1="+index_vl1+", index_vr1="+index_vr1+", index_hu1="+index_hu1+", index_hl1="+index_hl1);
                    } else if ( distance_arr.get(plane) < distance ) {
//                        Log.i(TAG,"Editing existing plane="+plane+", index_vl1="+index_vl1+", index_vr1="+index_vr1+", index_hu1="+index_hu1+", index_hl1="+index_hl1);
                        distance_arr.put(plane,distance);
                        Map<Point,Point> lines_obj=new HashMap();
                        lines_obj.put(new Point(x1,y1),new Point(x2,y2));
                        if ( plane=="vl" ) {
                            lines_arr.set(index_vl1,lines_obj);
                        } else if ( plane=="vr" ) {
                            lines_arr.set(index_vr1,lines_obj);
                        } else if ( plane=="hu" ) {
                            lines_arr.set(index_hu1,lines_obj);
                        } else if ( plane=="hl" ) {
                            lines_arr.set(index_hl1,lines_obj);
                        }
                    }
                }
            }

            boolean hu=false,hl=false,vl=false,vr=false;
            int index_vl=0,index_vr=0,index_hu=0,index_hl=0;
            for ( int g=0;g<lines_arr.size();g++ ) {
                Map<Point,Point> lines_obj=lines_arr.get(g);
                double x1=0,y1=0,x2=0,y2=0;

                for ( Map.Entry<Point,Point> entry : lines_obj.entrySet() ) {
                    x1=entry.getKey().x;y1=entry.getKey().y;x2=entry.getValue().x;y2=entry.getValue().y;
                }
//                Log.i(TAG, "Long Lines "+g+" x1=" + x1 + ", y1=" + y1+ ", x2=" + x2 + ", y2=" + y2);
//                Imgproc.line(edges, new Point(x1, y1), new Point(x2, y2), new Scalar(255, 0, 0), 3);

                if ( x1<halfw&&x2<halfw ) { vl=true;index_vl=g; }
                if ( x1>halfw&&x2>halfw ) { vr=true;index_vr=g; }
                if ( y1<halfh&&y2<halfh ) { hu=true;index_hu=g; }
                if ( y1>halfh&&y2>halfh ) { hl=true;index_hl=g; }
            }


            double x1 = 0, y1 = 0, x2 = 0, y2 = 0;
            double m1 = 0, n1 = 0, m2 = 0, n2 = 0;
            double i1 = 0, j1 = 0, i2 = 0, j2 = 0;
            double k1 = 0, l1 = 0, k2 = 0, l2 = 0;
            if ( vl ) {
                Map<Point, Point> lines_obj1 = lines_arr.get(index_vl);
                for (Map.Entry<Point, Point> entry : lines_obj1.entrySet()) {
                    x1 = entry.getKey().x;y1 = entry.getKey().y;
                    x2 = entry.getValue().x;y2 = entry.getValue().y;
                }
            }
            if ( vr ) {
                Map<Point, Point> lines_obj2 = lines_arr.get(index_vr);
                for (Map.Entry<Point, Point> entry : lines_obj2.entrySet()) {
                    m1 = entry.getKey().x;n1 = entry.getKey().y;
                    m2 = entry.getValue().x;n2 = entry.getValue().y;
                }
            }
            if ( hu ) {
                Map<Point, Point> lines_obj3 = lines_arr.get(index_hu);
                for (Map.Entry<Point, Point> entry : lines_obj3.entrySet()) {
                    i1 = entry.getKey().x;j1 = entry.getKey().y;
                    i2 = entry.getValue().x;j2 = entry.getValue().y;
                }
            }
            if ( hl ) {
                Map<Point, Point> lines_obj4 = lines_arr.get(index_hl);
                for (Map.Entry<Point, Point> entry : lines_obj4.entrySet()) {
                    k1 = entry.getKey().x;l1 = entry.getKey().y;
                    k2 = entry.getValue().x;l2 = entry.getValue().y;
                }
            }

            if ( vl&&vr&&hu&&hl ) {
                // Four lines in proper plane
                Log.i(TAG, "Line fell according to case 1");

                points.set(0,new Point(x1,j1));
                points.set(1,new Point(m1,j2));
                points.set(2,new Point(x2,l1));
                points.set(3,new Point(m2,l2));
            } else if ( vl&&vr&&hu ) {
                // Three lines - Left,Right,Upper
                Log.i(TAG, "Line fell according to case 2");

                points.set(0,new Point(x1,j1));
                points.set(1,new Point(m1,j2));
                points.set(2,new Point(x2,y2));
                points.set(3,new Point(m2,n2));
            } else if ( vl&&vr&&hl ) {
                // Three lines - Left,Right,Lower
                Log.i(TAG, "Line fell according to case 3");

                points.set(0,new Point(x1,y1));
                points.set(1,new Point(m1,n1));
                points.set(2,new Point(x2,l1));
                points.set(3,new Point(m2,l2));
            } else if ( vl&&hu&&hl ) {
                // Three lines - Left,Upper,Lower
                Log.i(TAG, "Line fell according to case 4");

                points.set(0,new Point(x1,j1));
                points.set(1,new Point(i2,j2));
                points.set(2,new Point(x2,l1));
                points.set(3,new Point(k2,l2));
            } else if ( vr&&hu&&hl ) {
                // Three lines - Right,Upper,Lower
                Log.i(TAG, "Line fell according to case 5");

                points.set(0,new Point(i1,j1));
                points.set(1,new Point(m1,j2));
                points.set(2,new Point(k1,l1));
                points.set(3,new Point(m2,l2));
            } else if ( vl&&vr ) {
                // Two lines - Left,Right
                Log.i(TAG, "Line fell according to case 6");

                points.set(0,new Point(x1,y1));
                points.set(1,new Point(m1,n1));
                points.set(2,new Point(x2,y2));
                points.set(3,new Point(m2,n2));

            } else if ( vl&&hu ) {
                // Two lines - Left,Upper
                Log.i(TAG, "Line fell according to case 7");

                points.set(0,new Point(x1,j1));
                points.set(1,new Point(i2,j2));
                points.set(2,new Point(x2,y2));
                points.set(3,new Point(i2,y2));
            } else if ( vl&&hl ) {
                // Two lines - Left,Lower
                Log.i(TAG, "Line fell according to case 8");

                points.set(0,new Point(x1,y1));
                points.set(1,new Point(k2,y1));
                points.set(2,new Point(x2,l1));
                points.set(3,new Point(k2,l2));
            } else if ( vr&&hu ) {
                // Two lines - Right,Upper
                Log.i(TAG, "Line fell according to case 9");

                points.set(0,new Point(i1,j1));
                points.set(1,new Point(m1,j2));
                points.set(2,new Point(i1,n2));
                points.set(3,new Point(m2,n2));
            } else if ( vr&&hl ) {
                // Two lines - Right,Lower
                Log.i(TAG, "Line fell according to case 10");

                points.set(0,new Point(k1,n1));
                points.set(1,new Point(m1,n1));
                points.set(2,new Point(k1,l1));
                points.set(3,new Point(m2,l2));
            } else if ( hu&&hl ) {
                // Two lines - Upper,Lower
                Log.i(TAG, "Line fell according to case 11");

                points.set(0,new Point(i1,j1));
                points.set(1,new Point(i2,j2));
                points.set(2,new Point(k1,l1));
                points.set(3,new Point(k2,l2));
            } else if ( vl ) {
                // One line in vl
                Log.i(TAG, "Line fell according to case 12");

                points.set(0,new Point(x1,y1));
                points.set(2,new Point(x2,y2));
                points.set(1,new Point(container_width,0));
                points.set(3,new Point(container_width,container_height));
            } else if ( vr ) {
                // One line in vr
                Log.i(TAG, "Line fell according to case 13");

                points.set(1,new Point(m1,n1));
                points.set(3,new Point(m2,n2));
                points.set(0,new Point(0,0));
                points.set(2,new Point(0,container_height));
            } else if ( hu ) {
                // One line in hu
                Log.i(TAG, "Line fell according to case 14");

                points.set(0,new Point(i1,j1));
                points.set(1,new Point(i2,j2));
                points.set(2,new Point(0,container_height));
                points.set(3,new Point(container_width,container_height));
            } else if ( hl ) {
                // One line in hl
                Log.i(TAG, "Line fell according to case 15");

                points.set(2,new Point(k1,l1));
                points.set(3,new Point(k2,l2));
                points.set(0,new Point(0,0));
                points.set(1,new Point(container_width,0));
            }
        }

        Log.i(TAG,"points array - "+points.toString());
/*
        Bitmap resultBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(edges, resultBitmap);
        sourceImageView.setImageBitmap(resultBitmap);
*/

        return points;
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) {
        List<PointF> pointFs = getContourEdgePoints(tempBitmap);
        Log.i(TAG,"Contour Points - "+pointFs.toString());
        Map<Integer, PointF> orderedPoints = orderedValidEdgePoints(tempBitmap, pointFs);
        return orderedPoints;
    }

    private List<PointF> getContourEdgePoints(Bitmap tempBitmap) {
        float[] points = ((ScanActivity) getActivity()).getPoints(tempBitmap);
        float x1 = points[0];
        float x2 = points[1];
        float x3 = points[2];
        float x4 = points[3];

        float y1 = points[4];
        float y2 = points[5];
        float y3 = points[6];
        float y4 = points[7];

        List<PointF> pointFs = new ArrayList<PointF>();
        pointFs.add(new PointF(x1, y1));
        pointFs.add(new PointF(x2, y2));
        pointFs.add(new PointF(x3, y3));
        pointFs.add(new PointF(x4, y4));
        return pointFs;
    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<Integer, PointF>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
        return outlinePoints;
    }

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
        Map<Integer, PointF> orderedPoints = polygonView.getOrderedPoints(pointFs);
        if (!polygonView.isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }

    private class ScanButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Map<Integer, PointF> points = polygonView.getPoints();
            if (isScanPointsValid(points)) {
                new ScanAsyncTask(points).execute();
            } else {
                showErrorDialog();
            }
        }
    }

    private void showErrorDialog() {
        SingleButtonDialogFragment fragment = new SingleButtonDialogFragment("Ok", "Cant crop the image, change the points", "Error", true);
        FragmentManager fm = getActivity().getFragmentManager();
        fragment.show(fm, SingleButtonDialogFragment.class.toString());
    }

    private boolean isScanPointsValid(Map<Integer, PointF> points) {
        return points.size() == 4;
    }

    private Bitmap scaledBitmap(Bitmap bitmap, int width, int height) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    private Bitmap getScannedBitmap(Bitmap original, Map<Integer, PointF> points) {
        int width = original.getWidth();
        int height = original.getHeight();
        float xRatio = (float) original.getWidth() / sourceImageView.getWidth();
        float yRatio = (float) original.getHeight() / sourceImageView.getHeight();

        float x1 = (points.get(0).x) * xRatio;
        float x2 = (points.get(1).x) * xRatio;
        float x3 = (points.get(2).x) * xRatio;
        float x4 = (points.get(3).x) * xRatio;
        float y1 = (points.get(0).y) * yRatio;
        float y2 = (points.get(1).y) * yRatio;
        float y3 = (points.get(2).y) * yRatio;
        float y4 = (points.get(3).y) * yRatio;
        Log.d("", "POints(" + x1 + "," + y1 + ")(" + x2 + "," + y2 + ")(" + x3 + "," + y3 + ")(" + x4 + "," + y4 + ")");
        Bitmap _bitmap = ((ScanActivity) getActivity()).getScannedBitmap(original, x1, y1, x2, y2, x3, y3, x4, y4);
        return _bitmap;
    }

    private class ScanAsyncTask extends AsyncTask<Void, Void, Bitmap> {

        private Map<Integer, PointF> points;

        public ScanAsyncTask(Map<Integer, PointF> points) {
            this.points = points;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("Scanning");
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bitmap =  getScannedBitmap(original, points);
            Uri uri = Utils.getUri(getActivity(), bitmap);
            scanner.onScanFinish(uri);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            bitmap.recycle();
            dismissDialog();
        }
    }

    protected void showProgressDialog(String message) {
        progressDialogFragment = new ProgressDialogFragment(message);
        FragmentManager fm = getFragmentManager();
        progressDialogFragment.show(fm, ProgressDialogFragment.class.toString());
    }

    protected void dismissDialog() {
        progressDialogFragment.dismissAllowingStateLoss();
    }

}