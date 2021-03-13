/* By LTC 2021.3.11
* 用于抓取斗鱼任一直播间的弹幕并输出到文本*/
package spider.bili;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class douYuDanMu {
    //把字符串输出到txt文本，参数分别为：字符串本身，txt路径，模式（1为追加模式）
    public static void stringToTxt(String s,String path,int module) throws Exception{
        File file=new File(path);
        if (file.exists() && module==1){                 //文件存在且指定为追加
            FileWriter fw=new FileWriter(path,true);
            fw.write(s);
            fw.flush();
        }
        else{
            FileWriter fw=new FileWriter(path);
            fw.write(s);
            fw.flush();
        }
    }
    public static void main(String[] args) throws Exception {
        WebDriver driver=getPictures1.getWebDriver(0,"C:\\Users\\ltc\\Desktop\\chromedriver.exe");
        JavascriptExecutor jse=(JavascriptExecutor)driver;
        //斗鱼房间号
        int id=63136;
        //当有count条弹幕时，读取并清空
        int count=20;
        //txt文件位置
        String path="****/chromedriver.exe";
        driver.get("https://www.douyu.com/"+id);
        //存储用户名
        ArrayList<String> names=new ArrayList<>();
        //存储弹幕内容
        ArrayList<String> danMus=new ArrayList<>();
        //存储弹幕对应的item
        ArrayList<WebElement> wes=null;
        Thread.sleep(5000);
        try {
            while (true) {
                //不断检查弹幕条数，
                wes = (ArrayList<WebElement>) driver.findElement(By.id("js-barrage-list")).findElements(By.className("Barrage-listItem"));
                if (wes.size() > count) {
                    for (WebElement we : wes) {
                        try {
                            danMus.add(we.findElement(By.className("Barrage-content")).getText());
                            names.add(we.findElement(By.className("Barrage-nickName")).getAttribute("title"));
                        } catch (Exception e) {
                            //抓取到礼物信息会报错，无需处理
                        }
                    }
                    jse.executeScript("document.getElementById(\"js-barrage-list\").innerHTML=\"\";");
                    new WebDriverWait(driver,30).until(ExpectedConditions.presenceOfElementLocated(By.className("Barrage-listItem")));
                }
                Thread.sleep(1000);
            }
        }catch (Exception e){}
        finally {
            //在浏览器console执行document.getElementById("js-barrage-list").outerHTML="" 使程序发生异常，进而保存结果并退出
            int i=0;
            System.out.println(names.size()+"  "+danMus.size());
            for(i=0;i<names.size();i++){
                stringToTxt(names.get(i)+" : ",path,1);
                stringToTxt(danMus.get(i)+"\n",path,1);
            }
            System.out.println("结束");
            driver.quit();
        }
    }
}
