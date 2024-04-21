package com.example.mango;

import android.provider.ContactsContract;

public class PosTrans {
    private DataLib dataLib;

    public PosTrans(DataLib dataLib) {
        this.dataLib = dataLib;
    }

    public double[] ENU2WGS84(double[] ENU) {
        double a = 6378137;
        double b = 6356752.3142;
        double f = (a - b) / a;
        double e_sq = f * (2 - f);
        double pi = 3.14159265359;

        double lat0 = dataLib.initWGS84Latitude;
        double lon0 = dataLib.initWGS84Longitude;
        double h0 = dataLib.initWGS84Altitude;

        double lamb = pi / 180 * (lat0);
        double phi = pi / 180 * (lon0);
        double s = Math.sin(lamb);
        double N = a / Math.sqrt(1 - e_sq * s * s);

        double sin_lambda = Math.sin(lamb);
        double cos_lambda = Math.cos(lamb);
        double sin_phi = Math.sin(phi);
        double cos_phi = Math.cos(phi);

        double x0 = (h0 + N) * cos_lambda * cos_phi;
        double y0 = (h0 + N) * cos_lambda * sin_phi;
        double z0 = (h0 + (1 - e_sq) * N) * sin_lambda;

        double xEast = ENU[0];
        double yNorth = ENU[1];
        double zUp = ENU[2];

        double t = cos_lambda * zUp - sin_lambda * yNorth;

        double zd = sin_lambda * zUp + cos_lambda * yNorth;
        double xd = cos_phi * t - sin_phi * xEast;
        double yd = sin_phi * t + cos_phi * xEast;

        double x = xd + x0;
        double y = yd + y0;
        double z = zd + z0;

        double x2 = x * x;
        double y2 = y * y;
        double z2 = z * z;

        double e = Math.sqrt(1 - (b / a) * (b / a));
        double b2 = b * b;
        double e2 = e * e;
        double ep = e * (a / b);
        double r = Math.sqrt(x2 + y2);
        double r2 = r * r;
        double E2 = a * a - b * b;
        double F = 54 * b2 * z2;
        double G = r2 + (1 - e2) * z2 - e2 * E2;
        double c = (e2 * e2 * F * r2) / (G * G * G);
        s = 1 + c + Math.pow(Math.sqrt(c * c + 2 * c), 1.0 / 3.0);
        double P = F / (3 * (s + 1 / s + 1) * (s + 1 / s + 1) * G * G);
        double Q = Math.sqrt(1 + 2 * e2 * e2 * P);
        double ro = -(P * e2 * r) / (1 + Q) + Math.sqrt((a * a / 2) * (1 + 1 / Q) - (P * (1 - e2) * z2) / (Q * (1 + Q)) - P * r2 / 2);
        double tmp = (r - e2 * ro) * (r - e2 * ro);
        double U = Math.sqrt(tmp + z2);
        double V = Math.sqrt(tmp + (1 - e2) * z2);
        double zo = (b2 * z) / (a * V);

        double height = U * (1 - b2 / (a * V));

        double lat = Math.atan((z + ep * ep * zo) / r);
        double lon;
        double temp = Math.atan(y / x);
        if (x >= 0) {
            lon = temp;
        } else {
            if (x < 0 && y >= 0) {
                lon = pi + temp;
            } else {
                lon = temp - pi;
            }
        }

        lat0 = lat / (pi / 180);
        lon0 = lon / (pi / 180);
        h0 = height;

        return new double[]{lat0, lon0, h0};
    }

    public double[] WGS842ENU(double[] WGS84) {
        double a = 6378137;
        double b = 6356752.3142;
        double pi = 3.14159265358979;
        double f = (a - b) / a;
        double e_sq = f * (2 - f);

        double lat = WGS84[0];
        double lon = WGS84[1];
        double h = WGS84[2];

        double lamb = lat / 180.0 * pi;
        double phi = lon / 180.0 * pi;
        double s = Math.sin(lamb);
        double N = a / Math.sqrt(1 - e_sq * s * s);

        double sin_lambda = Math.sin(lamb);
        double cos_lambda = Math.cos(lamb);
        double sin_phi = Math.sin(phi);
        double cos_phi = Math.cos(phi);

        double x = (h + N) * cos_lambda * cos_phi;
        double y = (h + N) * cos_lambda * sin_phi;
        double z = (h + (1 - e_sq) * N) * sin_lambda;

        double lat0 = dataLib.initWGS84Latitude;
        double lon0 = dataLib.initWGS84Longitude;
        double h0 = dataLib.initWGS84Altitude;

        double lamb0 = lat0 / 180.0 * pi;
        double phi0 = lon0 / 180.0 * pi;
        double s0 = Math.sin(lamb0);
        double N0 = a / Math.sqrt(1 - e_sq * s0 * s0);

        double sin_lambda0 = Math.sin(lamb0);
        double cos_lambda0 = Math.cos(lamb0);
        double sin_phi0 = Math.sin(phi0);
        double cos_phi0 = Math.cos(phi0);

        double x0 = (h0 + N0) * cos_lambda0 * cos_phi0;
        double y0 = (h0 + N0) * cos_lambda0 * sin_phi0;
        double z0 = (h0 + (1 - e_sq) * N0) * sin_lambda0;

        double xd = x - x0;
        double yd = y - y0;
        double zd = z - z0;

        double t = -cos_phi0 * xd - sin_phi0 * yd;

        double xEast = -sin_phi0 * xd + cos_phi0 * yd;
        double yNorth = t * sin_lambda0 + cos_lambda0 * zd;
        double zUp = cos_lambda0 * cos_phi0 * xd + cos_lambda0 * sin_phi0 * yd + sin_lambda0 * zd;

        return new double[]{xEast, yNorth, zUp};
    }

    public double[] WGS842GCJ02(double[] WGS84) {
        JZLocationConverter.LatLng wgs84 = new JZLocationConverter.LatLng();
        wgs84.setLatitude(WGS84[0]);
        wgs84.setLongitude(WGS84[1]);
        JZLocationConverter.LatLng gcj02 = JZLocationConverter.wgs84ToGcj02(wgs84);
        return new double[]{gcj02.getLatitude(), gcj02.getLongitude(), WGS84[2]};
    }

    public double[] GCJ022WGS84(double[] GCJ02) {
        JZLocationConverter.LatLng gcj02 = new JZLocationConverter.LatLng();
        gcj02.setLatitude(GCJ02[0]);
        gcj02.setLongitude(GCJ02[1]);
        JZLocationConverter.LatLng wgs84 = JZLocationConverter.gcj02ToWgs84(gcj02);
        return new double[]{wgs84.getLatitude(), wgs84.getLongitude(), GCJ02[2]};
    }
}
