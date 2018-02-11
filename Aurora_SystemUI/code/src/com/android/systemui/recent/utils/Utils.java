package com.android.systemui.recent.utils;

import java.util.HashMap;
import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.util.Log;

public class Utils{
	private static final String TAG = "PUBLIC_ICON";
	private static final String PUBLIC_ICON_RES = "com.aurora.publicicon.res";
	private static final String PACKAGE_CLASS = "lable_map";
	public static int ICON_REAL_SIZE = 178;
	private static int APP_ICON_RADIUS = 12;
	private static final boolean USE_RIGHT_ANGLE = true;
	private static final boolean USE_ICON_SHADOW = true;
	private static boolean isInitedConfig = false;
	/* Shadow type */
	public static final int OUTER_SHADOW = 0;
	public static final int INTER_SHADOW = 1;
	public static final int NONE_SHADOW_WITH_BOTTOM = 2;
	public static final int NONE_SHADOW = 3;
	
	private static Object configLock = new Object();

	/*
	 * 用于后期应用图标方案
	 */
	public static Bitmap drawable2bitmap(Drawable dw) {
		// 创建新的位图
		Bitmap bg = Bitmap.createBitmap(dw.getIntrinsicWidth(),
				dw.getIntrinsicHeight(), Config.ARGB_8888);
		// 创建位图画板
		Canvas canvas = new Canvas(bg);
		// 绘制图形
		dw.setBounds(0, 0, dw.getIntrinsicWidth(), dw.getIntrinsicHeight());
		dw.draw(canvas);
		// 释放资源
		canvas.setBitmap(null);
		return bg;
	}
	
	public static Bitmap getRoundedBitmap(Bitmap bm ,Context context){
		synchronized (configLock) {
			configClassArray(context, PACKAGE_CLASS);
		}
		Resources res = context.getResources();
		Log.i("zoom1", "getRoundedBitmap ICON_REAL_SIZE = "+ICON_REAL_SIZE+"  APP_ICON_RADIUS="+APP_ICON_RADIUS);
		return getRoundedBitmap(bm);
	}

	public static Bitmap getRoundedBitmap(Bitmap bm) {
		return getRoundedBitmap(bm,-1,false);
	}
	
	public static Bitmap  getRoundedBitmap(Bitmap bm,int customerSize){
		return getRoundedBitmap(bm,customerSize,false);
	}
	
	public static Bitmap getRoundedBitmap(Bitmap bm,int customerSize,boolean needScale) {
		// 创建新的位图
		int width = customerSize;
		//if(width < 0 || customerSize>bm.getWidth()){
			width = ICON_REAL_SIZE;
		//}
		Bitmap bg = Bitmap.createBitmap(width, width,
				Config.ARGB_8888);
		// 创建位图画板
		Canvas canvas = new Canvas(bg);
		Paint paint = new Paint();
		int extraW = (bm.getWidth() - width) / 2;
		if(2*extraW<bm.getWidth()-width){
			extraW++;
		}	
		//here need to be redesign
		int extraH = (bm.getHeight() - width) / 2;
		if(2*extraH<bm.getHeight()-width){
			extraH++;
		}
		extraW = extraW>50?10:extraW;
		extraH = extraH>50?10:extraH;
		Rect rect = new Rect(extraW, extraH, bm.getWidth() - extraW,
				bm.getHeight() - extraH);
		Rect rect2 = new Rect(0, 0, bg.getWidth(), bg.getHeight());
		RectF rectF = new RectF(extraW, extraH, bm.getWidth() - extraW,
				bm.getHeight() - extraH);
		// 设置圆角半径
		float roundR = APP_ICON_RADIUS;
		paint.setAntiAlias(true);
		canvas.save();
		canvas.translate(-(float) extraW, -(float) extraH);
		// 绘制圆形矩形
		canvas.drawRoundRect(rectF, roundR, roundR, paint);
		// 设置图形叠加模式
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		// 绘制图形
		canvas.restore();
		canvas.save();
		canvas.drawBitmap(bm, rect, rect2, paint);
		canvas.restore();
		// 释放资源
		canvas.setBitmap(null);
		return bg;
	}
	//1.draw shawdow 2.draw bitmap
	public static Bitmap getShadowBitmap1(Bitmap bm, Drawable shadow) {
		// 创建新位图
		Bitmap shadowBitmap = drawable2bitmap(shadow);
		Bitmap bg = Bitmap.createBitmap(shadowBitmap.getWidth(),
				shadowBitmap.getHeight(), Config.ARGB_8888);
		// 创建画板
		Canvas canvas = new Canvas(bg);
		Paint paint = new Paint();
		
		// 绘制图形
		canvas.save();
		canvas.translate((shadowBitmap.getWidth()-bm.getWidth())/2, (shadowBitmap.getHeight()-bm.getHeight())/2);
		canvas.drawBitmap(bm, 0.0f, 0.0f, paint);
		canvas.restore();
		// 绘制背景
		shadow.setBounds(0, 0, bg.getWidth(), bg.getHeight());
		shadow.draw(canvas);
		canvas.setBitmap(null);
		return bg;
	}
	
