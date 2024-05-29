package cc.aabss.eventutils.api.websocket;

import cc.aabss.eventutils.EventUtils;
import cc.aabss.eventutils.api.NotificationToast;
import cc.aabss.eventutils.config.EventUtil;
import com.google.gson.JsonObject;

import static cc.aabss.eventutils.config.EventUtil.*;

public class EventListener {

    public static String LAST_FAMOUS_IP = "";
    public static String LAST_POTENTIAL_FAMOUS_IP = "";
    public static String LAST_MONEY_IP = "";
    public static String LAST_PARTNER_IP = "";
    public static String LAST_FUN_IP = "";
    public static String LAST_HOUSING_IP = "";
    public static String LAST_COMMUNITY_IP = "";
    public static String LAST_CIVILIZATION_IP = "";

    public void onFamousEvent(String message) {
        if (!EventUtils.FAMOUS_EVENT){
            return;
        }
        NotificationToast.addFamousEvent();
        LAST_FAMOUS_IP = connectFamousIP(message);
    }

    public void onPotentialFamousEvent(String message) {
        if (!EventUtils.POTENTIAL_FAMOUS_EVENT){
            return;
        }
        NotificationToast.addPotentialFamousEvent();
        LAST_POTENTIAL_FAMOUS_IP = connectFamousIP(message);
    }

    public void onMoneyEvent(JsonObject message) {
        if (EventUtils.MONEY_EVENT){
            NotificationToast.addMoneyEvent();
            LAST_MONEY_IP = getAndConnectIP(message);
        }
    }

    public void onPartnerEvent(JsonObject message) {
        if (EventUtils.PARTNER_EVENT){
            NotificationToast.addPartnerEvent();
            LAST_PARTNER_IP = getAndConnectIP(message);
        }
    }

    public void onFunEvent(JsonObject message) {
        if (EventUtils.FUN_EVENT){
            NotificationToast.addFunEvent();
            LAST_FUN_IP = getAndConnectIP(message);
        }
    }

    public void onHousingEvent() {
        if (EventUtils.HOUSING_EVENT){
            NotificationToast.addHousingEvent();
            if (EventUtils.AUTO_TP) {
                EventUtil.connect("hypixel.net");
            }
            LAST_HOUSING_IP = "hypixel.net";
        }
    }

    public void onCommunityEvent(JsonObject message) {
        if (EventUtils.COMMUNITY_EVENT){
            NotificationToast.addCommunityEvent();
            LAST_COMMUNITY_IP = getAndConnectIP(message);
        }
    }

    public void onCivilizationEvent(JsonObject message) {
        if (EventUtils.CIVILIZATION_EVENT){
            NotificationToast.addCivilizationEvent();
            LAST_CIVILIZATION_IP = getAndConnectIP(message);
        }
    }

}
