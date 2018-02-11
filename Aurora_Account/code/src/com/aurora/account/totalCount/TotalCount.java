package com.aurora.account.totalCount;

import android.content.Context;
import android.text.TextUtils;

import com.aurora.account.bean.DownloadDataResult;
import com.aurora.account.bean.accessoryInfo;
import com.aurora.account.bean.accessoryObj;
import com.aurora.account.bean.syncDataItemObject;
import com.aurora.account.bean.syncDataObject;
import com.aurora.account.contentprovider.AccountsAdapter;
import com.aurora.account.db.ExtraFileDownloadDao;
import com.aurora.account.service.ExtraFileUpService;
import com.aurora.account.util.AccountPreferencesUtil;
import com.aurora.account.util.FileLog;
import com.aurora.account.util.Log;
import com.aurora.account.util.SystemUtils;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectTimeoutException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

public class TotalCount implements Runnable {
	
	public static final String TAG = "TotalCount";
	

	private Context m_context;
	private String m_module = "";
	private String m_action = "";
	private int value = 0;

	
	/**
	 * 构建文件下载器
	 * 
	 * @param module_id 模块id
	 * @param action_id 动作id
	 * @param value 值
	 */
	public TotalCount(Context context,String module_id,String action_id,int value) {
		this.m_context = context;
		this.m_module = module_id;
		this.m_action = action_id;
		this.value = value;
	}
	
	@Override
	public void run() {
		startCount();
	}

	/**
	 * 开始计数
	 * 
	 */
	private void startCount() {
		DataCount.updataData(m_context, m_module, m_action, value);
	}
	

	
	//============对外控制方法开始=============//

	/**
	 * 统计队列
	 * 
	 */
	public void CountData() {
		ThreadPoolExecutor threadPool = CountManage.getThreadPoolExecutor();
		threadPool.execute(this);
	}
	

}
