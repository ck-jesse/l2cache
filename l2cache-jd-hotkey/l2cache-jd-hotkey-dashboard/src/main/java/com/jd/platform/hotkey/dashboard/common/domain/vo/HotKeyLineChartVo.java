package com.jd.platform.hotkey.dashboard.common.domain.vo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author liyunfeng31
 */
public class HotKeyLineChartVo {

    private List<String> xAxis;

    private Map<String,int[]> series;

    public HotKeyLineChartVo() {
    }

    public HotKeyLineChartVo(List<String> xAxis, Map<String, int[]> series) {
        this.xAxis = xAxis;
        this.series = series;
    }

    public List<String> getxAxis() {
        return xAxis;
    }

    public Map<String, int[]> getSeries() {
        return series;
    }


    private Set<String> xAxis2;

    private Map<String,List<Integer>> series2;

    private Set<String> legend;

    public Set<String> getxAxis2() {
        return xAxis2;
    }

    public void setxAxis2(Set<String> xAxis2) {
        this.xAxis2 = xAxis2;
    }

    public Map<String, List<Integer>> getSeries2() {
        return series2;
    }

    public void setSeries2(Map<String, List<Integer>> series2) {
        this.series2 = series2;
    }

    public Set<String> getLegend() {
        return legend;
    }

    public void setLegend(Set<String> legend) {
        this.legend = legend;
    }
}
