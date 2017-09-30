package com.justcode.qqhongbao;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class HongbaoService extends AccessibilityService {
    private static final String WECHAT_OPEN_EN = "Open";
    private static final String WECHAT_OPENED_EN = "You've opened";
    private final static String QQ_DEFAULT_CLICK_OPEN = "点击拆开";
    private final static String QQ_HONG_BAO_PASSWORD = "口令红包";
    private final static String QQ_CLICK_TO_PASTE_PASSWORD = "点击输入口令";
    //列表界面是否返回标记
    private final static int LISTHB = 1;
    //红包界面是否返回标记
    private final static int CHATHB = 2;
    //是否有红包
    private boolean mLuckyMoneyReceived;
    private String lastFetchedHongbaoId = null;
    private long lastFetchedTime = 0;
    private static final int MAX_CACHE_TOLERANCE = 5000;
    private AccessibilityNodeInfo rootNodeInfo;
    private List<AccessibilityNodeInfo> mReceiveNode;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void recycle(AccessibilityNodeInfo info) {
        if (info.getChildCount() == 0) {
            //这个if代码的作用是：匹配“点击输入口令的节点，并点击这个节点”
            if (info.getText() != null && info.getText()
                    .toString().equals(QQ_CLICK_TO_PASTE_PASSWORD)) {
                info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            //这个if代码的作用是：匹配文本编辑框后面的发送按钮，并点击发送口令
            if (info.getClassName().toString()
                    .equals("android.widget.Button") && info.getText()
                    .toString().equals("发送")) {
                info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    recycle(info.getChild(i));
                }
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String className = event.getClassName().toString();
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                handleNotification(event);
                break;

            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                if (className.equals("com.tencent.mobileqq.activity.SplashActivity")) {
                    //QQ消息列表界面
                    AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                    if (nodeInfo != null) {
                        //输入框
                        List<AccessibilityNodeInfo> input = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mobileqq:id/inputBar");
                        //判断是否有输入框、有输入框的是聊天界面，没有输入框的是列表界面
                        if (input.size() == 0) {
                            //列表界面
                            Log.e("HongbaoService", "1列表界面");
                            listHongbao(nodeInfo,event,CHATHB);
                        } else {
                            //聊天界面
                            Log.e("HongbaoService", "1聊天界面");
                            chatHongbao(event,CHATHB);
                        }
                    }
                } else {
                    AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                    if (nodeInfo != null) {
                        //输入框
                        List<AccessibilityNodeInfo> input = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mobileqq:id/inputBar");
                        //判断是否有输入框、有输入框的是聊天界面，没有输入框的是列表界面
                        if (input.size() == 0) {
                            //列表界面
                            Log.e("HongbaoService", "2列表界面");
                            listHongbao(nodeInfo,event,CHATHB);
                        } else {
                            //聊天界面
                            Log.e("HongbaoService", "2聊天界面");
                            chatHongbao(event,CHATHB);
                        }
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED: //是否进入了红包消息（聊天）界面
                Log.e("HongbaoService", "2222" + className);

                if (className.equals("com.tencent.mobileqq.activity.SplashActivity")) {
                    //QQ消息列表界面
                    AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                    if (nodeInfo != null) {
                        //输入框
                        List<AccessibilityNodeInfo> input = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mobileqq:id/inputBar");
                        //判断是否有输入框、有输入框的是聊天界面，没有输入框的是列表界面
                        if (input.size() == 0) {
                            //列表界面
                            Log.e("HongbaoService", "3列表界面");
                            listHongbao(nodeInfo, event, LISTHB);

                        } else {
                            //聊天界面
                            Log.e("HongbaoService", "3聊天界面");
                            chatHongbao(event, CHATHB);
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void listHongbao(AccessibilityNodeInfo nodeInfo, AccessibilityEvent event, int back) {
        //所有消息
        List<AccessibilityNodeInfo> list = nodeInfo
                .findAccessibilityNodeInfosByViewId("com.tencent.mobileqq:id/relativeItem");
        if (list.size() != 0) {
            //遍历所有消息
            for (AccessibilityNodeInfo n : list) {
                //getChild(0)表示头像布局,getChild(1)表示标题加内容加未读消息
                AccessibilityNodeInfo child1 = n.getChild(1);
                //getChildCount==3有未读消息，==2没有未读消息
                if (child1 != null && child1.getChildCount() == 3 && !child1.getChild(2).getText().toString().isEmpty()) {
                    //有未读消息
                    Log.e("HongbaoService", "哈哈哈哈哈");
                    //点击进入消息界面
                    n.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    //遍历消息是否有红包，没有就返回
                    Log.e("HongbaoService", "aaaaaa");
                    chatHongbao(event,back);
                } else {
                    Log.e("HongbaoService", "没有未读消息不处理");
                }
            }
        }
    }

    //聊天界面处理
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void chatHongbao(AccessibilityEvent event, int back) {
        this.rootNodeInfo = event.getSource();
        if (rootNodeInfo == null) {
            return;
        }
        mReceiveNode = null;
        //判断是否有红包
        if (checkNodeInfo2()) {
            //有红包
            Log.e("HongbaoService", "有红包噢");
            if (mLuckyMoneyReceived && (mReceiveNode != null)) {
                int size = mReceiveNode.size();
                if (size > 0) {
                    String id = getHongbaoText(mReceiveNode.get(size - 1));
                    long now = System.currentTimeMillis();
                    if (shouldReturn(id, now - lastFetchedTime))
                        return;
                    lastFetchedHongbaoId = id;
                    lastFetchedTime = now;
                    AccessibilityNodeInfo cellNode = mReceiveNode.get(size - 1);
                    if (cellNode.getText().toString().equals("口令红包已拆开")) {
                        return;
                    }
                    cellNode.getParent()
                            .performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    if (cellNode.getText().toString().equals(QQ_HONG_BAO_PASSWORD)) {
                        AccessibilityNodeInfo rowNode = getRootInActiveWindow();
                        if (rowNode == null) {
                            Log.e("hongbao", "noteInfo is　null");
                            return;
                        } else {
                            recycle(rowNode);
                        }
                    }
                    mLuckyMoneyReceived = false;
                }
            }
        } else {
            switch (back) {
                case 1:
                    //没有红包返回,列表界面处理方式
                    Log.e("HongbaoService", "没有红包返回");
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    break;
                case 2:
                    //没有红包返回，聊天界面处理方式
                    Log.e("HongbaoService", "没有红包返回");
                    break;
                default:
                    break;
            }
        }
    }

    private void handleNotification(AccessibilityEvent event) {
        List<CharSequence> texts = event.getText();
        if (!texts.isEmpty()) {
            for (CharSequence text : texts) {
                String content = text.toString();
                if (content.contains("[QQ红包]")) {
                    if (event.getParcelableData() != null &&
                            event.getParcelableData() instanceof Notification) {
                        Notification notification = (Notification) event.getParcelableData();
                        PendingIntent pendingIntent = notification.contentIntent;
                        try {
                            pendingIntent.send();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean checkNodeInfo2() {
        //聊天会话窗口，遍历节点匹配“点击拆开”，“口令红包”，“点击输入口令”
        List<AccessibilityNodeInfo> nodes1 = this
                .findAccessibilityNodeInfosByTexts(this.rootNodeInfo,
                        new String[]{QQ_DEFAULT_CLICK_OPEN,
                                QQ_HONG_BAO_PASSWORD,
                                QQ_CLICK_TO_PASTE_PASSWORD});
        if (!nodes1.isEmpty()) {
            String nodeId = Integer
                    .toHexString(System.identityHashCode(this.rootNodeInfo));
            if (!nodeId.equals(lastFetchedHongbaoId)) {
                mLuckyMoneyReceived = true;
                mReceiveNode = nodes1;
            }
            return true;
        } else {
            return false;
        }

    }

    private String getHongbaoText(AccessibilityNodeInfo node) {
        //获取红包上的文本
        String content;
        try {
            AccessibilityNodeInfo i = node.getParent().getChild(0);
            content = i.getText().toString();
        } catch (NullPointerException npe) {
            return null;
        }
        return content;
    }

    private boolean shouldReturn(String id, long duration) {
        // ID为空
        if (id == null) return true;
        // 名称和缓存不一致
        if (duration < MAX_CACHE_TOLERANCE && id.equals(lastFetchedHongbaoId)) {
            return true;
        }
        return false;
    }

    private List<AccessibilityNodeInfo> findAccessibilityNodeInfosByTexts(AccessibilityNodeInfo nodeInfo, String[] texts) {
        for (String text : texts) {
            if (text == null) continue;
            List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByText(text);
            if (!nodes.isEmpty()) {
                if (text.equals(WECHAT_OPEN_EN) && !nodeInfo.findAccessibilityNodeInfosByText(WECHAT_OPENED_EN).isEmpty()) {
                    continue;
                }
                return nodes;
            }
        }
        return new ArrayList<>();
    }

    @Override
    public void onInterrupt() {
    }
}
