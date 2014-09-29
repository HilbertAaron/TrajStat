/* Copyright 2014 - Yaqiang Wang,
 * yaqiang.wang@gmail.com
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 */
package trajstat.trajectory;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;
import org.meteoinfo.global.MIMath;
import org.meteoinfo.table.DataTypes;
import org.meteoinfo.global.util.GlobalUtil;
import org.meteoinfo.layer.LayerDrawType;
import org.meteoinfo.layer.VectorLayer;
import org.meteoinfo.legend.LegendManage;
import org.meteoinfo.legend.LegendScheme;
import org.meteoinfo.shape.PointZ;
import org.meteoinfo.shape.PolylineZShape;
import org.meteoinfo.shape.ShapeTypes;

/**
 *
 * @author Yaqiang Wang
 */
public class TrajUtil {

    /**
     * Trajectory calculation
     *
     * @param trajConfig Trajectory configure
     * @throws IOException
     * @throws InterruptedException
     */
    public static void trajCal(TrajConfig trajConfig) throws IOException, InterruptedException {
        String pluginDir = GlobalUtil.getAppPath(TrajUtil.class);
        String workDir = pluginDir + File.separator + "working";
        String cfn = workDir + File.separator + "CONTROL";

        //Loop
        int dayNum = trajConfig.getDayNum();
        int hourNum = trajConfig.getStartHoursNum();
        //int tnum = dayNum * hourNum;
        for (int i = 0; i < dayNum; i++) {
            for (int j = 0; j < hourNum; j++) {
                //Write control file
                trajConfig.upateStartTime(i, j);
                trajConfig.saveControlFile(cfn);

                //Run trajectory module
                ProcessBuilder pb = new ProcessBuilder(trajConfig.getTrajExcuteFileName());
                pb.directory(new File(workDir));
                Process process = pb.start();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String s;
                while ((s = bufferedReader.readLine()) != null) {
                    System.out.println(s);
                }
                process.waitFor();
            }
        }
    }

    /**
     * Convert trajectory end point file to TGS file
     *
     * @param trajfn Trajectory end point file
     * @param tgsfn TGS file
     * @throws IOException
     */
    public static void trajToTGS(String trajfn, String tgsfn) throws IOException {
        if (!new File(trajfn).exists()) {
            return;
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(tgsfn)));
        bw.write("start_year,start_month,start_day,start_hour,year,month,day,hour,age_hour,latitude,longitude,height,press");
        bw.newLine();

        BufferedReader sr = new BufferedReader(new FileReader(new File(trajfn)));

        //Record #1
        String aLine = sr.readLine().trim();
        String[] dataArray = aLine.split("\\s+");
        int meteoFileNum = Integer.parseInt(dataArray[0]);

        //Record #2
        int m, n;
        for (m = 0; m < meteoFileNum; m++) {
            sr.readLine();
        }

        //Record #3
        aLine = sr.readLine().trim();
        dataArray = aLine.split("\\s+");
        int trajNum = Integer.parseInt(dataArray[0]);

        //Record #4             
        String[] sDates = new String[trajNum];
        for (m = 0; m < trajNum; m++) {
            aLine = sr.readLine().trim();
            dataArray = aLine.split("\\s+");
            aLine = "";
            for (n = 0; n <= 3; n++) {
                if (dataArray[n].length() < 2) {
                    dataArray[n] = "0" + dataArray[n];
                }
                aLine = aLine + dataArray[n] + ",";
            }
            sDates[m] = aLine;
        }

        //Record #5
        sr.readLine();

        //Record #6
        int id;
        String wYear, wMonth, wDay, wHour;
        String ageHour, lat, lon, Height, press;
        List<String>[] trajLines = new ArrayList[trajNum];
        for (m = 0; m < trajLines.length; m++) {
            trajLines[m] = new ArrayList();
        }
        //int pNum = 0;
        while (true) {
            aLine = sr.readLine();
            if (aLine == null) {
                break;
            }
            if (aLine.isEmpty()) {
                continue;
            }
            dataArray = aLine.trim().split("\\s+");

            if (dataArray.length < 13) {
                JOptionPane.showMessageDialog(null, "Wrong file format! Please Check!"
                        + System.getProperty("line.separator") + "Line: " + aLine);
                sr.close();
                if (new File(tgsfn).exists()) {
                    new File(tgsfn).delete();
                }
                return;
            }
            //pNum += 1;

            id = Integer.parseInt(dataArray[0]);
            wYear = dataArray[2];
            wMonth = dataArray[3];
            wDay = dataArray[4];
            wHour = dataArray[5];
            ageHour = dataArray[8];
            lat = dataArray[9];
            lon = dataArray[10];
            Height = dataArray[11];
            press = dataArray[12];
            aLine = sDates[id - 1] + wYear + "," + wMonth + "," + wDay + "," + wHour + "," + ageHour + "," + lat + "," + lon + "," + Height + "," + press;
            trajLines[id - 1].add(aLine);
        }
        sr.close();

