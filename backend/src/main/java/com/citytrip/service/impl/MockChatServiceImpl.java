package com.citytrip.service.impl;

import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatStatusVO;
import com.citytrip.model.vo.ChatVO;
import com.citytrip.service.ChatService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class MockChatServiceImpl implements ChatService {

    @Override
    public ChatStatusVO getStatus() {
        ChatStatusVO vo = new ChatStatusVO();
        vo.setProvider("mock");
        vo.setConfigured(true);
        vo.setRealModelAvailable(false);
        vo.setFallbackToMock(true);
        vo.setTimeoutSeconds(0);
        vo.setModel("mock");
        vo.setBaseUrl("local-mock");
        vo.setMessage("当前聊天服务正在使用本地 Mock 响应。");
        return vo;
    }

    @Override
    public ChatVO answerQuestion(ChatReqDTO req) {
        String q = req.getQuestion() != null ? req.getQuestion() : "";
        ChatReqDTO.ChatContext ctx = req.getContext();

        ChatVO vo = new ChatVO();
        String answer = "作为一个AI旅游助手，我目前处于模拟离线状态。";
        List<String> tips = Arrays.asList("成都有哪些适合拍照的地方？", "武侯祠有什么历史背景？");

        // 根据关键字做简单的 Mock 回答
        if (q.contains("拍照") || q.contains("出片") || q.contains("机位")) {
            answer = "如果想在成都拍出好照片，推荐你早上前往【武侯祠】的红墙竹影，光线非常柔和；下午可以去【东郊记忆】，那里有浓郁的工业复古风和废土赛博朋克感。另外【锦里】的夜景也非常适合汉服拍摄哦。";
            tips = Arrays.asList("锦里晚上几点去最好？", "东郊记忆要门票吗？");
        } else if (q.contains("雨天") || q.contains("下雨") || q.contains("下雨去哪")) {
            answer = "如果遇到雨天，不用担心！成都有很多极具特色的室内场馆可以打卡。比如可以去【成都博物馆】或者【四川博物院】了解古蜀文化；也可以在【太古里】的方所书店找个看书的好地方度过一个安静的下午。如果是吃货，找一家地道的火锅店看着外面的雨吃着热气腾腾的火锅也是一绝！";
            tips = Arrays.asList("成都市区有哪些好逛的商场？", "推荐几家好吃的火锅店？");
        } else if (q.contains("历史") || q.contains("文化") || q.contains("武侯祠") || q.contains("杜甫")) {
            answer = "成都是一座有着三千多年建城史的文化名城。武侯祠是全国唯一的君臣合祀祠庙，纪念了诸葛亮和刘备；而杜甫草堂则是诗圣杜甫流寓成都时的故居。走在这些地方，仿佛能触摸到千年前的三国风云和诗意长安。";
            tips = Arrays.asList("武侯祠游玩大约要多久？", "除了杜甫草堂还有哪些名人故居？");
        } else if (q.contains("春熙路") || q.contains("太古里") || q.contains("购物") || q.contains("逛街")) {
            answer = "春熙路和太古里是成都的商业中心。IFS的顶层有一只正在爬墙的巨大熊猫雕塑，是必打卡的地标。太古里则是保留了川西风格古建筑的开放式街区，传统与现代在这里完美交融，非常适合逛街喝咖啡。";
            tips = Arrays.asList("有什么好吃的街头小吃推荐？", "那只大熊猫具体在几楼？");
        } else if (q.contains("亲子") || q.contains("带孩子") || q.contains("小孩")) {
            answer = "考虑到带小朋友出行，成都【大熊猫繁育研究基地】绝对是首选，孩子们一定会被滚滚们萌化；其次推荐去【成都自然博物馆】，馆藏丰富且有着巨大的恐龙骨架，非常适合寓教于乐。另外，室内的【浩海立方海洋馆】也是不错的亲子选择。";
            tips = Arrays.asList("去熊猫基地要多早起？", "自然博物馆周一开门吗？");
        } else if (q.contains("夜游") || q.contains("晚上") || q.contains("夜市")) {
            answer = "成都的夜生活超级丰富！晚饭后可以去【九眼桥】欣赏锦江夜色，还可以去附近的酒吧街小酌一杯；如果想体验市井气，【建设路夜市】和【抚琴夜市】有着吃不完的成都特色小吃。";
            tips = Arrays.asList("九眼桥几点关灯？", "建设路夜市必吃的是什么？");
        } else {
            // 通用兜底
            answer = "收到你的问题啦：“" + q
                    + "”。在我的“行‘城’有数”智能旅游数据库里，成都有着无数令人惊喜的角落。不论你是想品尝地道川菜，还是漫步古老的街巷，我都会为你量身打造最佳的出行方案！目前虽然我还没有全面接入大模型，但我已经在帮你规划基础行程了哦。";
        }

        // ================= 基于前端传来的结构化Context进行智能小贴士追加 =================
        if (ctx != null) {
            StringBuilder suffix = new StringBuilder();
            
            if (Boolean.TRUE.equals(ctx.getRainy()) && !q.contains("雨天")) {
                suffix.append("（贴心提示：注意到您的行程设定里有雨天，出行时注意带好雨具，室外排队时多加小心。） ");
            }
            if ("亲子".equals(ctx.getCompanionType()) && !q.contains("孩子") && !q.contains("亲子")) {
                suffix.append("（亲子出行小贴士：带小朋友出行请注意行程不要太满，多留休息时间哦！） ");
            }
            if (ctx.getPreferences() != null && ctx.getPreferences().contains("拍照") && !q.contains("拍照")) {
                suffix.append("（打卡推荐：这个地方周围也是出片率极高的地方，记得带上相机！） ");
            }
            
            if (suffix.length() > 0) {
                answer += "\n\n" + suffix.toString();
            }
        }

        vo.setAnswer(answer);
        vo.setRelatedTips(tips);
        return vo;
    }
}
