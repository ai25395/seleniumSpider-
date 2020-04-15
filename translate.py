from selenium import webdriver as wd
import time
import random

'''一个翻译的小demo，有些细节写死了，用的时候再自己改吧。。。'''
'''给定一个单词表，一个单词一行，然后输出一个单词表，包含单词，发音，解释'''
'''源文件不要包含空行，否则会在第80行处死循环'''
'''2020.04.15 
    by:LTC'''


'''用金山搜索，参数为webdriver和一个word，返回两个集合，一个发音，一个解释'''
def getTranslateByJinshan(webdriver,word):
    result_pron=[]
    result_mean=[]
    textarea=webdriver.find_element_by_id("textarea")
    textarea.clear()
    time.sleep(random.randint(5,10)/10.0)
    textarea.send_keys(word)
    time.sleep(random.randint(6,12)/5.0)
    for p in webdriver.find_element_by_class_name("res-word").find_element_by_class_name("word-voice").find_elements_by_tag_name("span"):
        result_pron.append(p.text)
    for p in webdriver.find_element_by_class_name("res-word").find_elements_by_class_name("prop"):
        result_mean.append(p.text)
    return result_pron,result_mean

'''搜狗，同上，'''
'''记录一个遇到的小问题：by_class_name找不到目标element,查找整个html发现前面还有一个element有相同的类名。。。'''
def getTranslateBySougou(webdriver,word):
    result_pron=[]
    result_mean=[]
    textarea=webdriver.find_element_by_id("sogou-translate-input")
    textarea.clear()
    time.sleep(random.randint(5,10)/10.0)
    textarea.send_keys(word)
    '''自行指定速度，并且是速度随机，若速度太快或过于规律可能被反爬'''
    time.sleep(random.randint(6,12)/5.0)

    for p in webdriver.find_element_by_class_name("mod-pronounce").find_elements_by_class_name("phonetic"):
        result_pron.append(p.text)
    for p in webdriver.find_element_by_id("tree_1_0").find_element_by_class_name("inner-wrap").find_elements_by_class_name("item"):
        str1=p.find_element_by_class_name("word-class").text
        str2=p.find_element_by_class_name("translation").text
        result_mean.append(str1+str2)
    return result_pron,result_mean





'''用金山翻译做例子，请自行指定源和目标文件
此处使用的是chromedriver，对应版本为80.0.3987.162'''
webdriver=wd.Chrome()
webdriver.get("http://fy.iciba.com/")
list_1=[]
map_pron={}
map_mean={}
source_file="words1.txt"
target_file="words5.txt"
with open(source_file,"r",encoding='utf-8') as words:
    list_1=words.readlines()
print("获取初始单词表成功")

i=0
relax=0
flag=0
while(i<len(list_1)):
    try:
         map_pron[list_1[i]],map_mean[list_1[i]]=getTranslateByJinshan(webdriver,list_1[i])
    except:
        '''如果单词错误，将出现异常，用下面的来暂时填充此单词'''
        map_pron[list_1[i]]=['111111111111',random.randint(0,10).__str__()]
        map_mean[list_1[i]]=['222222222222',random.randint(0,10).__str__()]
        pass

    '''如果由于网速慢等原因造成读取的不是当前单词的翻译，回退'''
    if(i!=0 and map_mean[list_1[i]]==map_mean[list_1[i-1]] ):
        i-=1
    i+=1
    '''防止由于访问过于频繁引起的拒绝访问'''
    relax+=1
    if(relax>30):
        time.sleep(10)
        relax=0
print("获取全部单词翻译成功")

'''输出单词表'''
with open(target_file,"a",encoding='utf-8') as txt:
    j=0
    while(j<len(list_1)):
        txt.write(list_1[j].rstrip("\n")+" :     "+map_pron[list_1[j]].__str__().rstrip("]").lstrip("[")+"\n              "+
                  map_mean[list_1[j]].__str__()+"\n\n")
        j+=1
print("输出成功")








