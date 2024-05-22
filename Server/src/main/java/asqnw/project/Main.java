package asqnw.project;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class Main
{
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args)
    {
        int port = 10000;
        System.out.println("欢迎使用Asqnw产品--AutoDial授权服务器\n\n现在准备启动必须内容，请稍等");
        System.out.println("一、设置服务器端口");
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].startsWith("-port"))
            {
                try
                {
                    port = Integer.parseInt(args[i + 1]);
                    System.out.println("1.1设置端口：" + port);
                }
                catch (Exception e)
                {
                    System.out.println("1.1读取端口参数异常，使用默认参数10000");
                }
            }
        }
        if (port == 10000)
            System.out.println("1.1未设置端口参数，使用默认参数10000");
        System.out.println("二、加载授权和更新文件");
        AtomicReference<JSONObject> info = new AtomicReference<>();
        try
        {
            String content = Files.readString(Paths.get("author.json"));
            info.set(new JSONObject(content));
            if (info.get().isEmpty())
                throw new JSONException("File Message is empty");
        }
        catch (IOException ignored)
        {
            System.out.println("2.1授权和更新文件不存在，退出执行");
            return;
        }
        catch (JSONException ignored)
        {
            System.out.println("2.1授权和更新文件异常，退出执行");
            return;
        }
        finally
        {
            System.out.println("示例文件样本：" + new JSONObject() {{
                this.put("ver", "1").put("auth", new JSONArray() {{
                    this.put(new JSONObject() {{
                        this.put("id", "xxxxx-xxxxx").put("name", "admin").put("time", String.valueOf(System.currentTimeMillis()));
                    }}).put(new JSONObject() {{
                        this.put("id", "xxxxx-xxxxx-xxxxx").put("name", "admin").put("time", String.valueOf(System.currentTimeMillis()));
                    }});
                }});
            }});
        }
        System.out.println("准备完成，启动服务器");
        new HttpServer().start(request -> {
            HashMap<String, String> response = new HashMap<>();
            if (request.get(HttpServer.URL).get(0).equals("/"))
            {
                String body;
                System.out.println(request);
                ArrayList<String> postBody = request.get(HttpServer.BODY);
                if ((body = (postBody == null ? "" : postBody.get(0))).isEmpty())//GET请求
                {
                    response.put("403 Forbidden", "");
                }
                else
                {
                    String strBody = new String(Base64.getDecoder().decode(body.replaceAll(" ", "").replaceAll("\n", "")));
                    try
                    {
                        JSONObject json;
                        String name = (json = new JSONObject(strBody)).getString("name");
                        String id = json.getString("id");
                        String ver = json.getString("ver");
                        if (name.equals("autoDial"))
                        {
                            String content = Files.readString(Paths.get("author.json"));
                            info.set(new JSONObject(content));
                            if (info.get().getString("ver").equals(ver))
                            {
                                JSONArray auth = info.get().getJSONArray("auth");
                                boolean ok = false;
                                for (int i = 0; i < auth.length(); i++)
                                {
                                    long time;
                                    JSONObject jo = auth.getJSONObject(i);
                                    if (jo.getString("id").equals(id) && (time = Long.parseLong(jo.getString("time"))) > System.currentTimeMillis())
                                    {
                                        response.put("200 OK", Base64.getEncoder().encodeToString(new JSONObject().put("code", "0").put("msg", "你好！" +jo.getString("name") + "\n\n欢迎使用AutoDial程序\n让我们一起向暗影致敬\n授权到期时间" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(time)).toString().getBytes()).replaceAll(" ", "").replaceAll("\n", ""));
                                        ok = true;
                                        break;
                                    }
                                    else if (jo.getString("id").equals(id))
                                    {
                                        response.put("200 OK", Base64.getEncoder().encodeToString(new JSONObject().put("code", "1").toString().getBytes()).replaceAll(" ", "").replaceAll("\n", ""));
                                        ok = true;
                                        break;
                                    }
                                }
                                if (!ok)
                                    response.put("200 OK", Base64.getEncoder().encodeToString(new JSONObject().put("code", "1").toString().getBytes()).replaceAll(" ", "").replaceAll("\n", ""));
                            }
                            else
                            {
                                response.put("200 OK", Base64.getEncoder().encodeToString(new JSONObject().put("code", "2").toString().getBytes()).replaceAll(" ", "").replaceAll("\n", ""));
                            }
                        }
                        else
                        {
                            response.put("403 Forbidden", "");
                        }
                    }
                    catch (JSONException | IOException ignored)
                    {
                        response.put("403 Forbidden", "");
                    }
                }
            }
            else
            {
                response.put("403 Forbidden", "");
            }
            return response;
        }, port);
    }

    public static void thread(Runnable runnable)
    {
        threadPool.execute(runnable);
    }
}