	//1.draw bitmap 2.draw shawdow
	public static Bitmap getShadowBitmap3(Bitmap bm, Drawable shadow) {
		int bw=shadow.getIntrinsicWidth();
		int bh=shadow.getIntrinsicHeight();
		float extraW = (bw -bm.getWidth())/2;
		float extraH = (bh-bm.getHeight())/2;
				
		// 创建新位图
		Bitmap bg = Bitmap.createBitmap(shadow.getIntrinsicWidth(),
				shadow.getIntrinsicHeight(), Config.ARGB_8888);
		// 创建画板
		Canvas canvas = new Canvas(bg);
		Paint paint = new Paint();
		// 绘制图形
		canvas.save();
		canvas.translate(extraW, extraH);
		canvas.drawBitmap(bm, 0.0f, 0.0f, paint);
		canvas.restore();
		// 绘制背景
		shadow.setBounds(0, 0, bg.getWidth(), bg.getHeight());
		shadow.draw(canvas);
		// 释放资源
		canvas.setBitmap(null);
		return bg;
	} 

	public static Bitmap getShadowBitmap2(Bitmap bm) {
		// 创建阴影滤镜
		BlurMaskFilter blurMaskFilter = new BlurMaskFilter(30,
				BlurMaskFilter.Blur.SOLID);
		// 创建阴影画笔
		Paint shadowPaint = new Paint();
		shadowPaint.setMaskFilter(blurMaskFilter);
		// 获取阴影
		int[] offsetXY = new int[2];
		Bitmap shadow = bm.extractAlpha(shadowPaint, offsetXY);
		shadow = shadow.copy(Config.ARGB_8888, true);
		// 创建新位图
		Bitmap bg = Bitmap.createBitmap(shadow.getWidth(), shadow.getHeight(),
				Config.ARGB_8888);
		// 创建画板
		Canvas canvas = new Canvas(bg);
		Paint paint = new Paint();
		// 绘制背景
		canvas.drawBitmap(shadow, 0, 0, paint);
		// 绘制图形
		canvas.drawBitmap(bm, (shadow.getWidth() - bm.getWidth()) / 2.0f,
				(shadow.getHeight() - bm.getHeight()) / 2.0f, paint);
		// 释放资源
		canvas.setBitmap(null);
		return bg;
	}
	
	private static Drawable getSystemIconDrawable(Context context,
			String iconName, final int  isUseOuterShadow ) {
		Context prePackageContext = null;
		try {
			prePackageContext = context.createPackageContext(PUBLIC_ICON_RES,
					Context.CONTEXT_IGNORE_SECURITY);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Create Res Apk Failed:NameNotFoundException");
		} catch (Exception e) {
			Log.e(TAG, "Create Res Apk Failed:Exception");
		}
		Drawable drawable = null;
		Resources resources = prePackageContext.getResources();
		if (prePackageContext != null) {
			int resId = resources.getIdentifier(iconName, "drawable",
					PUBLIC_ICON_RES);
			if (resId == 0) {
				return null;
			}
			drawable = resources.getDrawable(resId);
		}
		drawable = getSystemIconDrawable(prePackageContext,drawable,isUseOuterShadow);
		return drawable;
	}
	
