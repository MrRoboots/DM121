package com.example.dm121;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.dascom.print.DeviceListActivity;
import com.dascom.print.SmartPrint;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {


    private BluetoothAdapter mBluetoothAdapter = null;
    private Boolean openflag = false;
    private boolean ynflag = false;

    private boolean btopenflag = false;
    private boolean wfopenflag = false;


    //端口标识
    public static int CONNECT_USB = 0;
    public static int CONNECT_BT = 1;
    public static int CONNECT_WIFI = 2;

    private int cdevice = 3;
    private SmartPrint mSmartPrint;
    private TextView mTvState;
    private EditText mEditText;
    private EditText bt_pstion_x;
    private EditText bt_pstion_y;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    @SuppressLint("WrongViewCast")
    private void initView() {
        mTvState = (TextView) findViewById(R.id.tv_state);
        mEditText = (EditText) findViewById(R.id.edt_print_str);
        bt_pstion_x = (EditText) findViewById(R.id.edt_x);
        bt_pstion_y = (EditText) findViewById(R.id.edt_y);
    }

    public void posprint(View view) {
        if (mSmartPrint.DSIsLinkedBT() != STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        String str = mEditText.getText().toString(); //输入的内容
        if (str.isEmpty()) {
            Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
            return;
        }
        Boolean res = false;
        res = mSmartPrint.DSPrintData(str, false);
        mTvState.setText(res ? "打印成功" : "打印失败!");
    }


    //实例化蓝牙
    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    public void initbt(View view) {
        if (cdevice == 0) {
            if (openflag == true && mSmartPrint != null) {
                mSmartPrint.DSCloseUsb();
                openflag = false;
            }
            mSmartPrint = null;
        } else if (cdevice == 2) {

        } else if (cdevice == 1) {
            return;
        }
        mSmartPrint = new SmartPrint(MainActivity.this, mHandler, CONNECT_BT);

        cdevice = CONNECT_BT;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            mSmartPrint.DSOpenBT(this);
        }
        mTvState.setText("蓝牙实例化成功");

        isGrantExternalRW(this);
    }

    /**
     * 获取储存权限
     *
     * @param activity
     */
    public static void isGrantExternalRW(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            activity.requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        }
    }


    //连接蓝牙
    public void connectbt(View view) {
        if (cdevice != 1) {
            Toast.makeText(this, "没有选择蓝牙方式",
                    Toast.LENGTH_SHORT).show();
            return;
        }


        if (btopenflag) {
            Toast.makeText(this, "已经是连接状态。。。",
                    Toast.LENGTH_SHORT).show();
            return;
        } else if (mSmartPrint.DSGetState() != 0 && mSmartPrint.DSGetState() != 4 && mSmartPrint.DSGetState() != -1) {
            Toast.makeText(this, "正在连接，请稍候。。。",
                    Toast.LENGTH_SHORT).show();
            return;
        } else {
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        }
    }

    //关闭蓝牙
    public void closebt(View view) {
        if (cdevice != 1) {
            Toast.makeText(this, "没有选择蓝牙方式", Toast.LENGTH_SHORT).show();
            return;
        }
        mSmartPrint.DSCloseBT();
        btopenflag = false;
    }

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_LISTEN = 1; // now listening for incoming
    // connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing
    // connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote
    private String btAddress = "";
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (cdevice == 1) {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:

                        switch (msg.arg1) {
                            case STATE_CONNECTED:
                                mTvState.setText(R.string.title_connected_to);
                                btopenflag = true;
                                btAddress = mSmartPrint.DSGetBTAddress();
                                //mTitle.append(mConnectedDeviceName);
                                break;
                            case STATE_CONNECTING:
                                mTvState.setText(R.string.title_connecting);
                                break;
                            case STATE_LISTEN:
                            case STATE_NONE:
                                mTvState.setText(R.string.title_not_connected);
                                break;
                            case 4:
                                mTvState.setText("设备丢失，正在尝试重新连接。。。");
                                btopenflag = false;
                                new reConnectPrinter().start();
                                break;

                            case -1:
                                //mTvState.setText("连接失败#错误码："+mJr.GetBTConErrorCode());

                                break;
                        }
                        break;

                    case 6:
                        mTvState.setText(msg.getData().getString("local"));
                        break;
                }
            }

            if (cdevice == 2) {
                if (msg.what == 1) {


                    mTvState.setText("Client: " + msg.getData().getString("READDATE"));
                    // wfopenflag=mJr.DSWifiState();
                }
            }
        }
    };

    boolean whenDestroy = false;

    public void DSLineFeed(View view) {
        if (mSmartPrint.DSIsLinkedBT() != STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        // ouTextView.setText(String.valueOf(mSmartPrint.Printer_Setup_Feed()));
        boolean sb = mSmartPrint.DSLineFeed();
        mTvState.setText(sb ? "换行成功" : "换行失败");
    }

    public void DSPrintImg(View view) {
        String tem = bt_pstion_x.getText().toString();
        if (tem.equals("") == true) {
            tem = "0";
        }
        double temp1 = Double.parseDouble(tem);
        String tempv = bt_pstion_y.getText().toString();
        if (tempv.equals("") == true) {
            tempv = "0";
        }
        Bitmap bitmap = null;
        try {
            bitmap = mSmartPrint.DSDMDrawQR("www.baidu.com", 45);
            // mSmartPrint.DSPrintCode128((float)0,(float)0,80,30,"www.baidu.com");
            mSmartPrint.DSPrintDrawImage2(Double.parseDouble(tem), Double.parseDouble(tempv), bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Toast.makeText(this, true ? "打印图片成功" : "打印失败", Toast.LENGTH_SHORT).show();
    }

    //送黑标纸到打印起始位
    public void sendBlackPrint(View view) {
        if (mSmartPrint.DSIsLinkedBT() != STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        boolean sb = mSmartPrint.DSBlackToPrint();
        mTvState.setText(sb ? "到打印位-成功" : "到打印位-失败");

    }

    //送黑标纸到撕纸位
    public void sendBlackCut(View view) {
        if (mSmartPrint.DSIsLinkedBT() != STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        boolean sb = mSmartPrint.DSBlackToCut();
        mTvState.setText(sb ? "到撕纸位-成功" : "到撕纸位-失败");
    }

    public void printPosition(View view) {
        if (mSmartPrint.DSIsLinkedBT() != STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        String tem = bt_pstion_x.getText().toString();
        if (tem.equals("") == true) {
            Toast.makeText(getApplicationContext(), "请输入横向数据",
                    Toast.LENGTH_LONG).show();
            return;
        }
        double temp1 = Double.parseDouble(tem);
        String tempv = bt_pstion_y.getText().toString();
        if (tempv.equals("") == true) {
            Toast.makeText(getApplicationContext(), "请输入纵向数据",
                    Toast.LENGTH_LONG).show();
            return;
        }
        double temp2 = Double.parseDouble(tempv);
        boolean sb = mSmartPrint.DSSetLocation(temp1, temp2);
        mTvState.setText(sb ? "打印位置设置成功" : "打印位置设置失败");
    }


    class reConnectPrinter extends Thread {
        public void run() {
            while ((!btopenflag) && (!whenDestroy)) {
                if (mSmartPrint.DSGetState() == 0 || mSmartPrint.DSGetState() == 4) {
                    mSmartPrint.DSLinkBT(btAddress);
                    Message msg = mHandler.obtainMessage(6);
                    Bundle bundle = new Bundle();
                    bundle.putString("local", "正在尝试重新连接。。。");
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                    System.out.println("正在尝试重新连接。。。");
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    System.out.println("sbma " + mSmartPrint.DSGetBTAddress());
                    mSmartPrint.DSLinkBT();

                }

                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a session

                } else {
                    // User did not enable Bluetooth or an error occured

                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }

            case 100:
                if (resultCode == 10) {
                    Toast.makeText(this, "Nothing has choosed",
                            Toast.LENGTH_SHORT).show();
                }
                if (resultCode == 20) {

//                    pdf=new PdfPrint(pdfpath);
//
//                    bmp=pdf.getBitmap();

                }
            case 200:
                if (resultCode == 202) {
                    Toast.makeText(this, "Nothing has choosed",
                            Toast.LENGTH_SHORT).show();
                }
                if (resultCode == 201) {
//                    hexpath=data.getExtras().getString("path");
//                    ds210tvstate.setText("二进制文件:"+hexpath);
//                    System.out.println(hexpath);

                }
        }
    }

    //测试样张 美团外卖
    public void table1(View v) {
        boolean res = isConnectPrinter();
        if (!res) {
            return;
        }

        int num = 1;
        int timeB = 2000;
        boolean a = false;
        for (int i = 0; i < num; i++) {

            mSmartPrint.DSReset();
            mSmartPrint.DSSetPrintMode(1);                //设置页模式/标准模式
            //mSmartPrint.DSSetFontMode(false,false,false);//设置字体样式 非倍宽倍高下划线
//            mSmartPrint.DSSetBold(false);				//粗体
            mSmartPrint.DSSetJustification(2);            //标题居中对齐
            mSmartPrint.DSPrintData("美团外卖\r\n", false);//发送打印数据
            mSmartPrint.DSFeedPaper(0.1);                //走纸0.2英寸
            mSmartPrint.DSSetJustification(0);            //文本内容左对齐
            mSmartPrint.DSPrintData("嵘基快餐（西乡店）（第一联）\r\n", false);

            mSmartPrint.DSPrintData("- - - - - - - - - - - - - - - - - - - - -\r\n", false);
//            mSmartPrint.DSSetBold(false);				//非粗体
            mSmartPrint.DSPrintData("下单时间：2015-09-29 11:46:33\r\n", false);
//            mSmartPrint.DSSetBold(false);				//粗体

            mSmartPrint.DSPrintData("- - - - - - - - - - - - - - - - - - - - -\r\n", false);
//            mSmartPrint.DSSetBold(false);				//非粗体
            mSmartPrint.DSPrintData("菜名", false);
            mSmartPrint.DSSetAbsoluteHposition(1.3);
            mSmartPrint.DSPrintData("数量", false);
            mSmartPrint.DSSetAbsoluteHposition(2.2);
            mSmartPrint.DSPrintData("小计\r\n", false);
//            mSmartPrint.DSSetBold(false);				//粗体

            mSmartPrint.DSPrintData("- - - - - - - - - - - - - - - - - - - - -\r\n", false);
            mSmartPrint.DSPrintData("脆皮鸡", false);
            mSmartPrint.DSSetAbsoluteHposition(1.3);
            mSmartPrint.DSPrintData("X1", false);
            mSmartPrint.DSSetAbsoluteHposition(2.2);
            mSmartPrint.DSPrintData("10\r\n", false);
            mSmartPrint.DSPrintData("章鱼丸", false);
            mSmartPrint.DSSetAbsoluteHposition(1.3);
            mSmartPrint.DSPrintData("X1", false);
            mSmartPrint.DSSetAbsoluteHposition(2.2);
            mSmartPrint.DSPrintData("6\r\n", false);

            mSmartPrint.DSPrintData("回锅肉饭", false);
            mSmartPrint.DSSetAbsoluteHposition(1.3);
            mSmartPrint.DSPrintData("X2", false);
            mSmartPrint.DSSetAbsoluteHposition(2.2);
            mSmartPrint.DSPrintData("24\r\n", false);

            mSmartPrint.DSPrintData("- - - - - - - - - - - - - - - - - - - - -\r\n", false);
//            mSmartPrint.DSSetBold(false);				//非粗体
            mSmartPrint.DSPrintData("【新用户专享】通过在线支付下首单立减15.0\r\n", false);
//            mSmartPrint.DSSetBold(false);				//粗体

            mSmartPrint.DSPrintData("- - - - - - - - - - - - - - - - - - - - -\r\n", false);
            mSmartPrint.DSPrintData("合计", false);
            mSmartPrint.DSSetAbsoluteHposition(1.3);
            mSmartPrint.DSPrintData("（已付款）25元\r\n", false);

            mSmartPrint.DSPrintData("- - - - - - - - - - - - - - - - - - - - -\r\n", false);
            mSmartPrint.DSPrintData("深圳市宝安区甲岸村\r\n", false);
            mSmartPrint.DSPrintData("留仙三路**号\n", false);
            mSmartPrint.DSSetAbsoluteHposition(1.3);
            Bitmap bitmap = null;
            try {
                bitmap = mSmartPrint.DSDMDrawQR("www.baidu.com", 50);
                mSmartPrint.DSPrintDrawImage2(0, 0, bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mSmartPrint.DSSetJustification(1);            //居中对齐
            mSmartPrint.DSPrintData("*******在线支付订单*******\r\n", false);
            mSmartPrint.DSSetJustification(0);            //左对齐
            mSmartPrint.DSPrintData("- - - - - - - - - - - - - - - - - - - - -\r\n", false);
            a = mSmartPrint.DSFormFeed();
            try {
                Thread.sleep(timeB);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (a)
            Toast.makeText(this, "发送完成，请等待打印。。。", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "发送失败。。。", Toast.LENGTH_SHORT).show();
    }

    //判断是否连接打印机
    private boolean isConnectPrinter() {
        if (cdevice == 1) {
            if (!btopenflag) {
                Toast.makeText(this, "蓝牙没有连接", Toast.LENGTH_SHORT)
                        .show();
                return false;
            }
            return true;
        } else {
            Toast.makeText(this, "没有连接打印机。。。", Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
    }

    /**
     * 模拟发票套打
     *
     * @param view
     */
    public void taoda(View view) {
        boolean res = isConnectPrinter();
        if (!res) {
            return;
        }
        mSmartPrint.DSBlackToPrint();  //送黑标纸到打印起始位
        mSmartPrint.DSSetLocation(0.5, 0.4);  //从打印位开始计算的位置 .单位英寸.小误差可以忽略
        mSmartPrint.DSPrintData("83435392", false);
        mSmartPrint.DSSetLocation(1.5, 0);  //从打印位开始计算的位置 .单位英寸.小误差可以忽略
        mSmartPrint.DSPrintData("661725498862", false);
        mSmartPrint.DSLineFeed();
        mSmartPrint.DSSetLocation(0.5, 0);
        mSmartPrint.DSPrintData("济南厉下香有名羊汤店", false);
        mSmartPrint.DSLineFeed();
        mSmartPrint.DSSetLocation(0.5, 0.2);
        mSmartPrint.DSPrintData("92370102MA3ERQDW8U", false);
        mSmartPrint.DSLineFeed();
        mSmartPrint.DSSetLocation(0.2, 0);
        mSmartPrint.DSPrintData("2018-05-25", false);
        mSmartPrint.DSSetLocation(1.5, 0);  //X是相对最左边算的,同一行也是.不是相对同行的第一个
        mSmartPrint.DSPrintData("林1", false);
        mSmartPrint.DSLineFeed();
        mSmartPrint.DSSetLocation(0.5, 0);
        mSmartPrint.DSPrintData("个人", false);
        mSmartPrint.DSSetLocation(0.5, 2);
        mSmartPrint.DSPrintData("$1.11", false);
        mSmartPrint.DSLineFeed();
        mSmartPrint.DSSetLocation(0.5, 0);
        mSmartPrint.DSPrintData("壹圓壹角壹分", false);
        mSmartPrint.DSLineFeed();
        mSmartPrint.DSSetLocation(0.4, 0);
        mSmartPrint.DSPrintData("84671965451690558164", false);
    }


    /**
     * 测试打印
     * 姓名、证件号、票号 可能不显示
     * 线路 日期 金额 会有值
     */
    public void test1(View v) {
        boolean res = isConnectPrinter();
        if (!res) {
            return;
        }

        int num = 1;
        int timeB = 2000;
        boolean a = false;
        for (int i = 0; i < num; i++) {

            mSmartPrint.DSReset();
            mSmartPrint.DSBlackToPrint();  //送黑标纸到打印起始位
//            mSmartPrint.DSSetPrintMode(1); //设置页模式/标准模式  会显示a b c d

            //mSmartPrint.DSSetFontMode(false,false,false);//设置字体样式 非倍宽倍高下划线

//            mSmartPrint.DSSetBold(false);				//粗体
//            mSmartPrint.DSSetJustification(2);            //标题居中对齐
//            mSmartPrint.DSPrintData("美团外卖\r\n", false);//发送打印数据

            mSmartPrint.DSSetJustification(0);            //文本内容左对齐
            mSmartPrint.DSPrintData("线路：成都-温江", false);
            mSmartPrint.DSSetAbsoluteHposition(2.2);
            mSmartPrint.DSPrintData("日期：" + getNowDateTime(), false);
            mSmartPrint.DSSetAbsoluteHposition(2.2);
            mSmartPrint.DSPrintData("姓名：小明", false);
            mSmartPrint.DSSetAbsoluteHposition(2.2);
            mSmartPrint.DSPrintData("证件号：350781196403074981", false);
            mSmartPrint.DSSetAbsoluteHposition(2.2);
            mSmartPrint.DSPrintData("金额：20.00元", false);
            mSmartPrint.DSSetAbsoluteHposition(2.2);
            mSmartPrint.DSPrintData("票号：00000001", false);
            mSmartPrint.DSFeedPaper(5);                //走纸0.2英寸

            a = mSmartPrint.DSFormFeed();
            try {
                Thread.sleep(timeB);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (a)
            Toast.makeText(this, "发送完成，请等待打印。。。", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "发送失败。。。", Toast.LENGTH_SHORT).show();
    }


    public String getNowDateTime() {
        Calendar c = Calendar.getInstance();
        String year = c.get(Calendar.YEAR) + "";
        String month = c.get(Calendar.MONTH) + "";
        String day = c.get(Calendar.DAY_OF_MONTH) + "";
        String hour = c.get(Calendar.HOUR_OF_DAY) + "";
        String minute = c.get(Calendar.MINUTE) + "";
        String second = c.get(Calendar.SECOND) + "";
        return year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
    }

}
