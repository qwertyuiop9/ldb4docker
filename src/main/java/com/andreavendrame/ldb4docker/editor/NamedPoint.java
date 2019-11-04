package com.andreavendrame.ldb4docker.editor;

import com.andreavendrame.ldb4docker.myjlibbig.ldb.Point;

import java.util.LinkedList;
import java.util.List;

public class NamedPoint {

    private Point point;
    private String name;

    public NamedPoint(Point point, String name) {
        this.point = point;
        this.name = name;
    }

    public Point getPoint() {
        return point;
    }

    public String getName() {
        return name;
    }

    public static List<Point> getPointList(List<NamedPoint> namedPoints) {

        List<Point> points = new LinkedList<>();
        for (NamedPoint namedPoint : namedPoints) {
            points.add(namedPoint.getPoint());
        }

        return points;
    }
}