	private  static  Drawable getSystemIconDrawable(Context context,Drawable d,final int iconMode){
		Context prePackageContext = null;
		try {
			prePackageContext = context.createPackageContext(PUBLIC_ICON_RES,
					Context.CONTEXT_IGNORE_SECURITY);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Create Res Apk Failed:NameNotFoundException");
		} catch (Exception e) {
			Log.e(TAG, "Create Res Apk Failed:Exception");
		}
		Resources resources = prePackageContext.getResources();
		Drawable drawable = d;
		if (USE_RIGHT_ANGLE && drawable != null) {
			int shadowId = 0;
			switch (iconMode) {
			case OUTER_SHADOW:
				shadowId = resources.getIdentifier("shadow", "drawable",
						PUBLIC_ICON_RES);
				break;
			case INTER_SHADOW:
				shadowId = resources.getIdentifier("shadow_app", "drawable",
						PUBLIC_ICON_RES);
				break;
			case NONE_SHADOW_WITH_BOTTOM:
				shadowId = resources.getIdentifier("shadow_not_self", "drawable",
						PUBLIC_ICON_RES);
				break;
			case NONE_SHADOW:
				//Maybe one day will use this tag;
				break;
			default:
				break;
			}	
			Bitmap bitmap1 = drawable2bitmap(drawable);
			Bitmap bitmap2 = null;
			if(iconMode == NONE_SHADOW_WITH_BOTTOM || iconMode == INTER_SHADOW){
				bitmap1 = zoomDrawable(bitmap1,ICON_REAL_SIZE,ICON_REAL_SIZE,resources);
				bitmap2 = getRoundedBitmap(bitmap1,ICON_REAL_SIZE);
			}else{
				bitmap2 = getRoundedBitmap(bitmap1);
			}
			if(USE_ICON_SHADOW){
				Bitmap bitmap3 = null;
				if (shadowId != 0) {
					Drawable shadow = resources.getDrawable(shadowId);
					if(shadow!=null){
						bitmap3 = getShadowBitmap1(bitmap2, shadow);
						drawable = new BitmapDrawable(resources, bitmap3);
					}else{
						drawable = new BitmapDrawable(resources, bitmap2);
					}
				}
			}else{
				drawable = new BitmapDrawable(resources, bitmap2);
			}
		}
		return drawable;
	}
	
	/*
	 * 判断图图标形状
	 */
	public static Drawable isRectangle(Drawable d,Context context) {
		Bitmap bmp = ((BitmapDrawable) d).getBitmap();
		if (null == bmp)
			return null;
		int width = bmp.getWidth();
		int height = bmp.getHeight();
		int DEST_SIZE = 230; // TODO magic number is good enough for now, should go with display info
        if (width > DEST_SIZE || height > DEST_SIZE) {
            // Resize bitmap
            float scale = 250f / Math.max(width, height);
            int destWidth = (int)(width * scale);
            int destHeight = (int)(height * scale);
            bmp = Bitmap.createScaledBitmap(bmp, destWidth, destHeight, false);
            width = bmp.getWidth();
            height = bmp.getHeight();
        }
		Drawable finalDrawable =null;
		int s = 0;
		int l = -1;
		int r = -1;
		int t = -1;
		int b = -1;
		int i, j;
		for (i = 0; i < height; ++i) {
			for (j = 0; j < width; ++j) {
				if ((bmp.getPixel(j, i) >>> 24) > 10) {
					if (l < 0 || l > j)
						l = j;
					if (r < 0 || r < j)
						r = j;
					if (t < 0 || t > i)
						t = i;
					if (b < 0 || b < i)
						b = i;
					++s;
				}
			}
		}
		float w = r - l;
		float h = b - t;
		if (s > 0.91f * w * h) {
			if (w > 0.97f * h && w < 1.02f * h) {
				//finalDrawable = clipDrawable(d, context.getResources(),r, l, b, t);
				finalDrawable = clipDrawable(new BitmapDrawable(context.getResources(),bmp), context.getResources(),r, l, b, t);
				finalDrawable = getSystemIconDrawable(context, finalDrawable ,INTER_SHADOW);
				return finalDrawable;
			}
		}
		return null;
	}
	
