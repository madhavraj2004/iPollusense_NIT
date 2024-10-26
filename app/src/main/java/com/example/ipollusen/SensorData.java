package com.example.ipollusen;

public class SensorData {
    private double temperature;
    private double humidity;
    private int no2;
    private int c2h5oh;
    private int voc;
    private int co;
    private int pm1;
    private int pm2_5;
    private int pm10;

    // Constructor
    public SensorData(double temperature, double humidity, int no2, int c2h5oh, int voc, int co, int pm1, int pm2_5, int pm10) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.no2 = no2;
        this.c2h5oh = c2h5oh;
        this.voc = voc;
        this.co = co;
        this.pm1 = pm1;
        this.pm2_5 = pm2_5;
        this.pm10 = pm10;
    }

    // Getters and setters
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public int getNo2() { return no2; }
    public void setNo2(int no2) { this.no2 = no2; }

    public int getC2h5oh() { return c2h5oh; }
    public void setC2h5oh(int c2h5oh) { this.c2h5oh = c2h5oh; }

    public int getVoc() { return voc; }
    public void setVoc(int voc) { this.voc = voc; }

    public int getCo() { return co; }
    public void setCo(int co) { this.co = co; }

    public int getPm1() { return pm1; }
    public void setPm1(int pm1) { this.pm1 = pm1; }

    public int getPm2_5() { return pm2_5; }
    public void setPm2_5(int pm2_5) { this.pm2_5 = pm2_5; }

    public int getPm10() { return pm10; }
    public void setPm10(int pm10) { this.pm10 = pm10; }
}
