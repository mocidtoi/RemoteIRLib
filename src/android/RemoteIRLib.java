package com.thanhnv.remoteirlib;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jingxun.jingxun.bean.DeviceItemBean;
import com.jingxun.jingxun.helper.ConfigureHelper;
import com.jingxun.jingxun.helper.DeviceProbeHelper;
import com.jingxun.jingxun.helper.RequestHelper;
import com.jingxun.jingxun.helper.SendCommandHelper;
import com.jingxun.jingxun.listener.ConfigureListener;
import com.jingxun.jingxun.listener.IProbeCallBack;
import com.jingxun.jingxun.listener.ResponseCallBack;


import android.telephony.TelephonyManager;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.util.Log;
import java.util.LinkedList;
import java.util.List;

import android.widget.Toast;



/**
 * This class echoes a string called from JavaScript.
 */
public class RemoteIRLib extends CordovaPlugin {
    private static final String TAG = "RemoteIRLib";

    private static final int SEND_COMMAND_SUCCESS = 1, 
                             SEND_COMMAND_FAILED = 2,
                             PROBE_SUCCESS = 5,
                             PROBE_FAILED = 6,
                             CONFIGURE_SUCCESS = 7,
                             CONFIGURE_FAILED = 8;


    private CallbackContext callbackContext = null;
    private Context mContext = null;
    private boolean isResultSent = false;
    private boolean isConfigRunning = false;

    // private String deivceId, deivceKey, deviceIp, deviceServerIp;


    private JSONObject deviceConfigureJSON = null;