	private static Drawable clipDrawable(Drawable drawable,Resources res,int r,int l,int b,int t){
		Bitmap bg = Bitmap.createBitmap(Math.abs(r -l)+1,
				Math.abs(t - b)+1, Config.ARGB_8888);
		Canvas canvas = new Canvas(bg);
		canvas.save();
		Bitmap oldBitmap = drawable2bitmap(drawable);
		canvas.drawBitmap(oldBitmap, -l, -t, new Paint());
		canvas.restore();
		return new BitmapDrawable(res,bg);
	}
	
	
	private static Bitmap zoomDrawable(Bitmap oldbmp, int finalWidth, int finalHeight,Resources res) {
		Matrix matrix = new Matrix();
		int width = oldbmp.getWidth();
		int height = oldbmp.getHeight();
		float scaleWidth = ((float) finalWidth / width);
		float scaleHeight = ((float) finalHeight / height);
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height,
				matrix, true);
		return newbmp;
	}
	
	
	/*
	 * 判断App Icon是否需要加底托
	 */

	public static HashMap<String, String> mlables_icons = null;
	public static Bitmap getIcon(Context context,ResolveInfo info,int iconMode){
		Bitmap icon = null;
		icon = getIcon(context,info.activityInfo,iconMode);
		return icon;
	}
	
	public static Bitmap getIcon(Context context,ActivityInfo info,int iconMode){
		String iconName = getIconName(context,info);
		Bitmap icon = null;
		if(iconName!=null){
			icon= getBitmapByName(context,iconName,iconMode);
		}
		if(icon == null ){
			icon = getCustomerIcon();
		}
		return icon;
	}

	public static Bitmap getIcon(Context context,String pkg,String cls ,int iconMode){
		CharSequence key = pkg+ "$" + cls;
		String iconName = getIconName(context,key.toString());
		Bitmap icon = null;
		if(iconName!=null){
			icon= getBitmapByName(context,iconName,iconMode);
		}
		if(icon == null ){
			icon = getCustomerIcon();
		}
		return icon;
	}
	
	public static Bitmap getIcon(Context context,String pkg,int iconMode){
		String iconName = getIconName(context,pkg);
		Bitmap icon = null;
		if(iconName == null ){
			ResolveInfo rInfo=getPackageFirstResolveInfo(context,pkg);
			if(rInfo!=null)icon = getIcon(context,rInfo,iconMode);
		}else {
			icon = getBitmapByName(context,iconName,iconMode);
		}
		return icon;
	}

	public static Drawable getIconDrawble(Context context,ResolveInfo info,int iconMode){
		Drawable icon = null;
		icon = getIconDrawble(context,info.activityInfo,iconMode);
		return icon;
	}

	public static Drawable getIconDrawble(Context context,String pkg,String cls ,int iconMode){
			CharSequence key = pkg + "$" + cls;
			String iconName = getIconName(context,key.toString());
			Drawable icon = null;
			if(iconName!=null){
				icon= getSystemIconDrawable(context,iconName,iconMode);
			}
			if(icon == null ){
				icon = getCustomerIconDrawable();
			}
			return icon;
		}
	
	public static Drawable getIconDrawble(Context context,ActivityInfo info,int iconMode){
		String iconName = getIconName(context,info);
		Drawable icon = null;
		if(iconName!=null){
			icon= getSystemIconDrawable(context,iconName,iconMode);
		}
		if(icon == null ){
			icon = getCustomerIconDrawable();
		}
		return icon;
	}
	
	public static Drawable getIconDrawble(Context context,String pkg,int iconMode){
		String iconName = getIconName(context,pkg);
		Drawable icon = null;
		if(iconName == null ){
			ResolveInfo rInfo=getPackageFirstResolveInfo(context,pkg);
			if(rInfo!=null)icon = getIconDrawble(context,rInfo,iconMode);
		}else {
			icon = getSystemIconDrawable(context,iconName,iconMode);
		}
		return icon;
	}
	
	private static Bitmap getBitmapByName(Context context, String drawableName,int iconMode) {
		Bitmap bitmap = null;
		Drawable drawable = getSystemIconDrawable(context, drawableName,iconMode);
		if (drawable != null) {
			bitmap = drawable2bitmap(drawable);
		}
		return bitmap;
	}
	
	
	private static  Bitmap getCustomerIcon(){
		return null;
	}
	private static  Drawable getCustomerIconDrawable(){
		return null;
	}
	
	private static String getIconName(Context context,ActivityInfo info){
		CharSequence key = info.packageName + "$" + info.name;
		String iconName = getIconName(context,key.toString());
		if(iconName == null){
			key = info.loadLabel(context
					.getPackageManager());
			if(key!=null){
				iconName = getIconName(context,key.toString());
			}
		}
		return iconName;
	}
	
	private static String getIconName(Context context,String key){
		synchronized (configLock) {
				configClassArray(context, PACKAGE_CLASS);
		}
		String bitmapName = mlables_icons.get(key.trim());
		String iconName = null;
		if (bitmapName != null) {
			String[] s = bitmapName.split("\\.");
			if (s.length == 2) {
				iconName = s[0];
			}
		}
		return iconName;
	}
	
	private static ResolveInfo getPackageFirstResolveInfo(Context context,String packageName){
		Intent it = new Intent(Intent.ACTION_MAIN);
		it.addCategory(Intent.CATEGORY_LAUNCHER);
		it.setPackage(packageName);
		List<ResolveInfo> list=context.getPackageManager().queryIntentActivities(it, 0);
		ResolveInfo resolveInfo=null;
		if(!list.isEmpty()){
			resolveInfo = list.get(0);
		}
		return resolveInfo;
	}
	
	public static boolean isUseSystemIcon(Context context, ActivityInfo info) {
		if (info == null)
			return false;
		return isUseSystemIcon(context , info.packageName , info.name);
	}

	public static boolean isUseSystemIcon(Context context, String packageName,
			String className) {
		if (packageName == null || className == null)
			return false;
		CharSequence key = packageName + "$" + className;
		return getIconName(context , key.toString()) != null;
	}
	private static HashMap<String, String> configClassArray(Context context,
			String arrayName) {
		if (!isInitedConfig) {
			Context prePackageContext = null;
			try {
				prePackageContext = context.createPackageContext(
						PUBLIC_ICON_RES, Context.CONTEXT_IGNORE_SECURITY);
			} catch (NameNotFoundException e) {
				Log.e(TAG, "Create Res Apk Failed:NameNotFoundException");
			} catch (Exception e) {
				Log.e(TAG, "Create Res Apk Failed");
			}
			Resources res = prePackageContext.getResources();
			final int icon_real_size_dimenId = res.getIdentifier("app_icon_real_size","dimen",PUBLIC_ICON_RES);
			final int app_icon_radius_dimenId = res.getIdentifier("app_icon_radius","dimen",PUBLIC_ICON_RES);
			ICON_REAL_SIZE = res.getDimensionPixelSize(icon_real_size_dimenId);
			APP_ICON_RADIUS = res.getDimensionPixelSize(app_icon_radius_dimenId);
			
			HashMap<String, String> packageClassMap = new HashMap<String, String>();
			if (prePackageContext != null) {
				final int resId = res.getIdentifier(arrayName, "array", PUBLIC_ICON_RES);
				if (resId == 0) {
					return null;
				}
				final String[] packageClasseIcons = prePackageContext
						.getResources().getStringArray(resId);
				for (String packageClasseIcon : packageClasseIcons) {
					String[] packageClasses_Icon = packageClasseIcon.split("#");
					if (packageClasses_Icon.length == 2) {
						String[] packageClasses = packageClasses_Icon[0]
								.split("\\|");
						for (String s : packageClasses) {
							packageClassMap.put(s.trim(),
									packageClasses_Icon[1]);
							String[] packageClass = s.split("\\$");
							if(packageClass.length == 2){
								packageClassMap.put(packageClass[0], packageClasses_Icon[1]);
							}
						}
					}
				}
			}
			isInitedConfig = true;
			mlables_icons = packageClassMap;
		}
		return mlables_icons;
	}
	
	private Bitmap getIconFromCacheDir(Context context,String name){
		return null;
	}