        for (m = 0; m < trajNum; m++) {
            for (n = 0; n < trajLines[m].size(); n++) {
                bw.write(trajLines[m].get(n));
                bw.newLine();
            }
        }
        bw.close();
    }

    /**
     * Convert trajectory end point file to TGS file
     *
     * @param trajfns Trajectory end point files
     * @param tgsfn TGS file
     * @throws IOException
     */
    public static void trajToTGS(List<String> trajfns, String tgsfn) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(tgsfn)));
        bw.write("start_year,start_month,start_day,start_hour,year,month,day,hour,age_hour,latitude,longitude,height,press");
        bw.newLine();

        for (String trajfn : trajfns) {
            if (!new File(trajfn).exists()) {
                continue;
            }

            BufferedReader sr = new BufferedReader(new FileReader(new File(trajfn)));

            //Record #1
            String aLine = sr.readLine().trim();
            String[] dataArray = aLine.split("\\s+");
            int meteoFileNum = Integer.parseInt(dataArray[0]);

            //Record #2
            int m, n;
            for (m = 0; m < meteoFileNum; m++) {
                sr.readLine();
            }

            //Record #3
            aLine = sr.readLine().trim();
            dataArray = aLine.split("\\s+");
            int trajNum = Integer.parseInt(dataArray[0]);

            //Record #4             
            String[] sDates = new String[trajNum];
            for (m = 0; m < trajNum; m++) {
                aLine = sr.readLine().trim();
                dataArray = aLine.split("\\s+");
                aLine = "";
                for (n = 0; n <= 3; n++) {
                    if (dataArray[n].length() < 2) {
                        dataArray[n] = "0" + dataArray[n];
                    }
                    aLine = aLine + dataArray[n] + ",";
                }
                sDates[m] = aLine;
            }

            //Record #5
            sr.readLine();

            //Record #6
            int id;
            String wYear, wMonth, wDay, wHour;
            String ageHour, lat, lon, Height, press;
            List<String>[] trajLines = new ArrayList[trajNum];
            for (m = 0; m < trajLines.length; m++) {
                trajLines[m] = new ArrayList();
            }
            //int pNum = 0;
            while (true) {
                aLine = sr.readLine();
                if (aLine == null) {
                    break;
                }
                if (aLine.isEmpty()) {
                    continue;
                }
                dataArray = aLine.trim().split("\\s+");

                if (dataArray.length < 13) {
                    JOptionPane.showMessageDialog(null, "Wrong file format! Please Check!"
                            + System.getProperty("line.separator") + "Line: " + aLine);
                    sr.close();
                    if (new File(tgsfn).exists()) {
                        new File(tgsfn).delete();
                    }
                    return;
                }
                //pNum += 1;

                id = Integer.parseInt(dataArray[0]);
                wYear = dataArray[2];
                wMonth = dataArray[3];
                wDay = dataArray[4];
                wHour = dataArray[5];
                ageHour = dataArray[8];
                lat = dataArray[9];
                lon = dataArray[10];
                Height = dataArray[11];
                press = dataArray[12];
                aLine = sDates[id - 1] + wYear + "," + wMonth + "," + wDay + "," + wHour + "," + ageHour + "," + lat + "," + lon + "," + Height + "," + press;
                trajLines[id - 1].add(aLine);
            }

            sr.close();

            for (m = 0; m < trajNum; m++) {
                for (n = 0; n < trajLines[m].size(); n++) {
                    bw.write(trajLines[m].get(n));
                    bw.newLine();
                }
            }
        }

        bw.close();
    }

    /**
     * Convert trajectory endpoint files to TGS files
     *
     * @param trajConfig Trajectory configure
     * @throws IOException
     */
    public static void trajToTGS(TrajConfig trajConfig) throws IOException {
        int dayNum = trajConfig.getDayNum();
        int hourNum = trajConfig.getStartHoursNum();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        for (int i = 0; i < dayNum; i++) {
            trajConfig.upateStartTime(i, 0);
            String tgsfn = trajConfig.getOutPath() + format.format(trajConfig.getStartTime())
                    + ".tgs";
            List<String> trajfns = new ArrayList<String>();
            for (int j = 0; j < hourNum; j++) {
                trajConfig.upateStartTime(i, j);
                String trajfn = trajConfig.getOutPath() + trajConfig.getTrajFileName();
                trajfns.add(trajfn);
            }
            trajToTGS(trajfns, tgsfn);
        }
    }

    /**
     * Join TGS files
     *
     * @param trajConfig Trajectory configure
     * @throws IOException
     */
    public static String joinTGSFiles(TrajConfig trajConfig) throws IOException {
        int dayNum = trajConfig.getDayNum();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMM");
        String monthfn = trajConfig.getOutPath() + format.format(trajConfig.getStartTime()) + ".tgs";
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(monthfn)));
        bw.write("start_year,start_month,start_day,start_hour,year,month,day,hour,age_hour,latitude,longitude,height,press");
        bw.newLine();
        format = new SimpleDateFormat("yyyyMMdd");
        for (int i = 0; i < dayNum; i++) {
            trajConfig.upateStartTime(i, 0);
            String tgsfn = trajConfig.getOutPath() + format.format(trajConfig.getStartTime()) + ".tgs";
            if (!new File(tgsfn).exists()) {
                continue;
            }
            BufferedReader sr = new BufferedReader(new FileReader(new File(tgsfn)));
            sr.readLine();
            String aLine = sr.readLine();
            while (aLine != null) {
                if (aLine.isEmpty()) {
                    aLine = sr.readLine();
                }
                bw.write(aLine);
                bw.newLine();
                aLine = sr.readLine();
            }
            sr.close();
        }
        bw.close();

        return monthfn;
    }

    /**
     * Join TGS files
     *
     * @param tgsfns TGS files
     * @param joinedfn Joined file name
     * @throws IOException
     */
    public static void joinTGSFiles(List<String> tgsfns, String joinedfn) throws IOException {

        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(joinedfn)));
        bw.write("start_year,start_month,start_day,start_hour,year,month,day,hour,age_hour,latitude,longitude,height,press");
        bw.newLine();
        for (int i = 0; i < tgsfns.size(); i++) {
            String tgsfn = tgsfns.get(i);
            if (!new File(tgsfn).exists()) {
                continue;
            }
            BufferedReader sr = new BufferedReader(new FileReader(new File(tgsfn)));
            sr.readLine();
            String aLine = sr.readLine();
            while (aLine != null) {
                if (aLine.isEmpty()) {
                    aLine = sr.readLine();
                }
                bw.write(aLine);
                bw.newLine();
                aLine = sr.readLine();
            }
            sr.close();
        }
        bw.close();
    }

    /**
     * Convert TGS file to shape file
     *
     * @param tgsFile The TGS file
     * @param shpFile The shape file
     * @throws FileNotFoundException
     * @throws IOException
     * @throws Exception
     * @return The vector layer
     */
    public static VectorLayer convertToShapeFile(String tgsFile, String shpFile) throws FileNotFoundException, IOException, Exception {
        Date sDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(sDate);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH");
        String aLine;
        String sMonth;
        String sDay;
        String sHour;
        float height = 0.0f;

        BufferedReader sr = new BufferedReader(new FileReader(new File(tgsFile)));

        sr.readLine();

        VectorLayer aLayer = new VectorLayer(ShapeTypes.PolylineZ);
        aLayer.editAddField("ID", DataTypes.Integer);
        aLayer.editAddField("Date", DataTypes.Date);
        aLayer.editAddField("Year", DataTypes.Integer);
        aLayer.editAddField("Month", DataTypes.Integer);
        aLayer.editAddField("Day", DataTypes.Integer);
        aLayer.editAddField("Hour", DataTypes.Integer);
        aLayer.editAddField("Height", DataTypes.Float);

        int i = 0;
        List<PointZ> pList = new ArrayList<PointZ>();
        while (true) {
            aLine = sr.readLine();
            if (aLine == null) {
                break;
            }
            String[] lineArray = aLine.split(",");
            if (lineArray.length < 13) {
                continue;
            }
            String sYear = lineArray[0];
            if (Integer.parseInt(sYear) < 40) {
                sYear = "20" + sYear;
            } else {
                sYear = "19" + sYear;
            }

            String ageHour = lineArray[8];
            float lat = Float.parseFloat(lineArray[9]);
            float lon = Float.parseFloat(lineArray[10]);
            float alt = Float.parseFloat(lineArray[11]);
            float press = Float.parseFloat(lineArray[12]);

            if (ageHour.equals("0.0")) {
                if (i > 0 && pList.size() > 1) {
                    PolylineZShape aPolylineZ = new PolylineZShape();
                    aPolylineZ.setPoints(pList);
                    aPolylineZ.value = 0;
                    aPolylineZ.setExtent(MIMath.getPointsExtent(pList));
                    int shapeNum = aLayer.getShapeNum();
                    if (aLayer.editInsertShape(aPolylineZ, shapeNum)) {
                        aLayer.editCellValue("ID", shapeNum, shapeNum + 1);
                        aLayer.editCellValue("Date", shapeNum, sDate);
                        aLayer.editCellValue("Year", shapeNum, cal.get(Calendar.YEAR));
                        aLayer.editCellValue("Month", shapeNum, cal.get(Calendar.MONTH) + 1);
                        aLayer.editCellValue("Day", shapeNum, cal.get(Calendar.DAY_OF_MONTH));
                        aLayer.editCellValue("Hour", shapeNum, cal.get(Calendar.HOUR_OF_DAY));
                        aLayer.editCellValue("Height", shapeNum, height);
                    }
                }
                sMonth = lineArray[1];
                sDay = lineArray[2];
                sHour = lineArray[3];
                height = Float.parseFloat(lineArray[11]);
                sDate = format.parse(sYear + "-" + sMonth + "-" + sDay + " " + sHour);
                cal.setTime(sDate);
                //aDateStr = sDate.ToString("yyyyMMddHH");
                pList = new ArrayList<PointZ>();
            }
            PointZ aPoint = new PointZ();
            aPoint.X = lon;
            aPoint.Y = lat;
            aPoint.Z = press;
            aPoint.M = alt;
            if (pList.size() > 1) {
                PointZ oldPoint = pList.get(pList.size() - 1);
                if (Math.abs(aPoint.X - oldPoint.X) > 100) {
                    if (aPoint.X > oldPoint.X) {
                        aPoint.X -= 360;
                    } else {
                        aPoint.X += 360;
                    }
                }
            }
            pList.add(aPoint);

            i += 1;
        }
        sr.close();

        if (i > 1 && pList.size() > 0) {
            PolylineZShape aPolylineZ = new PolylineZShape();
            aPolylineZ.setPoints(pList);
            aPolylineZ.value = 0;
            aPolylineZ.setExtent(MIMath.getPointsExtent(pList));
            int shapeNum = aLayer.getShapeNum();
            if (aLayer.editInsertShape(aPolylineZ, shapeNum)) {
                aLayer.editCellValue("ID", shapeNum, shapeNum + 1);
                aLayer.editCellValue("Date", shapeNum, sDate);
                aLayer.editCellValue("Year", shapeNum, cal.get(Calendar.YEAR));
                aLayer.editCellValue("Month", shapeNum, cal.get(Calendar.MONTH) + 1);
                aLayer.editCellValue("Day", shapeNum, cal.get(Calendar.DAY_OF_MONTH));
                aLayer.editCellValue("Hour", shapeNum, cal.get(Calendar.HOUR_OF_DAY));
                aLayer.editCellValue("Height", shapeNum, height);
            }
        }

        if (aLayer.getShapeNum() > 0) {
            aLayer.setLayerName(new File(shpFile).getName());
            LegendScheme aLS = LegendManage.createSingleSymbolLegendScheme(ShapeTypes.Polyline, Color.black, 1.0f);
            aLS.setFieldName("Year");
            aLayer.setLegendScheme(aLS);
            aLayer.setLayerDrawType(LayerDrawType.TrajLine);
            aLayer.setFileName(shpFile);
            aLayer.saveFile(shpFile);
            return aLayer;
        } else {
            //JOptionPane.showMessageDialog(null, "No valid shapes created.");
            return null;
        }
    }

    /**
     * Remove trajectory intermediate files
     *
     * @param trajConfig Trajectory configure
     */
    public static void removeTrajFiles(TrajConfig trajConfig) {
        int dayNum = trajConfig.getDayNum();
        int hourNum = trajConfig.getStartHoursNum();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        for (int i = 0; i < dayNum; i++) {
            trajConfig.upateStartTime(i, 0);
            String tgsfn = trajConfig.getOutPath() + format.format(trajConfig.getStartTime()) + ".tgs";
            File tgsf = new File(tgsfn);
            if (tgsf.exists()) {
                tgsf.delete();
            }
            for (int j = 0; j < hourNum; j++) {
                trajConfig.upateStartTime(i, j);
                String trajfn = trajConfig.getOutPath() + trajConfig.getTrajFileName();
                File trajf = new File(trajfn);
                if (trajf.exists()) {
                    trajf.delete();
                }
            }
        }
    }
}
