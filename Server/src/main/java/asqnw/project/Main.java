package asqnw.project;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
                            response.put("200 OK", Base64.getEncoder().encodeToString(new JSONObject().put("code", "0").put("msg", "test").toString().getBytes()).replaceAll(" ", "").replaceAll("\n", ""));
                        }
                    }
                    catch (JSONException ignored)
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