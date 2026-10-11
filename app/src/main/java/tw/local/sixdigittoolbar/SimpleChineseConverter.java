package tw.local.sixdigittoolbar;

import java.util.HashMap;
import java.util.Map;

/**
 * 極輕量簡體中文轉換器。
 * 專為同人誌、漫畫標題高頻繁簡體字與日文漢字新字體量身打造。
 * 零第三方依賴，純記憶體字元比對。
 */
final class SimpleChineseConverter {

    private static final Map<Character, Character> CHAR_MAP = new HashMap<>(600);

    static {
        // 同人誌、動漫、日文新字體與漢化高頻對照
        initPair("姉姐妹妹戀恋愛爱無无話话學学發发発发國国国国畫画画画體体体体");
        initPair("貓猫艦舰劍剑剣剑魔魔純纯纯纯墮堕堕堕亂乱乱乱獸兽獣兽調调教教");
        initPair("催催眠眠異异异异轉转転转生生聖圣処处處处隠隐陰隐語语錄录録录");
        initPair("戰战戦战記记變变変变癡痴痴痴漢汉化化寶宝貝贝龍龙竜龙騎骑士士");
        initPair("團团団团長长門门開开關关関关頭头飛飞風风鳥鸟魚鱼點点点点線线");
        initPair("專专専专屬属親亲見见聽听說说讀读読读寫写買买賣卖車车東东西西");
        initPair("鐵铁鉄铁鋼钢鏡镜鑑鉴鑒鉴鎖锁鏈链鍋锅錯错錦锦鍊炼針针釘钉釣钓");
        initPair("鈴铃銀银銃铳鏟铲銹锈銳锐鋒锋鋅锌鋤锄鋪铺鋸锯錐锥錘锤錚铮錮锢");
        initPair("錳锰錶表錸铼鍍镀鍛锻鍚钖鍔锷闊阔閃闪閉闭閈闬閏闰閑闲間间閔闵");
        initPair("閘闸閡阂閣阁閥阀閨闺閩闽閭闾閱阅閻阎閼阏閹阉閾阈闆板闌阑闐阗");
        initPair("闔阖闖闯闡阐經经経经驗验験验靈灵霊灵傳传伝传觸触触触術术師师");
        initPair("奴奴隷隶组组組组譯译訳译潔洁絕绝絶绝頂顶樂乐楽乐園园觀观観观");
        initPair("覺觉覚觉夢梦姬姬姫姬霸霸王王爭争奪夺幾几萬万億亿總总総总結结");
        initPair("終终續续続续誌志雜杂雑杂集集本本篇篇節节號号冊册頁页網网絡络");
        initPair("庫库軟软電电腦脑雲云輸输運运動动靜静態态勢势權权藏藏蔵藏設设");
        initPair("置置預预備备補补充充確确認认檢检索索尋寻標标題题名稱字字詞词");
        initPair("句句话话段段落落板面板面條条清清单单表表格格圖图圖像像片片影影");
        initPair("音音频頻道道訊讯息息銷销毀毁滅灭殺伤傷害害痛痛苦苦難难簡易简单");
        initPair("繁繁體体復复廣广告告攔拦截截防防护护牆墙鑰钥密密码码帳帐號号註注");
        initPair("登登录录離离線线連连接接網网页页瀏浏览览器器終终端端智智慧慧能能");
        initPair("神神経经網网络络模模型型訓训练练調调推推理理生生成成韓韩氣气気气");
        initPair("闘斗鬥斗勞劳労劳繪绘絵绘圖图図图視视観观縣县県县齒齿歯齿顏颜");
        initPair("顔颜聲声声声圓圆円圆奧奥奥奥實实実实賣卖売卖萬万万万與与与与");
        initPair("廣广広广獨独独独藝艺芸艺豐丰豊丰歸归帰归壽寿寿寿黨党党党");
        initPair("優优傷伤價价儉俭儀仪億亿儐parse優优");
    }

    private static void initPair(String pairs) {
        for (int i = 0; i < pairs.length() - 1; i += 2) {
            char from = pairs.charAt(i);
            char to = pairs.charAt(i + 1);
            CHAR_MAP.put(from, to);
        }
    }

    private SimpleChineseConverter() {
    }

    static String toSimplified(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Character mapped = CHAR_MAP.get(c);
            sb.append(mapped != null ? mapped : c);
        }
        return sb.toString();
    }
}
