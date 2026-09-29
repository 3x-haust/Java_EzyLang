package io.github._3xhaust.interpreter.runtime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public final class Num {
    private static final double MAX_SAFE = 9007199254740992.0;
    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private Num() {}

    public static boolean isNumber(Object o) {
        return o instanceof Long || o instanceof Double || o instanceof BigInteger;
    }

    private static boolean isIntegral(Object o) {
        return o instanceof Long || o instanceof BigInteger;
    }

    public static Object parse(String text) {
        String s = text.trim();
        if (s.matches("[+-]?\\d+")) return norm(new BigInteger(s));
        double value = Double.parseDouble(s);
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new NumberFormatException("number out of range: " + s);
        }
        return norm(value);
    }

    public static Object canonical(Object o) {
        if (o instanceof Double d && !Double.isInfinite(d) && !Double.isNaN(d) && d == Math.rint(d)) {
            return norm(new BigDecimal(d).toBigInteger());
        }
        if (o instanceof List<?> list) {
            List<Object> result = new java.util.ArrayList<>();
            for (Object item : list) result.add(canonical(item));
            return result;
        }
        return norm(o);
    }

    public static boolean isNaN(Object o) {
        return o instanceof Double d && Double.isNaN(d);
    }

    public static Object norm(Object o) {
        if (o instanceof Integer i) return (long) i;
        if (o instanceof Double d) {
            if (d == Math.rint(d) && Math.abs(d) <= MAX_SAFE) return d.longValue();
            return d;
        }
        if (o instanceof BigInteger b) {
            if (b.compareTo(LONG_MIN) >= 0 && b.compareTo(LONG_MAX) <= 0) return b.longValue();
            return b;
        }
        return o;
    }

    @SuppressWarnings("unchecked")
    public static Object deepNorm(Object o) {
        if (o instanceof ReadOnlyList) return o;
        if (o instanceof List<?> list) {
            List<Object> l = (List<Object>) list;
            for (int i = 0; i < l.size(); i++) l.set(i, deepNorm(l.get(i)));
            return l;
        }
        if (o instanceof Map<?, ?> map) {
            Map<Object, Object> m = (Map<Object, Object>) map;
            m.replaceAll((k, v) -> deepNorm(v));
            return m;
        }
        return norm(o);
    }

    public static double toDouble(Object o) {
        return ((Number) o).doubleValue();
    }

    public static int toInt(Object o) {
        return ((Number) o).intValue();
    }

    private static BigInteger big(Object o) {
        return o instanceof BigInteger b ? b : BigInteger.valueOf((Long) o);
    }

    public static Object add(Object a, Object b) {
        if (a instanceof Long x && b instanceof Long y) {
            long r = x + y;
            if (((x ^ r) & (y ^ r)) < 0) return norm(big(a).add(big(b)));
            return r;
        }
        if (isIntegral(a) && isIntegral(b)) return norm(big(a).add(big(b)));
        return norm(toDouble(a) + toDouble(b));
    }

    public static Object sub(Object a, Object b) {
        if (a instanceof Long x && b instanceof Long y) {
            long r = x - y;
            if (((x ^ y) & (x ^ r)) < 0) return norm(big(a).subtract(big(b)));
            return r;
        }
        if (isIntegral(a) && isIntegral(b)) return norm(big(a).subtract(big(b)));
        return norm(toDouble(a) - toDouble(b));
    }

    public static Object mul(Object a, Object b) {
        if (a instanceof Long x && b instanceof Long y) {
            long hi = Math.multiplyHigh(x, y);
            long lo = x * y;
            if ((hi == 0 && lo >= 0) || (hi == -1 && lo < 0)) return lo;
            return norm(big(a).multiply(big(b)));
        }
        if (isIntegral(a) && isIntegral(b)) return norm(big(a).multiply(big(b)));
        return norm(toDouble(a) * toDouble(b));
    }

    public static Object div(Object a, Object b) {
        if (isIntegral(a) && isIntegral(b) && big(b).signum() != 0) {
            BigInteger[] qr = big(a).divideAndRemainder(big(b));
            if (qr[1].signum() == 0) return norm(qr[0]);
        }
        return norm(toDouble(a) / toDouble(b));
    }

    public static Object mod(Object a, Object b) {
        if (isIntegral(a) && isIntegral(b) && big(b).signum() != 0) {
            return norm(big(a).remainder(big(b)));
        }
        return norm(toDouble(a) % toDouble(b));
    }

    public static Object negate(Object a) {
        if (a instanceof Long x && x != Long.MIN_VALUE) return -x;
        if (isIntegral(a)) return norm(big(a).negate());
        return -toDouble(a);
    }

    public static boolean isZero(Object a) {
        return isIntegral(a) ? big(a).signum() == 0 : toDouble(a) == 0;
    }

    public static int compare(Object a, Object b) {
        if (a instanceof Long x && b instanceof Long y) return Long.compare(x, y);
        if (isIntegral(a) && isIntegral(b)) return big(a).compareTo(big(b));
        double x = toDouble(a);
        double y = toDouble(b);
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isInfinite(x) || Double.isInfinite(y)) return Double.compare(x, y);
        return decimal(a).compareTo(decimal(b));
    }

    private static BigDecimal decimal(Object o) {
        if (o instanceof Long l) return BigDecimal.valueOf(l);
        if (o instanceof BigInteger b) return new BigDecimal(b);
        return new BigDecimal((Double) o);
    }

    public static boolean equal(Object a, Object b) {
        if (a instanceof Double x && Double.isNaN(x)) return false;
        if (b instanceof Double y && Double.isNaN(y)) return false;
        return compare(a, b) == 0;
    }

    public static String format(Object o) {
        if (o instanceof Double d) {
            if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e21) {
                return new BigDecimal(d).toPlainString();
            }
            return String.valueOf(d);
        }
        return String.valueOf(o);
    }
}
