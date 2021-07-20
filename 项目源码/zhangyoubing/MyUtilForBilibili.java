package mavenTest2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.jsoup.Connection.Response;

public class MyUtilForBilibili {
	/**
     * 给定txt文件的路径，把txt文件的内容逐行读取出来，存入String列表中，并返回。
     * @param txtPath txt文件的路径
     * @return
     * @throws IOException
     */
    public static ArrayList<String> getStrArrInTxt(String txtPath) throws IOException{
    	FileInputStream fis=new FileInputStream(txtPath);
    	BufferedReader br=new BufferedReader(new InputStreamReader(fis));
    	ArrayList<String> strarr=new ArrayList<String>();
    	while(true) {
    		String temp=br.readLine();
    		if(temp==null)
    		break;
    		else strarr.add(temp);
    	}
    	br.close();
    	return strarr;
    }
	/**
	 * 给定bvid,cids数组，和视频分p数，将返回当前的观看人数，数组，数组最长长度为5,每两次爬取之间进程进行sleep,防止因爬取过快被ban
	 * @param bvid
	 * @param cids
	 * @param p
	 * @return 如果p<5的话，返回的数组后面几个元素的值是-1。
	 * @throws InterruptedException 
	 */
	public static int[] getViewingNums(String bvid,ArrayList<String>cids,int p) throws InterruptedException{
		int[] array= {-1,-1,-1,-1,-1};
		if(p>5)p=5;
		if(p<1)p=1;
		for(int i=0;i<p;++i) {
			array[i]=getViewingNum(bvid, cids.get(i));
			Thread.sleep(5000);//休息5秒
		}
		return array;
	}
	/**
	 * 给定bvid和cid，返回当前观看人数
	 * @param bvid
	 * @param cid
	 * @return
	 */
	private  static int getViewingNum(String bvid,String cid) {
		//
	String UserAgent="BiLiBiLi WP Client/1.0.0 (2579025605@qq.com)";
		
		Map<String, String> headersmap=new HashMap<String, String>();
		headersmap.put("user-agent", UserAgent);
		String jsonString=getDocByUrlAndHeaders("https://api.bilibili.com/x/player/online/total?cid="+cid+"&bvid="+bvid, headersmap).text();

		JSONObject jsonobj=JSONObject.parseObject(jsonString);
		return jsonobj.getJSONObject("data").getIntValue("count");
	}
	/**
	 * 调用bilibili后台开放接口，由bvid获取cid数组。数组长度即视频分p数
	 */
	public static ArrayList<String> getCidsbyBvid(String bvid) {
		String UserAgent="BiLiBiLi WP Client/1.0.0 (2579025605@qq.com)";
		
		Map<String, String> headersmap=new HashMap<String, String>();
		headersmap.put("user-agent", UserAgent);
		String jsonString=getDocByUrlAndHeaders("https://api.bilibili.com/x/player/pagelist?bvid="+bvid, headersmap).text();

		JSONObject jsonobj=JSONObject.parseObject(jsonString);
		ArrayList<String> str_arr=new ArrayList<String>();
		JSONArray jsondataArray=jsonobj.getJSONArray("data");
		for(int i=0;i<jsondataArray.size();++i) {
			str_arr.add(jsondataArray.getJSONObject(i).getString("cid"));//获取cid
		}
		
	
		return str_arr;
	}
	
	
	/**
	 * 给定bvid,返回该视频的播放量和弹幕数
	 * 返回的数据类型仍是String[]
	 * 第一个是播放量，第二个是弹幕数,第三个是点赞数，第四个是投币数，第五个是收藏数,第6个是分p数，如果大于5就置为5
	 * 其中前三个的确切数值可以在网页中直接爬取，第四个第五个爬取的是生数据，类似"8765"、"1.2万"
	 * @param bvid
	 * @return
	 */
	public static String[] getVideoData(String bvid) {
		String[] strarr=new String[6];
		Document doc=getDocByUrl("https://www.bilibili.com/"+bvid);
		Element e0=doc.selectFirst("[class=view]");
		strarr[0]=e0.attr("title").replace("总播放数", "");
		Element e1=doc.selectFirst("[class=dm]");
		strarr[1]=e1.attr("title").replace("历史累计弹幕数", "");
		Element e2=doc.selectFirst("[class=like]");
		strarr[2]=e2.attr("title").replace("点赞数", "");
		Element e3=doc.selectFirst("[class=coin]");
		strarr[3]=e3.text();
		Element e4=doc.selectFirst("[class=collect]");
		strarr[4]=e4.text();
		int p_num=getVideoPageNum(doc);
		strarr[5]=String.valueOf(p_num>5?5:p_num);
		return strarr;
	}
	/**
	 * 给定bvurl,返回视频的bvid
	 * @param bvurl
	 * @return
	 */
	public static String getBVIDbyBVURL(String bvurl) {
		int beginIndex=bvurl.indexOf("BV");
		int endIndex=bvurl.indexOf("?");
		return bvurl.substring(beginIndex,endIndex);
	}
	//这个注释掉的方法并不能成功运行，原因是这个span标签是ajax请求生成的,不是原页面就有的。
//	public static ArrayList<String> getAllPinfoOfVideo(Document doc) {
//		ArrayList<String> strarr=new ArrayList<String>();
//		Elements eles = doc.select("[class=part] span");
//		for(Element e:eles) {
//			strarr.add(e.text());
//		}
//		System.out.println(eles.size());
//		return strarr;
//	}
	public static int getVideoPageNum(Document doc) {
		Element ele=doc.selectFirst("[class=cur-page]");
		if(ele==null) {
			return 1;
		}
		else {//对(1/844)这样的字符串进行处理，获取844
			String text=ele.text();
			int startIndex=text.indexOf('/')+1;
			int endIndex=text.lastIndexOf(')');
//			System.out.println(text.substring(startIndex,endIndex));
			return Integer.valueOf(text.substring(startIndex,endIndex));
		}
		
		
	}
	/**
	 * 给定b站查询结果，返回查询结果的页数。
	 * @param doc 查询结果
	 * @return
	 */
	public static int getPage(Document doc) {
		Element ele = doc.selectFirst("[class=pagination-btn]");//最后一页对应的class
		if(ele!=null) {//有最后一页对应的class
			return Integer.valueOf(ele.text());//那么页数就是button内容
		}
		else {//没有最后一页对应的class
			Elements eles = doc.select("[class=pagination-btn num-btn]");
			if(eles.size()!=0) {//依然有页数，最后一个button内容就是页数
				return Integer.valueOf(eles.last().text());
			}else {//下面没有显示页数，现在有两个可能
				//1 没有搜索结果。
				if(doc.selectFirst("[class=total-text]").text().contains("0")) {
					return 0;
				}//2 有搜索结果，页数是1
				else {
					return 1;
				}
				
			}
			
		}
	}
	/**
	 * 给定搜索词，返回b站搜索结果
	 * @param keyWord
	 * @return Document类型的搜索结果
	 */
	public static Document getDocByKeyWord(String keyWord) {
		return MyUtilForBilibili.getDocByUrlAndHeaders("https://search.bilibili.com/video?keyword="+keyWord,null);
		
	}
	/**
	 * 给定搜索词和页数,返回b站搜索结果
	 * @param keyWord
	 * @param pageNum
	 * @return Document类型的搜索结果
	 */
	public static Document getDocByKeyWordAndPageNum(String keyWord,int pageNum) {
		return MyUtilForBilibili.getDocByUrlAndHeaders("https://search.bilibili.com/video?keyword="+keyWord+"&page="+pageNum,null);
		
	}
	
