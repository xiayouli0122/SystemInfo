package com.yuri.systeminfo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * IMEI
 * MAC
 * 手机型号
 * 系统版本
 * IP
 * 地理位置
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Yuri";

    EditText mIpEditText;
    EditText mPortEditText;

    TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.text);

        mIpEditText = (EditText) findViewById(R.id.et_ip);
        mPortEditText = (EditText) findViewById(R.id.et_port);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                getInfo();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 0);
            }

        } else {
            getInfo();
        }

        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setProxy();
            }
        });

        findViewById(R.id.btn_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String baseUrl = "https://m.baidu.com";
                String qudao = "/from=844b";
                String keyword = "/s?word=ip";
                String guanggao = "";
//                String guanggao = "&sa=tb&ts=0&t_kt=0&ie=utf-8&rsv_t=c2eaKwziCjrrEC9Ar4vQxouAgsELwXwjEBY1YwzHvpQQrl%252Bh9Jp9&rsv_pq=14318853138871529188";
//                String guanggao = "&sa=tb&ts=6651294&t_kt=90&ie=utf-8&rsv_t=130bsjAIWB%252F%252F3HE40pdZEVGZpswrvZkGCSYnrYh9I0e7z%252BIUdfpAnSnvEg&ms=1&rsv_pq=7130147884630772087&ss=100&t_it=1&rqlang=zh&rsv_sug4=6814&inputT=5896&oq=阿里收购联华超市";
                Intent intent = new Intent();
                String url = baseUrl + qudao + keyword + guanggao;
                Log.d(TAG, "===url: " + url);
                intent.setData(Uri.parse(url));
                intent.setAction(Intent.ACTION_VIEW);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getInfo();
            }
        });

        findViewById(R.id.btn_imei).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
//                    Runtime.getRuntime().exec("input tap 250 300");
//                    Runtime.getRuntime().exec("setprop microvirt.imei 222222222222222222");
                    Runtime.getRuntime().exec("setprop microvirt.linenum 13666666666");
//                    Runtime.getRuntime().exec(" date -s 20150312.123234");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }


    private void setProxy() {
        String host = mIpEditText.getText().toString().trim();
        String portStr = mPortEditText.getText().toString().trim();
        int port = Integer.valueOf(portStr);
        Log.d(TAG, "host: " + host + ",post:" + port);
        WifiManager wifiManager =(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            return;
        }
        List<WifiConfiguration> configurationList = wifiManager.getConfiguredNetworks();
        WifiConfiguration configuration = null;
        int cur = wifiManager.getConnectionInfo().getNetworkId();
        for (int i = 0; i < configurationList.size(); ++i) {
            WifiConfiguration wifiConfiguration = configurationList.get(i);
            if (wifiConfiguration.networkId == cur) {
                configuration = wifiConfiguration;
            }
        }

        if (configuration == null) {
            Log.d(TAG, "configuration is null ");
            mTextView.setText("configuration is null ");
            return;
        }

        //get the link properties from the wifi configuration
        try {
            Object linkProperties = getFieldObject(configuration, "linkProperties");
            if (linkProperties == null) {
                Log.d(TAG, "linkProperties is null ");
                mTextView.setText("linkProperties is null");
                return;
            }

            //获取类 LinkProperties的setHttpProxy方法
            Class<?> proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
            Class<?>[] setHttpProxyParams = new Class[1];
            setHttpProxyParams[0] = proxyPropertiesClass;
            Class<?> lpClass = Class.forName("android.net.LinkProperties");

            Method setHttpProxy = lpClass.getDeclaredMethod("setHttpProxy",setHttpProxyParams);setHttpProxy.setAccessible(true);

            // 获取类 ProxyProperties的构造函数
            Constructor<?> proxyPropertiesCtor = proxyPropertiesClass.getConstructor(String.class,int.class, String.class);
            // 实例化类ProxyProperties
            Object proxySettings =proxyPropertiesCtor.newInstance(host, port, null);
            //pass the new object to setHttpProxy
            Object[] params = new Object[1];
            params[0] = proxySettings;
            setHttpProxy.invoke(linkProperties, params);
            setEnumField(configuration, "STATIC", "proxySettings");

            //save the settings
            wifiManager.updateNetwork(configuration);
            wifiManager.disconnect();
            wifiManager.reconnect();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            mTextView.setText(e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            mTextView.setText(e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            mTextView.setText(e.getMessage());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            mTextView.setText(e.getMessage());
        } catch (InstantiationException e) {
            e.printStackTrace();
            mTextView.setText(e.getMessage());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            mTextView.setText(e.getMessage());
        }
    }

    public void setEnumField(Object obj, String value, String name)throws SecurityException, NoSuchFieldException,IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getField(name);
        f.set(obj, Enum.valueOf((Class<Enum>) f.getType(), value));
    }

    // getField只能获取类的public 字段.
    public Object getFieldObject(Object obj, String name)throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        Field f =
                obj.getClass().getField(name);
        Object out = f.get(obj); return out;
    }

    private void getInfo() {
        String infostr = "";
        //获取IMEI
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);

        String deviceId = telephonyManager.getDeviceId();
        Log.d(TAG, "deviceId: " + deviceId);
        infostr += "deviceId=" + deviceId + "\n";

        String number = telephonyManager.getLine1Number();
        Log.d(TAG, "number: " + number);
        infostr += "number=" + number + "\n";

        String deviceSoftwareVersion = telephonyManager.getDeviceSoftwareVersion();
        Log.d(TAG, "deviceSoftwareVersion: " + deviceSoftwareVersion);
        infostr += "deviceSoftwareVersion=" + deviceSoftwareVersion + "\n";

        String model = Build.MODEL;
        Log.d(TAG, "model: " + model);
        infostr += "model=" + model + "\n";

        String release = Build.VERSION.RELEASE;
        Log.d(TAG, "release version: " + release);
        infostr += "version=" + release + "\n";

        int sdk = Build.VERSION.SDK_INT;
        Log.d(TAG, "SDK: " + sdk);
        infostr += "sdk=" + sdk + "\n";

        //在wifi未开启状态下，仍然可以获取MAC地址，但是IP地址必须在已连接状态下否则为0
        String ip = null;
        WifiManager wifiMgr = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = (null == wifiMgr ? null : wifiMgr.getConnectionInfo());
        if (null != info) {
            ip = int2ip(info.getIpAddress());
        }

        Log.d(TAG, "MacAddress: " + getMac());
        Log.d(TAG, "IP: " + ip);
        infostr += "MacAddress=" + getMac() + "\n";
        infostr += "IP=" + ip + "\n";
        mTextView.setText(infostr);
    }

    public String int2ip(long ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    public long ip2int(String ip) {
        String[] items = ip.split("\\.");
        return Long.valueOf(items[0]) << 24
                | Long.valueOf(items[1]) << 16
                | Long.valueOf(items[2]) << 8 | Long.valueOf(items[3]);
    }

    public static String getMac() {
        String macSerial = "";
        try {
            Process pp = Runtime.getRuntime().exec(
                    "cat /sys/class/net/wlan0/address");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            String line;
            while ((line = input.readLine()) != null) {
                macSerial += line.trim();
            }

            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return macSerial;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getInfo();
        }
    }
}