// Aurora <Felix.Duan> <2014-12-5> <BEGIN> Add Huawei Honor 6 to navigation bar device list
    public static boolean hasNavBar() {
        return isIUNI_U3() || isHonor6() || isSony() || isNexus() || isLG();
    }

    private static boolean isIUNI_U3() {
        boolean isU3 = ("U3".equals(SystemProperties.get("ro.product.model")));
        Log.d("felixxp", "isU3 = " + isU3);
        return isU3;
    }

    private static boolean isHonor6() {
        boolean isHonor6 = ("H60-L01".equals(SystemProperties.get("ro.product.model")));
        Log.d("felixxp", "isHonor6-L01 = " + isHonor6);
        return isHonor6;
    }
    
    private static boolean isSony() {
        boolean isSony = ("Xperia Z".equals(SystemProperties.get("ro.product.model")) || "C6603".equals(SystemProperties.get("ro.product.model")));
        Log.d("felixxp", "isSony = " + isSony);
        return isSony;
    }
    
    private static boolean isNexus() {
        boolean isNexus = ("Nexus 5".equals(SystemProperties.get("ro.product.model")));
        Log.d("felixxp", "isNexus = " + isNexus);
        return isNexus;
    }
    
    private static boolean isLG() {
        boolean isLG = ("LG-D802".equals(SystemProperties.get("ro.product.model")));
        Log.d("felixxp", "isLG = " + isLG);
        return isLG;
    }
// Aurora <Felix.Duan> <2014-12-5> <END> Add Huawei Honor 6 to navigation bar device list
}