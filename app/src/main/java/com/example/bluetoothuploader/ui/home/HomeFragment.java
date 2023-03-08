package com.example.bluetoothuploader.ui.home;

import static android.content.Context.MODE_PRIVATE;
import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.bluetoothuploader.R;
import com.example.bluetoothuploader.databinding.FragmentHomeBinding;

import cn.leancloud.LCObject;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private static final String TAG = "dsh";
    private static int NOTIFICATION_ID = 1;

    private FragmentActivity activity;

    private SharedPreferences sp;

    // 构建对象
    private LCObject todo = new LCObject("Todo");

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    // 下面使用Android5.0新增的扫描API，扫描返回的结果更友好，比如BLE广播数据以前是byte[] scanRecord，而新API帮我们解析成ScanRecord类\
    final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        activity = getActivity();

        //本地存储： 获取SharedPreferences对象
        sp = activity.getSharedPreferences("SP", MODE_PRIVATE);


        // 点击启动事件
        View startBtnDom = root.findViewById(R.id.startBtn);
        startBtnDom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String appId = sp.getString("appId", "");
                String appKey = sp.getString("appKey", "");
                String appApi = sp.getString("appApi", "");
                String mac = sp.getString("mac", "");
                String timer = sp.getString("timer", "");

                if (mac.equals("")) {
                    Toast.makeText(activity, "请检查蓝牙监听目标配置", Toast.LENGTH_LONG).show();
                    ;
                } else if (appId.equals("") || appKey.equals("") || appApi.equals("")) {
                    Toast.makeText(activity, "请检查上报服务器配置", Toast.LENGTH_LONG).show();
                    ;
                } else {
                    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        bluetoothLeScanner.startScan(new ScanCallback() {
                            @Override
                            public void onScanResult(int callbackType, ScanResult result) {
                                super.onScanResult(callbackType, result);
                                BluetoothDevice dev = result.getDevice(); // 获取BLE设备信息
                                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                    String address =  dev.getAddress();
                                    if(!address.equals(null)){
                                        if(mac.equals(address)){
                                            byte[] scanRecord = result.getScanRecord().getBytes();
                                            Log.i(TAG, "onScanResult: " +dev.getName());
                                            Log.i(TAG, "onScanResult: " +address);
//                                            Log.i(TAG, "onScanResult: " +result.getRssi());
                                            Log.i(TAG, "onScanResult: " +scanRecord);
                                        }

                                    }

                                }

                            }
                        });
                    }

                    // 扫描蓝牙 并上报数据
//                    saveData();
                    //                addNotification();
                }


            }
        });

        return root;
    }


    private void saveData() {


        // 为属性赋值
        todo.put("title", "马拉松报名");
        todo.put("priority", 2);

        // 将对象保存到云端
        todo.saveInBackground().subscribe(new Observer<LCObject>() {
            public void onSubscribe(Disposable disposable) {
            }

            public void onNext(LCObject todo) {
                // 成功保存之后，执行其他逻辑
                Log.i("aaaaa", "保存成功。objectId：" + todo.getObjectId());
            }

            public void onError(Throwable throwable) {
                // 异常处理
                Log.i("aaaaa", "保存失败。objectId：" + throwable.getMessage());
            }

            public void onComplete() {
            }
        });
    }

    private void addNotification() {
        //1、NotificationManager
        NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        /** 2、Builder->Notification
         *  必要属性有三项
         *  小图标，通过 setSmallIcon() 方法设置
         *  标题，通过 setContentTitle() 方法设置
         *  内容，通过 setContentText() 方法设置*/
        Notification.Builder builder = new Notification.Builder(activity);
        builder.setContentText("通知内容")//设置通知内容
                .setContentTitle("通知标题")//设置通知标题
                .setSmallIcon(R.drawable.notification);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {  // 这里的判断是为了兼容更高版本的API (O // 26)
            NotificationChannel channel = new NotificationChannel("001", "my_channel", NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true); //是否在桌面icon右上角展示小红点
            channel.setLightColor(Color.GREEN); //小红点颜色
            channel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
            manager.createNotificationChannel(channel);
            builder.setChannelId("001");
        }

        Notification n = builder.build();
        //3、manager.notify()
        manager.notify(NOTIFICATION_ID, n);


    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}