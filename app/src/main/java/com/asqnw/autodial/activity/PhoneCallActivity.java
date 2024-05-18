package com.asqnw.autodial.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.asqnw.autodial.R;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 暗影之风
 * @CreateTime 2024-05-13 20:35:29
 * @Description 拨号活动
 * @noinspection JavadocDeclaration
 */
public class PhoneCallActivity extends AppCompatActivity
{
    public static final int port = 12453;
    private boolean callStateListenerRegistered = false;
    private boolean isFree = false;
    private long tel = 0;
    private PrintWriter out;
    private Button nextButton;
    private final List<String> list = new ArrayList<>();
    private BaseAdapter adapter;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);
        String safe = getIntent().getStringExtra("safe");
        if (safe == null || !safe.equals("Asqnw"))
        {
            Toast.makeText(this, "非法进入", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        registerCallStateListener();
        new Thread(() -> connect(getIntent().getExtras().getString("ip"))).start();
        this.toolbar = this.findViewById(R.id.child_toolbar);
        this.toolbar.setTitle(R.string.app_name);
        ((TextView)this.findViewById(R.id.tips)).setText(MainActivity.authInfo);
        (this.nextButton = this.findViewById(R.id.next)).setOnClickListener(v -> {
            this.nextButton.setEnabled(false);
            new Thread(() -> {
                if (this.isFree && this.out != null)
                    this.out.println("ok");
            }).start();
        });
        this.nextButton.setText("报告服务器");
        ((ListView)this.findViewById(R.id.num_list)).setAdapter(this.adapter = new BaseAdapter() {
            @Override
            public int getCount()
            {
                return list.size();
            }

            @Override
            public Object getItem(int position)
            {
                return position;
            }

            @Override
            public long getItemId(int position)
            {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                TextView tv = new TextView(parent.getContext());
                tv.setText(list.get(position));
                return tv;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == port)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && isFree && this.tel != 0)
                this.call(this.tel);
        }
    }

    private void connect(String ip)
    {
        try (Socket socket = new Socket(ip, port); PrintWriter out = new PrintWriter(socket.getOutputStream(), true); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())))
        {
            out.println("ok");
            this.out = out;
            while (true)
            {
                String recv = in.readLine().replaceAll("-", "").replaceAll(" ", "");
                try
                {
                    Long.parseLong(recv);
                }
                catch (NumberFormatException ignored)
                {
                    out.println("ok");
                    continue;
                }
                if (recv.length() <= 4)
                {
                    out.println("ok");
                    continue;
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    this.tel = Long.parseLong(recv);
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED && isFree && this.tel != 0)
                        this.call(this.tel);
                    else
                        new AlertDialog.Builder(this).setTitle("申请电话权限").setMessage("需要申请拨打电话权限才能继续，点击跳转").setCancelable(false).setPositiveButton("跳转", (dialog1, which) -> ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, port)).setNegativeButton("取消", null).create().show();
                });
            }
        }
        catch (IOException ignored)
        {
            new Handler(Looper.getMainLooper()).post(() -> {
                startActivity(new Intent(this, MainActivity.class));
                Toast.makeText(this, "服务端已经断连", Toast.LENGTH_LONG).show();
                finish();
            });
        }
    }

    private void executeState(int state)
    {
        if (state == TelephonyManager.CALL_STATE_IDLE)
        {
            this.isFree = true;
            this.nextButton.setEnabled(true);
//            if (this.out != null && this.tel != 0)
//                new Thread(() -> out.println("ok")).start();
        }
        else
        {
            this.nextButton.setEnabled(false);
            this.isFree = false;
        }
    }

    private void call(long tel)
    {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + tel));
        startActivity(callIntent);
        if (this.tel != 0)
        {
            this.list.add(String.valueOf(this.tel));
            this.adapter.notifyDataSetChanged();
            this.toolbar.setSubtitle("已拨：" + this.list.size());
        }
    }

    private void registerCallStateListener()
    {
        if (!this.callStateListenerRegistered)
        {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
                {
                    telephonyManager.registerTelephonyCallback(getMainExecutor(), this.callStateListener);
                    this.callStateListenerRegistered = true;
                }
            }
            else
            {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                this.callStateListenerRegistered = true;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static abstract class CallStateListener extends TelephonyCallback implements TelephonyCallback.CallStateListener
    {
        @Override
        abstract public void onCallStateChanged(int state);
    }

    private final CallStateListener callStateListener = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ?
            new CallStateListener() {
                @Override
                public void onCallStateChanged(int state)
                {
                    executeState(state);
                }
            } : null;

    private final PhoneStateListener phoneStateListener = (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) ?
            new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String phoneNumber)
                {
                    executeState(state);
                }
            }
            : null;
}
