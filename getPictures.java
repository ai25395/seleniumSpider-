package spider.bili;
import com.google.common.primitives.Bytes;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import sun.net.www.http.HttpClient;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//偶然发现了b站一个up主在动态发福利图片，于是想把图片爬下来，用到了selenium和httpclient
/*存在的问题：1.慢。用selenium肯定慢，但是这种js获取的东西，自己去分析request简直爆炸。。。。。。。
             2.爬取的图片有的不是想要的，可能只是一些普通的图片（对于我爬取的这个有对应的解决办法，因为正确的图片的描述中总有一个固定的字符串
             不过具体实现没写（专业课太多，没时间了。。。））
             3.爬取的动态过多时，selenium会出现内存不够用的问题（8g内存运行），不过我测试时爬了300多张图片,问题不大
             4.检测是否到底的代码太笨了，希望有大佬给点建议
*/
public class getPictures {

    //getFileAsString用来从一个文本文件读取出一个字符串，不过这个小项目中没用到，我写着玩的
    public static String getFileAsString(String Path)
    {
        String result=null;
        try{
            FileReader fr=new FileReader(Path);
            BufferedReader br=new BufferedReader(fr);
            String tem=null;
            while((tem=br.readLine())!=null){
                result+=tem;
            }
            if(result==null)
                result="null";
            br.close();
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            return result;
        }
    }

    //putPicture用来将二进制流输出为指定类型的文件，
    public static int putBinaryFile(String filename,InputStream inputStream){
        int flag=1;
        try{
            File file=new File(filename);
            if(!file.exists()) file.createNewFile();
            byte[] bytes=new byte[4096];
            int length=-1;
            while ((length=inputStream.read(bytes))!=-1)
            {
                OutputStream os=new FileOutputStream(file,true);  //true指定为追加模式，而不是覆盖
                os.write(bytes,0,length);                             //这里如果不指定起始和长度，输出的图片会失真
                System.out.println(length);
            }
        }catch (Exception e){
            System.out.println("输出异常，可能是文件名有问题或者输入流有问题："+filename);
            flag=-1;
        }finally {
            return flag;
        }
    }
    public static void main(String[] args) throws Exception{
        System.setProperty("webdriver.chrome.driver","C:\\Program Files (x86)\\Google\\Chrome\\Application\\chromedriver.exe");
        WebDriver driver=new ChromeDriver();
        JavascriptExecutor js=(JavascriptExecutor)driver;
        driver.get("https://space.bilibili.com/424263116/dynamic");
        boolean isBottom=false;      //指示是否到底
        int checkTime=50;            //每滚动50次查看一下是否到底了
        int count=0;                 //滚动总次数，因为有些用户的动态太多了，自己估算一下吧
        Thread.sleep(2000);
        while(isBottom!=true&&count<1000){
            //执行滚动js，滚动长度随机
            js.executeScript("window.scrollBy(0,"+600*Math.random()+");");
            //检查是否到底，我觉得这么写（获取document，检查其中的字符串）挺笨的，不过对前端不太了解，希望大佬们帮忙改进
            if(checkTime==0){
                String doc = (String) js.executeScript("return document.documentElement.outerHTML");
                Document document= Jsoup.parse(doc);
                Elements ele=document.getElementsByClass("div-load-more tc-slate");
                System.out.println(ele.first().wholeText());
                if (ele.first().wholeText().equals("你已经到达了世界的尽头"))
                    isBottom=true;
                checkTime=50;
                Thread.sleep((long)Math.random()*15000);   //每次检查完暂停随机时间，有反爬，大概
            }
            checkTime--;
            count++;
        }


        String doc = (String) js.executeScript("return document.documentElement.outerHTML");
        ArrayList<String> al=new ArrayList<String>();
        Pattern pa=Pattern.compile("//.{0,100}?(gif|jpg)");  //匹配这样的：    //asdwedwefwefefe.gif 或者  //sdfweferwf.jpg
        Matcher mac=pa.matcher(doc);
        CloseableHttpClient chc= HttpClientBuilder.create().build();
        HttpGet hg=new HttpGet("");
        //将获取到的图片url保存，并去掉重复的
        while(mac.find()){
            if(!al.contains(mac.group(0)))
                al.add(mac.group(0));
        }

        int picNum=1;   //生成的图片文件的编号
        Iterator<String> iterator=al.iterator();
        //根据url获取输入流，再输出到本地，为了直观，没有单独声明一个磁盘地址变量
        while(iterator.hasNext()){
            String url=iterator.next();
            hg.setURI(new URI("http:"+url));
            CloseableHttpResponse chr=chc.execute(hg);
            HttpEntity he=chr.getEntity();
            InputStream is=he.getContent();
            putBinaryFile("D:\\codes\\pictures\\pictures1\\"+picNum+"."+url.substring(url.length()-3,url.length()),is);
            picNum++;
        }

    }
}
