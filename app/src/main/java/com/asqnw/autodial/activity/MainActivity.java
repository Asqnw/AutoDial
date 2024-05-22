package com.asqnw.autodial.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.asqnw.autodial.R;
import com.asqnw.autodial.util.HttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity
{
    public static String authInfo = "Welcome";
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Loading...");
        pd.setCancelable(false);
        pd.show();
        new Thread(() -> {
            try
            {
                JSONObject json;
                final String deviceId = getDeviceID();
                String decode = new String(Base64.decode(new HttpClient().postReqStr("https://product.asqnw.com/", Base64.encodeToString(new JSONObject().put("name", "autoDial").put("id", deviceId).put("ver", "2").toString().getBytes(), Base64.DEFAULT)), Base64.DEFAULT));
                if ((json = new JSONObject(decode)).getString("code").equals("0"))
                {
                    authInfo = json.getString("msg");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        setContentView(R.layout.activity_main);
                        Toolbar toolbar = this.findViewById(R.id.toolbar);
                        toolbar.setTitle(R.string.app_name);
                        setSupportActionBar(toolbar);
                        TextView info = this.findViewById(R.id.info);
                        info.setGravity(Gravity.CENTER);
                        info.setText("欢迎使用本程序\n开发者：影幽网络科技开发组");
                        TextView info2 = this.findViewById(R.id.info2);
                        info2.setGravity(Gravity.CENTER);
                        info2.setText("请等待下方列出服务端列表或手动输入服务地址");
                        TextView info3 = this.findViewById(R.id.info3);
                        info3.setGravity(Gravity.CENTER);
                        info3.setText("开源地址：https://github.com/Asqnw/AutoDial/");
                        new Thread(() -> {
                            try (DatagramSocket socket = new DatagramSocket())
                            {
                                socket.setBroadcast(true);
                                byte[] sendData = "Asqnw_AutoDial_FindServer".getBytes();
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 11000);
                                socket.send(sendPacket);
                                socket.setSoTimeout(10000);
                                byte[] recvBuf = new byte[15000];
                                List<ServerInfo> servers = new ArrayList<>();
                                while (true)
                                {
                                    DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                                    try
                                    {
                                        socket.receive(receivePacket);
                                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                                        if (response.startsWith("Server System User Name:"))
                                        {
                                            String userName = response.substring("Server System User Name:".length());
                                            String serverIp = receivePacket.getAddress().getHostAddress();
                                            servers.add(new ServerInfo(userName, serverIp));
                                            new Handler(Looper.getMainLooper()).post(() -> updateUI(servers));
                                        }
                                    }
                                    catch (IOException e)
                                    {
                                        new Handler(Looper.getMainLooper()).post(() -> updateUI(servers));
                                        new Handler(Looper.getMainLooper()).post(() -> findViewById(R.id.progress).setVisibility(View.GONE));
                                        break;//超时后退出循环
                                    }
                                }
                            }
                            catch (IOException ignored)
                            {}
                        }).start();
                        Button button = this.findViewById(R.id.enter);
                        button.setText("连接");
                        button.setOnClickListener(v -> {
                            String input = ((EditText)this.findViewById(R.id.device)).getText().toString();
                            Matcher matcherIpv4 = Pattern.compile("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$").matcher(input);
                            if (matcherIpv4.matches())
                            {
                                button.setEnabled(false);
                                new Thread(() -> {
                                    try (Socket ignored = new Socket(input, PhoneCallActivity.port))
                                    {
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            Intent intent = new Intent(this, PhoneCallActivity.class);
                                            intent.putExtra("ip", input);
                                            intent.putExtra("safe", "Asqnw");
                                            startActivity(intent);
                                            finish();
                                        });
                                    }
                                    catch (IOException ignored)
                                    {
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            button.setEnabled(true);
                                            Toast.makeText(this, "连接失败", Toast.LENGTH_LONG).show();
                                        });
                                    }
                                }).start();
                            }
                            else
                                Toast.makeText(this, "地址不合法", Toast.LENGTH_LONG).show();
                        });
                        pd.dismiss();
                    });
                }
                else if (json.getString("code").equals("1"))
                {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("未授权").setMessage("当前设备未授权，请复制设备ID后联系作者暗影之风处理\n\nID：" + deviceId).setPositiveButton("退出", null).setNegativeButton("复制", null).setCancelable(false).create();
                        dialog.show();
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> System.exit(0));
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                            ClipboardManager cm = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData mClipData = ClipData.newPlainText("Label", deviceId);
                            cm.setPrimaryClip(mClipData);
                        });
                        pd.dismiss();
                    });
                }
                else if (json.getString("code").equals("2"))
                {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("有更新").setMessage("当前版本已经过期，联系作者暗影之风处理").setPositiveButton("退出", null).setCancelable(false).create();
                        dialog.show();
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> System.exit(0));
                        pd.dismiss();
                    });
                }
                else
                {
                    String msg = json.getString("msg");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("服务器提示").setMessage(msg).setPositiveButton("退出", null).setCancelable(false).create();
                        dialog.show();
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> System.exit(0));
                        pd.dismiss();
                    });
                }
            }
            catch (HttpClient.HttpException.UnAuthorize | HttpClient.HttpException.Forbidden | HttpClient.HttpException.Unknown | HttpClient.HttpException.ServerError | UnknownHostException | JSONException ignored)
            {
                new Handler(Looper.getMainLooper()).post(() -> {
                    AlertDialog dialog = new AlertDialog.Builder(this).setTitle("获取授权异常").setMessage("无法获取授权信息，请联系作者暗影之风处理").setPositiveButton("退出", null).setCancelable(false).create();
                    dialog.show();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> System.exit(0));
                    pd.dismiss();
                });
            }
        }).start();
    }

    private void updateUI(List<ServerInfo> servers)
    {
        ((ListView)findViewById(R.id.devices)).setAdapter(new BaseAdapter() {
            @Override
            public int getCount()
            {
                return servers.isEmpty() ? 1 : servers.size();
            }

            @Override
            public Object getItem(int position)
            {
                return servers.get(position);
            }

            @Override
            public long getItemId(int position)
            {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                Button view;
                if (!servers.isEmpty())
                {
                    view = (convertView instanceof Button) ? (Button) convertView : new Button(parent.getContext());
                    ServerInfo server = servers.get(position);
                    view.setText(String.format("用户名: %s, IP: %s", server.userName, server.ip));
                    view.setOnClickListener(v -> ((EditText)findViewById(R.id.device)).setText(server.ip));
                }
                else
                {
                    view = new Button(parent.getContext());
                    view.setEnabled(false);
                    view.setText("未找到服务器，请确保服务器与本机是同局域网或手动输入地址");
                }
                return view;
            }
        });
    }

    static final class ServerInfo
    {
        String userName;
        String ip;

        ServerInfo(String userName, String ip)
        {
            this.userName = userName;
            this.ip = ip;
        }
    }

    public static String getDeviceID()
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("MD5").digest((Arrays.toString(Build.FINGERPRINT.getBytes()) + Arrays.toString(String.valueOf(Build.TIME).getBytes())).getBytes());
            StringBuilder sb = new StringBuilder();
            char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
            for (byte b : digest)
                sb.append(cArr[(b >> 4) & 15]).append(cArr[b & 15]);
            String replaceAll = (sb + Long.toHexString(Build.TIME / 1000)).replaceAll("(.{5})","$1-");
            return replaceAll.charAt(replaceAll.length() - 1) == '-' ? replaceAll.substring(0,replaceAll.length() - 1) : "";
        }
        catch (Exception e)
        {
            return "00000-00000-00000-00000-00000-00000-00000-00000";
        }
    }
}