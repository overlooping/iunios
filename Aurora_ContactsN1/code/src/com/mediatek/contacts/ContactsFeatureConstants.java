package com.mediatek.contacts;

import com.android.contacts.GNContactsUtils;
import android.os.SystemProperties;

public class ContactsFeatureConstants {

    public interface FeatureOption {
        public static boolean MTK_SEARCH_DB_SUPPORT = com.aurora.featureoption.FeatureOption.MTK_SEARCH_DB_SUPPORT;
        public static boolean MTK_DIALER_SEARCH_SUPPORT = com.aurora.featureoption.FeatureOption.MTK_DIALER_SEARCH_SUPPORT;
        //Gionee <wangth><2013-04-25> modify for CR00801918 begin
        public static boolean MTK_GEMINI_SUPPORT = GNContactsUtils.isMultiSimEnabled();/*"yes".equals(SystemProperties.get("ro.gn.gemini.support", "yes"))
                && !GNContactsUtils.isMultiSimEnabled();*///aurora change zhouxiaobing 20140421
        //Gionee <wangth><2013-04-25> modify for CR00801918 end
        public static boolean MTK_VT3G324M_SUPPORT = false;//com.mediatek.featureoption.FeatureOption.MTK_VT3G324M_SUPPORT;
        public static boolean MTK_GEMINI_3G_SWITCH = com.aurora.featureoption.FeatureOption.MTK_GEMINI_3G_SWITCH;

        
        public static boolean MTK_BT_PROFILE_BPP = com.aurora.featureoption.FeatureOption.MTK_BT_PROFILE_BPP;
        
        
        // Gionee:wangth 20120616 add for CR00624473 begin
        public static boolean MTK_2SDCARD_SWAP = com.aurora.featureoption.FeatureOption.MTK_2SDCARD_SWAP;
        // Gionee:wangth 20120616 add for CR00624473 end
        
        public static boolean GN_OVERSEA_PRODUCT = com.aurora.featureoption.FeatureOption.GN_OVERSEA_PRODUCT;
        
    }
    
    public static boolean DBG_DIALER_SEARCH = true;
    public static boolean DBG_CONTACTS_GROUP = true;
    
    
    
    
    /*
     * qc--mtk
     */
    public static String EVENT_PRE_3G_SWITCH = "com.mtk.PRE_3G_SWITCH";
    
    public static final int GEMINI_SIM_1 = 0;
    public static final int GEMINI_SIM_2 = 1;
    
    
    
    

    /** RADIOOFF,  has SIM/USIM inserted but not in use . */
    public static final int SIM_INDICATOR_RADIOOFF = 1;
    /** LOCKED,  has SIM/USIM inserted and the SIM/USIM has been locked. */
    public static final int SIM_INDICATOR_LOCKED = 2;
    /** INVALID : has SIM/USIM inserted and not be locked but failed to register to the network. */
    public static final int SIM_INDICATOR_INVALID = 3; 
    /** SEARCHING : has SIM/USIM inserted and SIM/USIM state is Ready and is searching for network. */
    public static final int SIM_INDICATOR_SEARCHING = 4; 
     
    /** ROAMING : has SIM/USIM inserted and in roaming service(has no data connection). */  
    public static final int SIM_INDICATOR_ROAMING = 6; 
    /** CONNECTED : has SIM/USIM inserted and in normal service(not roaming) and data connected. */
    public static final int SIM_INDICATOR_CONNECTED = 7; 
    /** ROAMINGCONNECTED = has SIM/USIM inserted and in roaming service(not roaming) and data connected.*/
    public static final int SIM_INDICATOR_ROAMINGCONNECTED = 8;


    
    public static final int USIM_ERROR_GROUP_COUNT = -20;   //out number the  max count of groups
    public static final int USIM_ERROR_NAME_LEN = -10;      //the input group name is too long!
    
    public static final String DUAL_SIM_MODE_SETTING = "dual_sim_mode_setting";
    public static final long DEFAULT_SIM_SETTING_ALWAYS_ASK = -1;  
    public static final long DEFAULT_SIM_NOT_SET = -5; 
    public static final long VOICE_CALL_SIM_SETTING_INTERNET = -2;
    
    
    public static final String VOICE_CALL_SIM_SETTING =  "multi_sim_voice_call";
    
    public static final String ACTION_VOICE_CALL_DEFAULT_SIM_CHANGED = "android.intent.action.VOICE_CALL_DEFAULT_SIM";
    
    
}
