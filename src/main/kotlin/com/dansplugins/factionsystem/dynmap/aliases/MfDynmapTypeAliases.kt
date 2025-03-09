package com.dansplugins.factionsystem.dynmap.aliases

/**
 * Represents a point in a 2D space with x and y coordinates.
 */
typealias Point = Pair<Int, Int>

/**
 * Represents a line segment defined by two points.
 */
typealias LineSegment = Pair<Point, Point>

/**
 * Represents a path as a list of points.
 */
typealias Path = List<Point>