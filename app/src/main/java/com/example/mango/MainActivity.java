package com.example.mango;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.CoordinateConverter;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements AMapLocationListener, LocationSource {
    private DataLib dataLib;
    private Record record;
    private Gravity gravity;
    private Yaw yaw;
    //请求权限码
    private static final int REQUEST_PERMISSIONS = 9527;
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    private MapView mapView;
    //地图控制器
    private AMap aMap = null;
    //位置更改监听
    private LocationSource.OnLocationChangedListener mListener;
    //定位样式
    private MyLocationStyle myLocationStyle = new MyLocationStyle();
    private boolean btmSheetStatus;
    private boolean gravStatus, posStatus, yawStatus;
    private boolean startBtnStatus;
    private boolean canSave, canClear;
    private PosTrans posTrans;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mapView = (MapView) findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        checkingAndroidVersion();

        initMap(savedInstanceState);

        initLocation();

        LinearLayout btmSheet = findViewById(R.id.btmSheet);
        LinearLayout mainBtmSheet = findViewById(R.id.mainBtmSheet);
        btmSheetStatus = false;
        gravStatus = false;
        posStatus = false;
        yawStatus = false;
        startBtnStatus = false;
        canSave = false;
        canClear = false;

        android.widget.Button getGravBtn = findViewById(R.id.getGravBtn);
        android.widget.Button getPosBtn = findViewById(R.id.getPosBtn);
        android.widget.Button getYawBtn = findViewById(R.id.getYawBtn);
        android.widget.Button saveBtn = findViewById(R.id.saveBtn);
        android.widget.Button resetBtn = findViewById(R.id.resetBtn);

        android.widget.Button startBtn = findViewById(R.id.startBtn);
        android.widget.Button pauseBtn = findViewById(R.id.pauseBtn);
        android.widget.Button stopBtn = findViewById(R.id.stopBtn);

        TextView gravText = findViewById(R.id.gravText);
        TextView yawText = findViewById(R.id.yawText);
        TextView trueLatlngText = findViewById(R.id.trueLatlngText);
        TextView preLatlngText = findViewById(R.id.preLatlngText);
        TextView ENUText = findViewById(R.id.ENUText);
        TextView ns = findViewById(R.id.ns);
        TextView ms = findViewById(R.id.ms);
        TextView yawGyroText = findViewById(R.id.yawGyroText);
        TextView yawMagText = findViewById(R.id.yawMagText);
        TextView accModText = findViewById(R.id.accModText);
        TextView accFilterText = findViewById(R.id.accFilterText);
        TextView extremumText = findViewById(R.id.extremumText);
        TextView PVText = findViewById(R.id.PVText);
        TextView peakMeanText = findViewById(R.id.peakMeanText);
        TextView valleyMeanText = findViewById(R.id.valleyMeanText);
        TextView peakMSDText = findViewById(R.id.peakMSDText);
        TextView valleyMSDText = findViewById(R.id.valleyMSDText);
        TextView peakText = findViewById(R.id.peakText);
        TextView valleyText = findViewById(R.id.valleyText);
        TextView deltaTimeText = findViewById(R.id.deltaTimeText);
        TextView lastDeltaTimeText = findViewById(R.id.lastDeltaTimeText);
        TextView stepNumText = findViewById(R.id.stepNumText);
        TextView stepLengthText = findViewById(R.id.stepLengthText);

        dataLib = new DataLib();

        Context context = this;

        record = new Record(aMap,
                mLocationClient,
                dataLib,
                context,
                startBtn, pauseBtn, stopBtn,
                trueLatlngText,
                preLatlngText,
                ENUText,
                ns, ms,
                yawGyroText, yawMagText,
                accModText, accFilterText,
                extremumText, PVText,
                peakMeanText, valleyMeanText,
                peakMSDText, valleyMSDText,
                peakText, valleyText,
                deltaTimeText, lastDeltaTimeText,
                stepNumText, stepLengthText);

        gravity = new Gravity(dataLib, context, gravText);

        getGravBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(startBtnStatus) showMsg("请先停止记录");
                else {
                    gravity.getGravity();

                    gravStatus = true;
                }
            }
        });

        yaw = new Yaw(dataLib, context, yawText);

        getYawBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(startBtnStatus) showMsg("请先停止记录");
                else {
                    yaw.getYaw();

                    yawStatus = true;
                }
            }
        });

        getPosBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(startBtnStatus) showMsg("请先停止记录");
                else {
                    mLocationClient.startLocation();

                    dataLib.initLatitude = dataLib.latitude;
                    dataLib.initLongitude = dataLib.longtitude;
                    dataLib.initAltitude = dataLib.altitude;

                    double[] GCJ02 = new double[]{dataLib.initLatitude, dataLib.initLongitude, dataLib.initAltitude};
                    double[] WGS84 = posTrans.GCJ022WGS84(GCJ02);

                    dataLib.initWGS84Latitude = WGS84[0];
                    dataLib.initWGS84Longitude = WGS84[1];
                    dataLib.initWGS84Altitude = WGS84[2];

                    double[] ENU = posTrans.WGS842ENU(WGS84);

                    trueLatlngText.setText("GCJ02定位经纬度: " + String.format("%.6f", GCJ02[0]) + ", "
                            + String.format("%.6f", GCJ02[1]));

                    ENUText.setText("ENU坐标: " + String.format("%.6f", ENU[0]) + ", "
                            + String.format("%.6f", ENU[1]));

                    double[] preWGS84 = posTrans.ENU2WGS84(ENU);
                    double[] preGCJ02 = posTrans.WGS842GCJ02(preWGS84);

                    preLatlngText.setText("GCJ02预测经纬度: " + String.format("%.6f", preGCJ02[0]) + ", "
                            + String.format("%.6f", preGCJ02[1]));

                    posStatus = true;

                    showMsg("获取初始位置成功");
                }
            }
        });

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!canClear) showMsg("请先记录一段数据");
                else if(startBtnStatus) showMsg("请先停止记录");
                else {
                    gravText.setText("平均重力值: ");
                    yawText.setText("初始航向角: ");
                    trueLatlngText.setText("GCJ02定位经纬度: ");
                    preLatlngText.setText("GCJ02预测经纬度: ");
                    ENUText.setText("ENU坐标: ");
                    ns.setText("纳秒: ");
                    ms.setText("毫秒: ");
                    yawGyroText.setText("陀螺仪yaw: ");
                    yawMagText.setText("磁力计yaw: ");
                    accModText.setText("加速度计z轴值: ");
                    accFilterText.setText("滤波值: ");
                    extremumText.setText("极值类型: ");
                    PVText.setText("峰谷值类型: ");
                    peakMeanText.setText("PeakMean: ");
                    valleyMeanText.setText("ValleyMean: ");
                    peakMSDText.setText("PeakMSD: ");
                    valleyMSDText.setText("ValleyMSD: ");
                    peakText.setText("Peak: ");
                    valleyText.setText("Valley: ");
                    deltaTimeText.setText("本步间隔: ");
                    lastDeltaTimeText.setText("上步间隔: ");
                    stepNumText.setText("步数: ");
                    stepLengthText.setText("步长: ");

                    showMsg("清除面板数据");

                    canClear = false;
                }
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!canSave) showMsg("请先记录一段数据");
                else if(startBtnStatus) showMsg("请先停止记录");
                else {
                    int time = (int) (System.currentTimeMillis() / 1000000.0);

                    FileSave.writeToFile(context, "SensorData_" + time + ".txt", dataLib.sensorDataString.toString());
                    FileSave.writeToFile(context, "FilterData_" + time + ".txt", dataLib.filterDataString.toString());
                    FileSave.writeToFile(context, "PositionData_" + time + ".txt", dataLib.positionDataString.toString());

                    dataLib.sensorDataString = new StringBuilder();
                    dataLib.filterDataString = new StringBuilder();
                    dataLib.positionDataString = new StringBuilder();

                    showMsg("保存文件成功");

                    canSave = false;
                }
            }
        });

        posTrans = new PosTrans(dataLib);

        mainBtmSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ObjectAnimator moveUp = ObjectAnimator.ofFloat(btmSheet, "translationY", 0f, -1100f);
                ObjectAnimator moveDown = ObjectAnimator.ofFloat(btmSheet, "translationY", -1100f, 0f);

                ObjectAnimator moveMapUp = ObjectAnimator.ofFloat(mapView, "translationY", 0f, -700f);
                ObjectAnimator moveMapDown = ObjectAnimator.ofFloat(mapView, "translationY", -700f, 0f);

                moveUp.setDuration(250);
                moveDown.setDuration(250);

                moveMapUp.setDuration(250);
                moveMapDown.setDuration(250);

                moveUp.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        startBtn.setEnabled(false);
                        aMap.getUiSettings().setAllGesturesEnabled(false);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startBtn.setEnabled(true);
                        aMap.getUiSettings().setAllGesturesEnabled(true);
                    }
                });

                moveDown.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        startBtn.setEnabled(false);
                        aMap.getUiSettings().setAllGesturesEnabled(false);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startBtn.setEnabled(true);
                        aMap.getUiSettings().setAllGesturesEnabled(true);
                    }
                });

                moveMapUp.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        startBtn.setEnabled(false);
                        aMap.getUiSettings().setAllGesturesEnabled(false);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startBtn.setEnabled(true);
                        aMap.getUiSettings().setAllGesturesEnabled(true);
                    }
                });

                moveMapDown.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        startBtn.setEnabled(false);
                        aMap.getUiSettings().setAllGesturesEnabled(false);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startBtn.setEnabled(true);
                        aMap.getUiSettings().setAllGesturesEnabled(true);
                    }
                });

                AnimatorSet animatorSetUp = new AnimatorSet();
                animatorSetUp.playTogether(moveUp, moveMapUp);

                AnimatorSet animatorSetDown = new AnimatorSet();
                animatorSetDown.playTogether(moveDown, moveMapDown);

                if (!btmSheetStatus) {
                    Log.d("BottomSheet", "向上移动, status=" + btmSheetStatus);
                    animatorSetUp.start();
                    btmSheetStatus = true;
                    Log.d("BottomSheet", "status改为" + btmSheetStatus);
                } else {
                    Log.d("BottomSheet", "向下移动, status=" + btmSheetStatus);
                    animatorSetDown.start();
                    btmSheetStatus = false;
                    Log.d("BottomSheet", "status改为" + btmSheetStatus);
                }
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!gravStatus) {
                    showMsg("请先获取平均重力值");
                } else if (!posStatus) {
                    showMsg("请先获取当前经纬度");
                } else if(!yawStatus) {
                    showMsg("请先获取当前航向角");
                }
                else {
                    mLocationOption = new AMapLocationClientOption();
                    mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.Sport);
                    mLocationClient.setLocationOption(mLocationOption);
                    mLocationClient.stopLocation();

                    startBtnStatus = true;

                    Log.d("accSensor", String.valueOf(dataLib.grav));

                    aMap.getUiSettings().setAllGesturesEnabled(false);

                    record.StartRecord();

                    showMsg("开始记录");
                }
            }
        });

        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                record.PauseRecord();
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBtnStatus = false;
                gravStatus = false;
                posStatus = false;
                yawStatus = false;

                canSave = true;
                canClear = true;

                posTrans = new PosTrans(dataLib);
                gravity = new Gravity(dataLib, context, gravText);
                yaw = new Yaw(dataLib, context, yawText);
                Log.d("DataLib",dataLib.initLatitude + "," + dataLib.initLongitude);

                record.StopRecord();
                record = new Record(aMap,
                        mLocationClient,
                        dataLib,
                        context,
                        startBtn, pauseBtn, stopBtn,
                        trueLatlngText,
                        preLatlngText,
                        ENUText,
                        ns, ms,
                        yawGyroText, yawMagText,
                        accModText, accFilterText,
                        extremumText, PVText,
                        peakMeanText, valleyMeanText,
                        peakMSDText, valleyMSDText,
                        peakText, valleyText,
                        deltaTimeText, lastDeltaTimeText,
                        stepNumText, stepLengthText);

                showMsg("停止记录");
            }
        });
    }

    /**
     * 初始化地图
     */
    private void initMap(Bundle savedInstanceState) {
        mapView = findViewById(R.id.map_view);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mapView.onCreate(savedInstanceState);
        //初始化地图控制器对象
        aMap = mapView.getMap();
        //设置最小缩放等级为16 ，缩放级别范围为[3, 20]
        aMap.setMinZoomLevel(3);
        aMap.setMaxZoomLevel(20);

        // 设置定位监听
        aMap.setLocationSource(this);
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationEnabled(true);

        // 自定义定位蓝点图标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.gps_point));
        // 自定义精度范围的圆形边框颜色  都为0则透明
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));
        // 自定义精度范围的圆形边框宽度  0 无宽度
        myLocationStyle.strokeWidth(0);
        // 设置圆形的填充颜色  都为0则透明
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));

        //设置定位蓝点的Style
        aMap.setMyLocationStyle(myLocationStyle);

    }

    /**
     * 检查Android版本
     */
    private void checkingAndroidVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Android6.0及以上先获取权限再定位
            requestPermission();
        } else {
            //Android6.0以下直接定位
            mLocationClient.startLocation();
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSIONS)
    private void requestPermission() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (EasyPermissions.hasPermissions(this, permissions)) {
            //true 有权限 开始定位
            mLocationClient.startLocation();
        } else {
            //false 无权限
            EasyPermissions.requestPermissions(this, "需要权限", REQUEST_PERMISSIONS, permissions);
        }
    }

    /**
     * 请求权限结果
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //设置权限请求结果
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * Toast提示
     *
     * @param msg 提示内容
     */
    private void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        //初始化定位
        try {
            mLocationClient = new AMapLocationClient(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mLocationClient != null) {
            //设置定位回调监听
            mLocationClient.setLocationListener(this);
            //初始化AMapLocationClientOption对象
            mLocationOption = new AMapLocationClientOption();
            mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.SignIn);
//            //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
//            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
//            //获取最近3s内精度最高的一次定位结果：
//            //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
//            mLocationOption.setOnceLocationLatest(true);
//            //设置是否返回地址信息（默认返回地址信息）
//            mLocationOption.setNeedAddress(true);
//            //设置定位请求超时时间，单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
//            mLocationOption.setHttpTimeOut(20000);
//            //关闭缓存机制，高精度定位会产生缓存。
//            mLocationOption.setLocationCacheEnable(false);
            //给定位客户端对象设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
            mLocationClient.stopLocation();
        }
    }

    /**
     * 接收异步返回的定位结果
     *
     * @param aMapLocation
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                //地址
                dataLib.latitude = aMapLocation.getLatitude();
                dataLib.longtitude = aMapLocation.getLongitude();
                dataLib.altitude = aMapLocation.getAltitude();

//                showMsg(String.format("%.6f", dataLib.latitude) + "\n" + String.format("%.6f", dataLib.longtitude));

                //停止定位后，本地定位服务并不会被销毁
                mLocationClient.stopLocation();

                //显示地图定位结果
                if (mListener != null) {
                    // 显示系统图标
                    mListener.onLocationChanged(aMapLocation);
                }
            } else {
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }
    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mLocationClient != null) {
            mLocationClient.startLocation();//启动定位
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stopLocation();
        mLocationClient.onDestroy();
        mapView.onDestroy();
    }
}