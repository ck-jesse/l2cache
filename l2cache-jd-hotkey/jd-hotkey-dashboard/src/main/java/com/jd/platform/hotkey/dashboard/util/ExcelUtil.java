package com.jd.platform.hotkey.dashboard.util;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.List;

import com.jd.platform.hotkey.dashboard.common.domain.dto.ExcelDataDto;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExcelUtil {

    private static Logger log = LoggerFactory.getLogger(ExcelUtil.class);

    public static void exportExcel(HttpServletResponse response, ExcelDataDto data) {
        log.info("导出解析开始，fileName:{}",data.getFileName());
        try {
            //实例化HSSFWorkbook
            XSSFWorkbook workbook = new XSSFWorkbook();
            //创建一个Excel表单，参数为sheet的名字
            XSSFSheet sheet = workbook.createSheet("sheet1");
            //设置表头
            setTitle(workbook, sheet, data.getHead());
            //设置单元格并赋值
            setData(sheet, data.getRows());
            //设置浏览器下载
            setBrowser(response, workbook, data.getFileName());
            log.info("导出解析成功!");
        } catch (Exception e) {
            log.info("导出解析失败!");
            e.printStackTrace();
        }
    }
    /**
     * 设置标题
     * @param workbook
     * @param sheet
     * @param str
     */
    private static void setTitle(XSSFWorkbook workbook, XSSFSheet sheet, List<String> str) {
        try {
            XSSFRow row = sheet.createRow(0);
            //设置为居中加粗,格式化时间格式
            XSSFCellStyle style = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            style.setDataFormat(HSSFDataFormat.getBuiltinFormat("m/d/yy h:mm"));
            //创建表头名称
            XSSFCell cell;
            for (int j = 0; j < str.size(); j++) {
                cell = row.createCell(j);
                cell.setCellValue(str.get(j));
                cell.setCellStyle(style);
            }
        } catch (Exception e) {
            log.info("导出时设置表头失败！");
            e.printStackTrace();
        }
    }

    /**
     * 添加数据
     * @param sheet
     * @param rows
     */
    private static void setData(XSSFSheet sheet, List<List<String>> rows) {
        try{
            int rowNum = 1;
            for (int i = 0; i < rows.size(); i++) {
                XSSFRow row = sheet.createRow(rowNum);
                List<String> po = rows.get(i);
                for (int j = 0; j < po.size(); j++) {
                    if (j==0){  row.createCell(j).setCellValue(po.get(0)); }
                    if (j==1){  row.createCell(j).setCellValue(po.get(1)); }
                    if (j==2){  row.createCell(j).setCellValue(po.get(2)); }
                }
                rowNum++;
            }

            log.info("表格赋值成功！");
        }catch (Exception e){
            log.info("表格赋值失败！");
            e.printStackTrace();
        }
    }

    /**
     * 使用浏览器下载
     * @param response
     * @param workbook
     * @param fileName
     */
    private static void setBrowser(HttpServletResponse response, XSSFWorkbook workbook, String fileName) {
        try {
            //清空response
            response.reset();
            //设置response的Header
            response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.setContentType("application/vnd.ms-excel;charset=utf-8");
            OutputStream os = response.getOutputStream();
            //将excel写入到输出流中
            workbook.write(os);
            os.flush();
            os.close();
            log.info("设置浏览器下载成功！");
        } catch (Exception e) {
            log.info("设置浏览器下载失败！");
            e.printStackTrace();
        }
    }
}