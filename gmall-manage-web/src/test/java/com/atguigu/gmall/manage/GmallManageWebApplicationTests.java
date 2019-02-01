package com.atguigu.gmall.manage;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallManageWebApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void textFileUpload() throws IOException, MyException {
		//读取图片服务器地址
		String file = this.getClass().getResource("/tracker.conf").getFile();
		ClientGlobal.init(file);
		//客户端
		TrackerClient trackerClient=new TrackerClient();
		//服务器
		TrackerServer trackerServer=trackerClient.getConnection();
		StorageClient storageClient=new StorageClient(trackerServer,null);
		//上传的图片
		String orginalFilename="e://001.jpg";

		//upload /root/001.jpg
		String[] upload_file = storageClient.upload_file(orginalFilename, "jpg", null);
		for (int i = 0; i<upload_file.length; i++) {
			String s = upload_file[i];
			System.out.println("s = " + s);

			//s = group1
			//s = M00/00/00/wKjlgFv9Ij-AVOG9AAAl_GXv6Z4910.jpg
		}

	}

}
