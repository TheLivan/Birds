package com.thelivan.birds.util;

/**
 * Immutable 3D vector.
 * <p>
 * 1.7.10 ships {@link net.minecraft.util.Vec3}, but it is mutable, has a protected constructor and lacks most of the
 * operations the original 1.12.2 sources rely on. This class mirrors the 1.12.2 {@code net.minecraft.util.math.Vec3d}
 * API instead, so the rest of the port stays close to the original code.
 */
public final class Vec3d {

    public static final Vec3d ZERO = new Vec3d(0.0, 0.0, 0.0);

    public final double x;
    public final double y;
    public final double z;

    public Vec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3d add(Vec3d v) {
        return new Vec3d(this.x + v.x, this.y + v.y, this.z + v.z);
    }

    public Vec3d add(double dx, double dy, double dz) {
        return new Vec3d(this.x + dx, this.y + dy, this.z + dz);
    }

    public Vec3d subtract(Vec3d v) {
        return new Vec3d(this.x - v.x, this.y - v.y, this.z - v.z);
    }

    public Vec3d subtract(double dx, double dy, double dz) {
        return new Vec3d(this.x - dx, this.y - dy, this.z - dz);
    }

    public Vec3d scale(double factor) {
        return new Vec3d(this.x * factor, this.y * factor, this.z * factor);
    }

    /**
     * Returns {@link #ZERO} for a zero-length vector instead of NaN, matching vanilla 1.12.2 behaviour.
     */
    public Vec3d normalize() {
        double len = length();
        return (len < 1.0E-4) ? ZERO : new Vec3d(this.x / len, this.y / len, this.z / len);
    }

    public double dotProduct(Vec3d v) {
        return this.x * v.x + this.y * v.y + this.z * v.z;
    }

    public Vec3d crossProduct(Vec3d v) {
        return new Vec3d(this.y * v.z - this.z * v.y, this.z * v.x - this.x * v.z, this.x * v.y - this.y * v.x);
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public double lengthSquared() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public double distanceTo(Vec3d v) {
        return Math.sqrt(squareDistanceTo(v));
    }

    public double squareDistanceTo(Vec3d v) {
        return squareDistanceTo(v.x, v.y, v.z);
    }

    public double squareDistanceTo(double px, double py, double pz) {
        double dx = px - this.x;
        double dy = py - this.y;
        double dz = pz - this.z;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ")";
    }
}
