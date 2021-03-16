/*
* By LTC
* 2021.3.9*/
package spider.bili;
import com.github.kevinsawicki.http.HttpRequest;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class getPictures2 {
    public static WebDriver getWebDriver(int moudle , String driverPath){
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
    //将所有链接对应的图片下载到path中，并按照从number开始的顺序编号
    public static void downLoad(String path,ArrayList<String> links,int number) throws Exception{
        for (String s:links){
            HttpRequest hr=HttpRequest.get("https:"+s);
            if (hr.ok()){
                File file=new File(path+number+s.substring(s.length()-4));
                hr.receive(file);
                number++;
            }
        }
    }
    public static void main(String[] args) throws Exception{

        //用户uid
        String uid="387636363";
        //图片存储位置
        String dir="E:\\codes\\pictures\\"+"luluDiary1"+"\\";
        //driver位置
        String driverPath="C:\\Users\\ltc\\Desktop\\chromedriver.exe";
        //没有图片可以加载时会显示这个
        String bottomFlag="你已经到达了世界的尽头";
        //pt1用来匹配一个动态里的文本
        //Pattern pt1=Pattern.compile("(Pixiv)|(ID)|(id)|(\\u753b\\u5e08)");
        Pattern pt1=Pattern.compile("lulu日记");
        //pt2用来匹配一个动态里的图片链接
        Pattern pt2=Pattern.compile("//i0[^@]{50,100}(png|jpg)");
        //初始化
        WebDriver driver=getWebDriver(1,driverPath);
        JavascriptExecutor jse=(JavascriptExecutor)driver;
        ArrayList<WebElement> wes=null;
        //图片链接links
        ArrayList<String> links=new ArrayList<String>();
        driver.get("https://space.bilibili.com/"+uid+"/dynamic");
        Thread.sleep(3000);
        jse.executeScript("window.scrollBy(0,"+4000+");");
        long time1=System.currentTimeMillis();
        while (true){
            //向下滚动
            jse.executeScript("window.scrollBy(0,"+800+500*Math.random()+");");
            //如果发现到底了，就退出循环
            if (driver.findElement(By.className("div-load-more")).getAttribute("innerHTML").contains(bottomFlag))
                break;
            wes=(ArrayList<WebElement>) driver.findElements(By.className("original-card-content"));
            wes.remove(wes.size()-1);
            //每20个动态获取一次，并删除对应的网页元素（否则会很慢）
            if (wes.size()>20){
                for (WebElement we:wes){
                    String innerHtml=we.getAttribute("innerHTML");
                    Matcher matcher2=pt2.matcher(innerHtml);
                    if (pt1.matcher(innerHtml).find()){
                        while (matcher2.find()){
                            String link=matcher2.group();
                            if (link.contains("album"))
                                links.add(link);
                        }
                    }
                    jse.executeScript("document.getElementsByClassName(\"card\")[0].remove();");
                }
            }
            Thread.sleep(50);
        }
        Collections.reverse(links);
        long time2=System.currentTimeMillis();
        //下载
        downLoad(dir,links,0);
        System.out.println(time2-time1);
        driver.quit();
    }
}
