package com.tianyi.helmet.service.gps;

import com.alibaba.fastjson.JSON;
import com.tianyi.helmet.BaseServiceTest;
import com.tianyi.helmet.server.entity.data.AbstractGpsData;
import com.tianyi.helmet.server.entity.data.GpsLineData;
import com.tianyi.helmet.server.service.data.TyBoxDataService;
import com.tianyi.helmet.server.service.data.TyBoxDataV1Resorver;
import com.tianyi.helmet.server.util.Dates;
import com.tianyi.helmet.server.vo.MainDetailVo;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by liuhanc on 2017/10/23.
 */
public class TyBoxDataServiceTest extends BaseServiceTest {
    @Autowired
    private TyBoxDataV1Resorver tyBoxDataV1Resorver;
    @Autowired
    private TyBoxDataService tyBoxDataService;

    @Test
    public void testResorve1() {
        String line = "0F865473039633451F0384221061E31203020A1A38000100AA0002006C000F00AA00150123D0B004498995000000B080090B1203020A1A37001000AA0015FFE1000900000000000000000000000000000CCD00000600AA000115000700AA000200AC000D00AB000121000300AB0001000008011700010000090117000100000D01180001210005011800022AC20006011800011900070118000200B0000E012100010000080121000100000901210001000005012200022ACE0006012200011900070122000200B0000D01230001210008012B0001000009012B000100000D012C0001210005012C00022ACE0006012C0001190007012C000200B00008013500010000090135000100000D01360001210005013600022ACC0006013600011900070136000200A800080801000100000908010001000005080200022AD20006080200011700070802000200AC000D0803000121000208040002212C0008080B0001000009080B000100000A080C0003000000000B080C0003000000000D080C0001210005080D00022AD20006080D0001170007080D000200AC0004080E0002007A00080815000100000908150001000005081600022AC60006081600011800070816000200A0000D0817000121000308180001000008082900010000090829000100000E082A0001000005082A00022ABF0006082A0001160007082A000200AC000D082B0001210008083300010000090833000100000D08340001210005083500022ABF0006083500011600070835000200AC0008083D0001000009083D000100000D083E0001210005083F00022ABE0006083F0001180007083F000200B00008084700010000090847000100000D08480001210005084900022ABE0006084900011800070849000200B00005085200022AC90006085200011900070852000200B00008085300010000090853000100000C08540003010000000808FB000100000908FB000100000D08FC000121000508FC00022ACE000608FC000118000708FC000200A80008090500010000090905000100000D09060001210005090600022AE00006090600011700070906000200A80008090F0001000009090F0001000005091100022AE00006091100011700070911000200A8000D09110001210008091900010000090919000100000C091A0003010000000D091A0001210005091B00022AD10006091B0001170007091B000200980008092300010000090923000100000D09240001210005092500022AD1FB00";
        GpsLineData lineData = tyBoxDataV1Resorver.resorveLineData(line);
        LocalDateTime gpsBoxTime = lineData.getBaseTime();
        LocalDateTime helmetTime = Dates.toLocalDateTime(Dates.parse("20180302102708", "yyyyMMddHHmmss"));//20171207163336
        long timeAdjustMillies = Dates.duration(gpsBoxTime, helmetTime);//第2个参数-第1个参数
        System.out.println("蓝牙盒子" + lineData.getImei() + "时间" + gpsBoxTime + "比头盔helmet1004时间" + helmetTime + (timeAdjustMillies > 0 ? "小" : "大") + Math.abs(timeAdjustMillies) + "毫秒");
        MainDetailVo<GpsLineData, AbstractGpsData> md = tyBoxDataService.saveGpsData(line, "helmet1004", -3, 1, false);

//        MainDetailVo<GpsLineData,AbstractGpsData>  md = gpsDataComponent.resorveGpsData(line,"helmet0020");
        System.out.println("原始数据:" + line);
        List<AbstractGpsData> list = md.getList();
        int idx = 1;
        for (AbstractGpsData data : list) {
            System.out.println(idx + ":" + data.toString());
            idx++;
        }
        lineData = md.getMain();
        System.out.println("主数据信息：" + JSON.toJSONString(lineData));
    }

}