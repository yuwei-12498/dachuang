package com.citytrip.service.domain.planning;

import com.citytrip.service.TravelTimeService;
import com.citytrip.service.geo.GeoPoint;
import com.citytrip.service.geo.GeoRouteEstimate;
import com.citytrip.service.geo.GeoRouteStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SegmentRouteGuideServiceTest {

    private final SegmentRouteGuideService service = new SegmentRouteGuideService();

    @Test
    void buildGuideUsesRealProviderStepsWithoutInventingFacts() {
        TravelTimeService.TravelLegEstimate leg = new TravelTimeService.TravelLegEstimate(
                27,
                BigDecimal.valueOf(8.4D),
                "地铁+步行",
                List.of(
                        point(30.65731D, 104.081703D),
                        point(30.655D, 104.079D),
                        point(30.646D, 104.048D)
                ),
                new GeoRouteEstimate(
                        27,
                        BigDecimal.valueOf(8.4D),
                        "地铁+步行",
                        List.of(
                                point(30.65731D, 104.081703D),
                                point(30.655D, 104.079D),
                                point(30.646D, 104.048D)
                        ),
                        List.of(
                                new GeoRouteStep(
                                        "步行 300 米到天府广场地铁站 B 口",
                                        "walk",
                                        300,
                                        4,
                                        null,
                                        null,
                                        "天府广场",
                                        "B口",
                                        null,
                                        null,
                                        List.of(point(30.65731D, 104.081703D), point(30.655D, 104.079D))
                                ),
                                new GeoRouteStep(
                                        "乘 1 号线往文殊院方向 2 站",
                                        "metro",
                                        6200,
                                        15,
                                        "1号线",
                                        "天府广场",
                                        "文殊院",
                                        null,
                                        null,
                                        2,
                                        List.of(point(30.655D, 104.079D), point(30.657D, 104.061D))
                                ),
                                new GeoRouteStep(
                                        "从 C 口出站后步行 450 米到景点入口",
                                        "walk",
                                        450,
                                        6,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "C口",
                                        null,
                                        List.of(point(30.657D, 104.061D), point(30.646D, 104.048D))
                                )
                        )
                )
        );

        var guide = service.buildGuide(leg);

        assertThat(guide.getSummary()).contains("步行 300 米");
        assertThat(guide.getSummary()).contains("地铁 2 站");
        assertThat(guide.getSummary()).contains("步行 450 米");
        assertThat(guide.getDetailAvailable()).isTrue();
        assertThat(guide.getIncompleteReason()).isNull();
        assertThat(guide.getTransportMode()).isEqualTo("地铁+步行");
        assertThat(guide.getSource()).isEqualTo("geo-provider");
        assertThat(guide.getSteps()).hasSize(3);
        assertThat(guide.getSteps().get(1).getLineName()).isEqualTo("1号线");
        assertThat(guide.getSteps().get(1).getStopCount()).isEqualTo(2);
        assertThat(guide.getSteps().get(2).getExitName()).isEqualTo("C口");
        assertThat(guide.getPathPoints()).hasSize(3);
    }

    @Test
    void buildGuideDegradesWhenStructuredStepsMissingButPathExists() {
        TravelTimeService.TravelLegEstimate leg = new TravelTimeService.TravelLegEstimate(
                14,
                BigDecimal.valueOf(5.1D),
                "打车",
                List.of(
                        point(30.650D, 104.060D),
                        point(30.646D, 104.048D)
                ),
                new GeoRouteEstimate(
                        14,
                        BigDecimal.valueOf(5.1D),
                        "打车",
                        List.of(
                                point(30.650D, 104.060D),
                                point(30.646D, 104.048D)
                        ),
                        List.of()
                )
        );

        var guide = service.buildGuide(leg);

        assertThat(guide.getSummary()).isEqualTo("打车约 14 分钟，约 5.1 公里");
        assertThat(guide.getDetailAvailable()).isFalse();
        assertThat(guide.getIncompleteReason()).isEqualTo("该段暂未获取完整导航详情");
        assertThat(guide.getSteps()).isEmpty();
        assertThat(guide.getPathPoints()).hasSize(2);
    }

    @Test
    void buildGuideFallsBackToLegPathWhenProviderPathOnlyContainsInvalidPoints() {
        TravelTimeService.TravelLegEstimate leg = new TravelTimeService.TravelLegEstimate(
                12,
                BigDecimal.valueOf(3.2D),
                "公交+步行",
                List.of(
                        point(30.650D, 104.060D),
                        point(30.646D, 104.048D)
                ),
                new GeoRouteEstimate(
                        12,
                        BigDecimal.valueOf(3.2D),
                        "公交+步行",
                        List.of(new GeoPoint(null, null)),
                        List.of()
                )
        );

        var guide = service.buildGuide(leg);

        assertThat(guide.getPathPoints()).hasSize(2);
        assertThat(guide.getIncompleteReason()).isEqualTo("该段暂未获取完整导航详情");
    }

    @Test
    void buildGuideMarksFailureWhenNoStepsAndNoRenderableFacts() {
        TravelTimeService.TravelLegEstimate leg = new TravelTimeService.TravelLegEstimate(
                0,
                null,
                null,
                List.of(),
                null
        );

        var guide = service.buildGuide(leg);

        assertThat(guide.getSummary()).isEqualTo("该段导航数据暂不可用");
        assertThat(guide.getDetailAvailable()).isFalse();
        assertThat(guide.getIncompleteReason()).isEqualTo("该段导航数据获取失败，当前仅能提供概略通行信息");
        assertThat(guide.getPathPoints()).isEmpty();
    }

    private GeoPoint point(double latitude, double longitude) {
        return new GeoPoint(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude));
    }
}
