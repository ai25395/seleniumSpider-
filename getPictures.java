package spider.bili;
import com.google.common.primitives.Bytes;
import com.sun.deploy.security.SelectableSecurityManager;
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
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import sun.net.www.http.HttpClient;

import java.io.*;
import java.net.URI;
import java.util.*;
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

    //请指定propertie文件，否则会使用默认，下面给出properties文件实例

    //#key为一个uid，value为   图片最大编号和爬取的第一张图片url（最晚发布的）
    //320162664=176,//i0.hdslb.com/bfs/album/d11d6b1739ff95b7c90240ad4cfaa4ee3472527b.png,
    //424263116=1102,//i0.hdslb.com/bfs/album/ddcd6b57772a9c69708e3c4a3dc54268537784c2.jpg,
    //479985318=365,//i0.hdslb.com/bfs/album/7169856a4a40d97532188918ed6ba2c20a107ecd.jpg,
    public static String getNewest(String uid)  {
        String newest="newest";
        try {
            Properties properties = new Properties();
            FileInputStream fis = new FileInputStream("/pic.properties");
            properties.load(fis);
            newest=properties.getProperty(uid,"100,no");
        }catch (Exception e){
            System.out.println("加载配置文件错误");
            newest="100,no";
        }
        finally {
            return newest;
        }
    }

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
            byte[] bytes=new byte[1024*64];
            int length=-1;
            while ((length=inputStream.read(bytes))!=-1)
            {
                OutputStream os=new FileOutputStream(file,true);  //true指定为追加模式，而不是覆盖
                os.write(bytes,0,length);                             //这里如果不指定起始和长度，输出的图片会失真\
                os.close();                                               //这里如果不关闭，会导致后面无法删除！！！！！！！
            }
        }catch (Exception e){
            System.out.println("输出异常，可能是文件名有问题或者输入流有问题："+filename);
            System.out.println("或者是，你已经爬过这个up了，但是没有设置properties，导致图片编号仍然从0开始，看getNewest方法");
            flag=-1;
        }finally {
            return flag;
        }
    }

    //给定一个包含url的List，下载全部，并保存到指定文件夹，用从1开始的自增数字命名文件
    public static void downloadBinaryByList(String path,ArrayList<String> al,String number) throws Exception{
        CloseableHttpClient chc= HttpClientBuilder.create().build();
        HttpGet hg=new HttpGet("");
        String downloadGap="500";
        String directory=path;                                           //图片保存的位置
        int picNum=Integer.valueOf(number)+1;                            //生成的图片文件的编号
        Iterator<String> iterator=al.iterator();
        //根据url获取输入流，再输出到本地,   putBinaryFile函数
        System.out.println("开始获取"+al.size()+"张图片");
        while(iterator.hasNext()){
            String url=iterator.next();
            hg.setURI(new URI("http:"+url));
            Thread.sleep(Long.valueOf(downloadGap)+(long)(Math.random()*750));          //爬慢些，要做一只有道德的爬虫
            try {
                CloseableHttpResponse chr = chc.execute(hg);
                HttpEntity he = chr.getEntity();
                InputStream is = he.getContent();
                putBinaryFile(directory+ picNum + "." + url.substring(url.length() - 3, url.length()), is);
                is.close();
            }catch (Exception e){
                e.printStackTrace();
                System.out.println("可能连接错误了");
            }
            picNum++;
            System.out.println("第"+picNum+"张");
        }

    }

    //过滤一个目录下的文件，小于len字节的文件删除
    public static void filterFilesByDirectory(String path,long size){
        File directory=new File(path);
        File[] names=directory.listFiles();
        long len=size;
        for(File s:names){
            if(s.isFile()) {
                fileFilterBySize(path + s.getName(), len);
            }
        }
    }
    //判断一个文件是否小于给定大小，如果是就删除
    public static void fileFilterBySize(String filename,long bytes){
        File file=new File(filename);
        long len=file.length();
        System.out.println(len);
        if(len<bytes){
            boolean isDeleted=file.delete();
            System.out.println("已删除:"+filename+":"+isDeleted);
        }
    }

    //获取webdriver
    public static WebDriver getWebDriver(int moudle){
        String driverPath="C:\\Program Files (x86)\\Google\\Chrome\\Application\\chromedriver.exe";
        System.setProperty("webdriver.chrome.driver",driverPath);
        HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
        chromePrefs.put("profile.managed_default_content_settings.images", 2);
        WebDriver driver;
        if(moudle==1)
            driver=new ChromeDriver(new ChromeOptions().setHeadless(true).setExperimentalOption("prefs",chromePrefs));
        else
            driver=new ChromeDriver();
        return driver;
    }

    //删除多个文件夹中相同大小的图片
    public static void deleteSameInDirectories(String[] paths){
        HashMap<String,Long> map=new HashMap<String, Long>();
        int i=0;
        while (i<paths.length) {
            File file = new File(paths[i]);
            if (file.exists() && file.isDirectory()) {
                File[] files = file.listFiles();
                Long[] sizes = new Long[files.length];
                int j = 0;
                while (j < files.length) {
                    sizes[j] = files[j].length();
                    j++;
                }
                j = 0;
                while (j < files.length) {
                    map.put(files[j].getAbsolutePath(), sizes[j]);
                    j++;
                }
            }
            i++;
        }
            List<Map.Entry<String,Long>> list = new ArrayList<Map.Entry<String,Long>>(map.entrySet());
            Collections.sort(list,new Comparator<Map.Entry<String,Long>>() {
                //升序排序
                public int compare(Map.Entry<String, Long> o1,
                                   Map.Entry<String, Long> o2) {
                    if (o1.getValue()>o2.getValue())
                        return 1;
                    else if(o1.getValue()<o2.getValue())
                        return -1;
                    else
                        return 0;
                }
            });
            int k=0;
            while (k<list.size()-1){
                if (list.get(k).getValue().equals(list.get(k+1).getValue())) {
                    new File(list.get(k).getKey()).delete();
                    System.out.println("文件"+list.get(k)+"删除成功");
                }
                k++;
            }

    }
    public static void main(String[] args) throws Exception{
        String Uid="552413098";                                     //uid
        String isVisiable="0";                              //浏览器是否可见，不可见时，也不会加载css和图片
        String pattern1="//i0[^@]{50,100}(png|jpg)";
        String pattern2="(Pixiv)|(ID)|(id)|(\\u753b\\u5e08)";
        String isBottomFlag="你已经到达了世界的尽头";         //动态到达底部标志
        String filterSize="502400";                       //过滤文件大小
        String downloadDirectory="D:\\codes\\pictures\\";                       //下载到哪
        String scrollGap="300";
        String number="110";                                  //生成图片的起始标号

        //配置chromedriver，并设置为headless，且不加载图片
        WebDriver driver=getWebDriver(Integer.valueOf(isVisiable));
        JavascriptExecutor js=(JavascriptExecutor)driver;
        String uid=Uid;
        String targetDynamicUrl="https://space.bilibili.com/"+uid+"/dynamic";
        driver.get(targetDynamicUrl);
        boolean isBottom=false;      //指示是否到底
        int checkTime=50;            //每滚动50次查看一下是否到底了,并暂停一会
        int count=0;                 //滚动总次数
        String isBottomS=isBottomFlag; //如果到底了，就会出现这个字符串
        Thread.sleep(5000);
        System.out.println("初始化完成，启动。");
        while(isBottom!=true&&count<Integer.valueOf(number)){       //如果不想滚动到底，可以增加判断条件 ：count<n，设置滚动n次
            //执行滚动js，滚动长度随机
            js.executeScript("window.scrollBy(0,"+400*Math.random()+");");
            //检查是否到底，我觉得这么写（获取document，检查其中的字符串）挺笨的，不过对前端不太了解，希望大佬们帮忙改进
            if(checkTime==0){
                String doc = (String) js.executeScript("return document.documentElement.outerHTML");
                Document document= Jsoup.parse(doc);
                Elements ele=document.getElementsByClass("div-load-more tc-slate");
                System.out.println(ele.first().wholeText());
                if (ele.first().wholeText().equals(isBottomS))
                    isBottom=true;
                checkTime=50;
                Thread.sleep(Long.valueOf(scrollGap));   //每次检查完暂停随机时间，遵守爬虫道德
            }
            checkTime--;
            count++;
            System.out.println(count);
        }

        //获取全部html，进行正则匹配，把符合结果的url放入一个List中
        String doc = (String) js.executeScript("return document.documentElement.outerHTML");
        Document document=Jsoup.parse(doc);
        String urls=null;
        ArrayList<String> al=new ArrayList<String>();
        Pattern pa_1=Pattern.compile(pattern1);  //匹配这样的：    //asdwedwefwefefe.gif 或者  //sdfweferwf.jpg
        Pattern pa_2=Pattern.compile(pattern2);   //用于筛选节点，动态中要含有Pixiv或ID或“图片”
        Matcher mac_1=pa_1.matcher("111");
        Matcher mac_2=pa_2.matcher("222");
        Element bigBrother=document.getElementsByClass("feed-card").first().child(0);
        String urlNewest=getNewest(uid).split(",")[1];
        String url="00";


        //上次爬的最新的图片的url被保存到一个properties里，这次爬的时候，如果爬到了那张，就停止，防止重复
        for(Element e:bigBrother.children()){
            String nodeText=e.outerHtml();
            if(url.equals(urlNewest))
                break;
            mac_2.reset(nodeText);
            if(mac_2.find()){
                mac_1.reset(nodeText);
                while (mac_1.find()){
                    url=mac_1.group();
                    if(url.equals(urlNewest))
                        break;
                    if(!al.contains(url)){
                        al.add(url);
                        System.out.println(url);
                    }
                }
                }
            }
        downloadBinaryByList(downloadDirectory+uid+"\\",al,(getNewest(uid).split(","))[0]);
        filterFilesByDirectory(downloadDirectory+uid+"\\",Long.valueOf(filterSize));

    }
}