	/**
	 * 给定b站搜索结果，返回当前页面下所有的视频信息。
	 * @param doc
	 * @return
	 */
	public static ArrayList<VideoInfo> getVideoInfos(Document doc){
		ArrayList<VideoInfo> Varr=new ArrayList<VideoInfo>();
		//
		Elements elements=doc.select("[class=video-item matrix]");
		
		for(Element e:elements) {
			String title=e.selectFirst("a").attr("title");//视频标题
			String watch_num=e.selectFirst("[class=so-icon watch-num]").text();//播放量
			String danmu_num=e.selectFirst("[class=so-icon hide]").text();//弹幕数
			String time=e.selectFirst("[class=so-icon time]").text();//上传时间
			String up_name=e.selectFirst("[class=so-icon]").text();//up主名字
			String duration=e.selectFirst("[class=so-imgTag_rb]").text();//视频时长
			String bv_url=("https:"+e.selectFirst("a").attr("href"));//bv类型的url
			VideoInfo vi=new VideoInfo(title, watch_num, danmu_num, time, up_name, duration, bv_url);
			Varr.add(vi);
			
		}
		return Varr;
	}
	/**
	 * 
	 * 从b站上下载视频，
	 * @param ffmpegPath ffmpeg的路径
	 * @param url 给定bv类型的url(url中不要指定p数)
	 * @param dir 下载目录
	 * @param filename 文件名
	 * @param isWindows 是不是Windows操作系统
	 * @param PNum 哪一集视频
	 * @throws IOException
	 */
	public static void downLoadAVandCombineWithPNum(String ffmpegPath,String url,String dir,String filename,boolean isWindows,int PNum) throws Exception {
		if(url.contains("?")) {
			url+=("&p="+PNum);//更改url，加入集数这个参数。
		}else url+=("?p="+PNum);
		
		downLoadAVandCombine(ffmpegPath, url, dir, filename, isWindows);
	}
	/**
	 * 从b站上下载音频、视频，并混合。
	 * @param url bv类型的url
	 * @param dir 下载目录
	 * @param filename	文件名
	 * @throws IOException 
	 */
	public static void downLoadAVandCombine(String ffmpegPath,String url,String dir,String filename,boolean isWindows) throws Exception {
		downLoadBiliBiliVAByBVurl(url, dir, filename);
//		String ffmpegpath="D:\\ffmpeg\\bin\\ffmpeg";
		combineWithFfmpegPath(ffmpegPath,dir+filename+"_A.mp3", dir+filename+"_V.mp4", dir+filename+".mp4",isWindows);
	}
	/**
	 * 把b站下载来的音频文件和视频文件混合。请在第一参数中指定ffmpeg路径
	 * 
	 * @param ffmpegpath
	 * @param path1 文件1
	 * @param path2	文件2
	 * @param path3	生成的mp4文件
	 */
	public static void combineWithFfmpegPath(String ffmpegpath,String path1,String path2,String path3,boolean isWindows)  {
		//.\ffmpeg.exe -i video.m4s -i audio.m4s -codec copy Output.mp4
		checkPath(path1);//保证有目录
		if(!ffmpegpath.endsWith("ffmpeg")) {
			System.out.println("输入的ffmpeg路径有误，停止操作");
			return;
		}
//		String deleteCMD=isWindows?"del":"rm";//操作系统不同，删除指令也不同。
		try {
			Process p=Runtime.getRuntime().exec(ffmpegpath+" -i "+path1+" -i "+path2+" -y -codec copy "+path3);
			
//			p.waitFor();//命令执行完毕前阻塞进程。
//			System.out.println("混合命令执行完毕");
//			//把不需要的文件删掉(只保留音视频混合后的文件)
//			System.out.println(deleteCMD+" "+path1);
//			Thread.sleep(1000);
//			p=Runtime.getRuntime().exec(deleteCMD+" "+path1);
//			p.waitFor();
//			p=Runtime.getRuntime().exec(deleteCMD+" "+path2);
//			p.waitFor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}
//	/**
//	 * 把音频和视频文件混合。
//	 * 调用这个函数的时候，请确保配置了ffmpeg环境变量。
//	 * @param path1 文件1
//	 * @param path2	文件2
//	 * @param path3 生成的音视频混合文件
//	 */
//	public static void combine(String path1,String path2,String path3)  {
//		//.\ffmpeg.exe -i video.m4s -i audio.m4s -codec copy Output.mp4
//		checkPath(path1);//保证有目录
//		try {
//			Runtime.getRuntime().exec("ffmpeg -i "+path1+" -i "+path2+" -y -codec copy "+path3);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}
	/**
	 * 通过给定bilibili的bv的网址,这个方法将下载两个文件，一个是mp3,一个是mp4
	 * 保存在dir目录下，文件名是filename
	 * @param bvurl见上
	 * @param dir见上
	 * @param filename见上
	 * @throws IOException 
	 */
	public static void downLoadBiliBiliVAByBVurl(String bvurl,String dir,String filename) throws Exception {
		
		Document doc=MyUtilForBilibili.getDocByUrl(bvurl);
//		System.out.println(doc.select("script").get(4).html());
		String Json=doc.select("script").get(4).html().replace("window.__playinfo__=","");//直接获取json格式的字符串
		JSONObject jobj=JSON.parseObject(Json);
		jobj=jobj.getJSONObject("data");
		jobj=jobj.getJSONObject("dash");//找到dash属性对应的jsonObject
		//视频地址
		String videoUrl=jobj.getJSONArray("video").getJSONObject(0).getString("baseUrl");
		//音频地址
		String audioUrl=jobj.getJSONArray("audio").getJSONObject(0).getString("baseUrl");
		

		MyUtilForBilibili.downloadBilibiliAV(videoUrl, dir+filename+"_V.mp4");
		MyUtilForBilibili.downloadBilibiliAV(audioUrl, dir+filename+"_A.mp3");
	}
	/**
	 * 给定url，返回请求得到的document 采用get方法。
	 * @param url
	 * @return
	 */
	public static Document getDocByUrl(String url) {
		try {
			Connection con=Jsoup.connect(url);
//		con.headers(headersmap);
			return con.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * 给定url和请求头，返回请求得到的document 采用get方法。
	 * 不设置内容类型
	 * @param url
	 * @param headersmap
	 * @return
	 */
public static Document getDocByUrlAndHeaders(String url,Map<String, String> headersmap) {
		
		try {
			Connection con=Jsoup.connect(url);
			if(headersmap!=null)
			con.headers(headersmap);
			con.ignoreContentType(true);
			return con.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * 给定url和请求头还有请求方法，返回Document请求得到的Document
	 * method字符串只有为"post"(不区分大小写)的时候才会用post方法。
	 * @param url
	 * @param headersmap
	 * @param method
	 * @return
	 */
	public static Document getDocByUrlAndHeaders(String url,Map<String, String> headersmap,String method) {
		
		try {
			Connection con=Jsoup.connect(url);
		con.headers(headersmap);
		if(method.equalsIgnoreCase("post")) {
			return con.post();
		}
		else 	return con.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * 给定b站音/视频下载地址，和保存路径，程序将下载音/视频。
	 * @param savePath
	 * @throws IOException
	 */
	public static void downloadBilibiliAV(String url,String savePath) throws Exception {
		checkPath(savePath);
//		String url="https://upos-sz-mirrorkodo.bilivideo.com/upgcxcode/60/80/115048060/115048060-1-32.flv?e=ig8euxZM2rNcNbhahbUVhoMz7zNBhwdEto8g5X10ugNcXBlqNxHxNEVE5XREto8KqJZHUa6m5J0SqE85tZvEuENvNo8g2ENvNo8i8o859r1qXg8xNEVE5XREto8GuFGv2U7SuxI72X6fTr859r1qXg8gNEVE5XREto8z5JZC2X2gkX5L5F1eTX1jkXlsTXHeux_f2o859IB_&uipk=5&nbs=1&deadline=1626423114&gen=playurlv2&os=kodobv&oi=2559054688&trid=3a11e176a3d8492eaf7c997fdc930788u&platform=pc&upsig=adf325a92033bab506bda7abee951790&uparams=e,uipk,nbs,deadline,gen,os,oi,trid,platform&mid=0&bvc=vod&nettype=0&orderid=0,3&agrr=1&logo=80000000";
		Connection con=Jsoup.connect(url);
		con.header("Referer","https://www.bilibili.com/");
		con.ignoreContentType(true);
		con.maxBodySize(500000000);
		Response res = con.execute();
		BufferedInputStream bis=res.bodyStream();
		
		File file=new File(savePath);
		BufferedOutputStream bos=new BufferedOutputStream(new FileOutputStream(file));
		byte[] b=new byte[1024];
		int len;
		while((len=bis.read(b, 0, 1024))!=-1) {
			bos.write(b,0,len);
		}
		System.out.println("文件已保存到"+savePath);
		//		downVideo(url, "test123");
	}
	/**
	 * 检查str文件对应的目录路径是否存在，如果不存在，创建它的目录。此方法不会创建文件。
	 * @param str
	 */
	public static void checkPath(String str) {
		File file=new File(str).getParentFile();
//		System.out.println(file);
		if(!file.exists()) {
			file.mkdirs();
			
		}
	}
	/**
	 * 把String保存到指定的位置中
	 * @param str 要保存的str
	 * @param savePath 保存路径
	 */
	public static void saveString(String str,String savePath) {
		
		checkPath(savePath);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(savePath);
			fos.write(str.getBytes());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	
	}
	/**
	 * 把给定url地址的图片下载到指定地址中
	 * 
	 * @param urlstr   图片网址
	 * @param savePath 保存路径
	 */
	public static void savePic(String urlstr, String savePath) {
		checkPath(savePath);
		try {
			URL url = new URL(urlstr);

//			URLConnection urlcon=url.openConnection();
			BufferedInputStream bis = new BufferedInputStream(url.openStream());
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(savePath));
			byte[] Bytes = new byte[1024];// 1KB缓冲区
			int len;
			while ((len = bis.read(Bytes, 0, 1024)) != -1) {
				bos.write(Bytes, 0, len);//把刚刚读入的一点数据写入文件。
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 函数功能：把txt文件转成map txt文件格式 attr1:value1 attr2:value2(无需双引号) ...
	 * 
	 * @param path
	 * @return
	 */
	public static Map<String, String> txtToMap(String path) {
		ArrayList<String> sarr = getStrings(path);
		Map<String, String> map = new HashMap<String, String>();

		for (String s : sarr) {
			addToMap(map, s);
		}
		return map;
	}

	/**
	 * 把attr:value这样的String转成键值对，加入到map中
	 * 
	 * @param map
	 * @param str
	 */
	public static void addToMap(Map<String, String> map, String str) {
		String[] a = str.split(":", 2);
		if (a.length >= 2) {
			System.out.println(a[0]);
			System.out.println(a[1]);
			map.put(a[0], a[1]);
		}

	}

	/**
	 * 给定文件路径，把每行数据放入arraylist中，一起返回。
	 * 
	 * @param path
	 * @return
	 */
	public static ArrayList<String> getStrings(String path) {
		BufferedReader reader;
		ArrayList<String> arr = new ArrayList<String>();
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();
			while (line != null) {
				arr.add(line);
				// read next line
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			return arr;
		}
	}
}
//'http://152.136.107.79/test.jsp?search_key_word='+document.getElementById('1').value
