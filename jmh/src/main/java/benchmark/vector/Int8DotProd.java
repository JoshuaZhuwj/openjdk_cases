package benchmark.vector;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;
import static jdk.incubator.vector.VectorOperators.*;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
public class Int8DotProd {
    @Param({"1024"})
    int SIZE;

    byte[] a;
    byte[] b;

    @Setup(Level.Trial)
    public void setup() {
        Random rd = new Random();
        a = new byte[SIZE];
        b = new byte[SIZE];
        rd.nextBytes(a);
        rd.nextBytes(b);
    }

    @Benchmark
    public int dotProduct_scalar() {
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result += a[i] * b[i];
        }
        return result;
    }

    @Benchmark
    public int dotProduct_vec() {
        int i = 0, result = 0;
        int upperBound = ByteVector.SPECIES_64.loopBound(a.length - ByteVector.SPECIES_64.length());
        IntVector acc = IntVector.zero(IntVector.SPECIES_128);

        // 4 bytes at a time (re-loading half the vector each time!)
        for (; i < upperBound; i += ByteVector.SPECIES_64.length() >> 1) {
            // load 8 bytes
            ByteVector va8 = ByteVector.fromArray(ByteVector.SPECIES_64, a, i);
            ByteVector vb8 = ByteVector.fromArray(ByteVector.SPECIES_64, b, i);

            // process first "half" only: 16-bit multiply
            Vector<Short> va16 = va8.convert(B2S, 0); // B2S Byte2Short
            Vector<Short> vb16 = vb8.convert(B2S, 0);
            Vector<Short> prod16 = va16.mul(vb16);

            // 32-bit add - S2I Short2Int
            acc = acc.add(prod16.convertShape(S2I, IntVector.SPECIES_128, 0));
        }
        // reduce
        result = acc.reduceLanes(ADD);

        // tail loop
        for (; i < a.length; i++) {
            result += a[i] * b[i];
        }
        return result;
    }

    @Benchmark
    public int dotProduct_vec_new() {
        int i = 0, result = 0;
        int upperBound = ByteVector.SPECIES_128.loopBound(a.length);
        IntVector acc = IntVector.zero(IntVector.SPECIES_128);

        for (; i < upperBound; i += ByteVector.SPECIES_128.length()) {
            ByteVector va8 = ByteVector.fromArray(ByteVector.SPECIES_128, a, i);
            ByteVector vb8 = ByteVector.fromArray(ByteVector.SPECIES_128, b, i);
            acc = va8.fma(vb8, acc);
        }
        // reduce
        result = acc.reduceLanes(ADD);

        // tail loop
        for (; i < a.length; i++) {
            result += a[i] * b[i];
        }
        return result;
    }

}

