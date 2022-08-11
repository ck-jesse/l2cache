package com.jd.platform.hotkey.dashboard.common.domain.vo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author liyunfeng31
 */
public class LineChartVo {

    private Set<String> legend;

    private Set<String> xAxis;

    private Map<String,List<Integer>> series;

    public LineChartVo(Set<String> xAxis, Map<String, List<Integer>> series) {
        this.xAxis = xAxis;
        this.series = series;
    }

    public LineChartVo(Set<String> legend, Set<String> xAxis, Map<String, List<Integer>> series) {
        this.legend = legend;
        this.xAxis = xAxis;
        this.series = series;
    }

    public Set<String> getxAxis() {
        return xAxis;
    }

    public void setxAxis(Set<String> xAxis) {
        this.xAxis = xAxis;
    }

    public Map<String, List<Integer>> getSeries() {
        return series;
    }

    public void setSeries(Map<String, List<Integer>> series) {
        this.series = series;
    }

    public Set<String> getLegend() {
        return legend;
    }

    public void setLegend(Set<String> legend) {
        this.legend = legend;
    }
}