    private JSONObject deivceProbeJSON = null;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            PluginResult res;
            switch (msg.what) {
                case CONFIGURE_SUCCESS:
                    if (!isResultSent) {
                        res = new PluginResult(PluginResult.Status.OK, deviceConfigureJSON);
                        sendPluginResult(res);
                        isResultSent = true;
                        deviceConfigureJSON = null;
                    }
                    isConfigRunning = false;
                    break;
                case CONFIGURE_FAILED:
                    if (!isResultSent) {
                        res = new PluginResult(PluginResult.Status.OK, "Error");
                        sendPluginResult(res);
                        isResultSent = true;
                    }
                    isConfigRunning = false;
                    break;
                case PROBE_SUCCESS:
                    DeviceProbeHelper.getInstance().stopProbe();
                    res = new PluginResult(PluginResult.Status.OK, deivceProbeJSON);
                    sendPluginResult(res);
                    deivceProbeJSON = null;                    
                    break;
                case PROBE_FAILED:
                    DeviceProbeHelper.getInstance().stopProbe();
                    res = new PluginResult(PluginResult.Status.OK, "Error");
                    sendPluginResult(res);
                    break;
                case SEND_COMMAND_SUCCESS:
                    res = new PluginResult(PluginResult.Status.OK, "Send command success");
                    sendPluginResult(res);
                    break;
                case SEND_COMMAND_FAILED:
                    res = new PluginResult(PluginResult.Status.OK, "Error");
                    sendPluginResult(res);
                    break;
                default:
                    break;
            }

            
        }
    };





    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mContext = this.cordova.getActivity();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (action.equals("configure")) {
            String ssid = args.getString(0);
            String password = args.getString(1);

            return configure(ssid, password);
        }
    	else if(action.equals("cancelConfig")){
            return cancelConfig();
        }
        else if(action.equals("probe")){
            String deviceId = args.getString(0);
            String deviceKey = args.getString(1);

            probe(deviceId, deviceKey);
            return true;
        } 
        else if (action.equals("sendCommand")){
            String deviceId = args.getString(0);
            String deviceIp = args.getString(1);
            String deviceServerIp = args.getString(2);
            String irData = args.getString(3);
            sendCommand(deviceId, deviceIp, deviceServerIp, irData);
            return true;
        }

        return false;
    }

    private void sendPluginResult(PluginResult res){
        if (this.callbackContext != null) {
            res.setKeepCallback(true);
            this.callbackContext.sendPluginResult(res);
        }
    }

    private boolean configure(String ssid, String passWifi) {
        if( !isConfigRunning ) {
            isResultSent = false;
            isConfigRunning = true;
            ConfigureHelper.getInstance().startConfigure(mContext, ssid, passWifi, new ConfigureListener() {
                @Override
                public void onSuccess(DeviceItemBean deviceItemBean) {
                    try{
                        deviceConfigureJSON = new JSONObject();
                        deviceConfigureJSON.put("DEVICE_ID", deviceItemBean.getDeviceId());
                        deviceConfigureJSON.put("DEVICE_KEY", deviceItemBean.getKey());
                        mHandler.sendEmptyMessage(CONFIGURE_SUCCESS);
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                        PluginResult res = new PluginResult(PluginResult.Status.OK, "JSONException");
                        sendPluginResult(res);
                    }
                }

                @Override
                public void onFailed(Exception e) {
                    mHandler.sendEmptyMessage(CONFIGURE_FAILED);
                }
            });
            return true;
        }
        return false;
    }
    private boolean cancelConfig() {
        ConfigureHelper.getInstance().stopConfigure();
        return true;
    }
    private void probe(String deviceId, String deviceKey) {
        RequestHelper.getInstance().releaseSocket();

        Log.d(TAG, deviceId + " - " + deviceKey);

        LinkedList<DeviceItemBean> mList = new LinkedList<DeviceItemBean>();

        DeviceItemBean bean = new DeviceItemBean.DeivceItemBuilder()
                .deviceId(deviceId)
                .key(deviceKey)
                .build();
        mList.add(bean);

        DeviceProbeHelper.getInstance().startProbe(mContext, mList, new IProbeCallBack() {
            @Override
            public void onCallBack(List<DeviceItemBean> list) {
                Log.d(TAG, "probe ---->");

                if (list.get(0).isOnline()){
                    DeviceItemBean bean = list.get(0);

                    try{
                        deivceProbeJSON = new JSONObject();
                        deivceProbeJSON.put("DEVICE_ID", bean.getDeviceId());
                        deivceProbeJSON.put("DEVICE_KEY", bean.getKey());
                        deivceProbeJSON.put("DEVICE_IP", bean.getIp());
                        deivceProbeJSON.put("DEVICE_SERVER_IP", bean.getServerIp());
                        createSocket(bean.getIp());
                        mHandler.sendEmptyMessage(PROBE_SUCCESS);
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                        PluginResult res2 = new PluginResult(PluginResult.Status.OK, "JSONException");
                        sendPluginResult(res2);
                    }
                } else {
                    Log.d("DeviceBeanManager", "failed probe ");
                    mHandler.sendEmptyMessage(PROBE_FAILED);
                }
            }
        });




    }
    private void sendCommand(String deviceId, String deviceIp, String deviceServerIp, String irData) {
        if (irData == null || irData.equals("")){
            return;
        }

        String param = SendCommandHelper.transitIRCode(deviceId, irData);

        // Toast.makeText(mContext, "device Id : " + deviceId, Toast.LENGTH_SHORT).show();
        // Toast.makeText(mContext, "device IP : " + deviceIp, Toast.LENGTH_SHORT).show();
        // Toast.makeText(mContext, "server IP : " + deviceServerIp, Toast.LENGTH_SHORT).show();
        // Toast.makeText(mContext, "irdata  : " + irData, Toast.LENGTH_SHORT).show();

        Log.d("DeviceBeanManager", "param send ir data : " + param);

        DeviceItemBean bean = new DeviceItemBean.DeivceItemBuilder()
                .deviceId(deviceId)
                .ip(deviceIp)
                .serverIp(deviceServerIp)
                .build();

        RequestHelper.getInstance().requestData(bean, param, new ResponseCallBack() {
            @Override
            public void onSuccess(int i, JSONObject jsonObject) {
                Log.d("DeviceBeanManager", "send command success");
                mHandler.sendEmptyMessage(SEND_COMMAND_SUCCESS);
            }

            @Override
            public void onFailed(Exception e) {
                Log.d("DeviceBeanManager", "send onFailed: " + e.toString());
                mHandler.sendEmptyMessage(SEND_COMMAND_FAILED);
            }
        });
    }

    private void createSocket(String ip) {
        RequestHelper.getInstance().createSocket(ip);
    }
}
