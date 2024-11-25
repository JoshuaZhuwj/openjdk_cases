package benchmark.vector;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;
import static jdk.incubator.vector.VectorOperators.*;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-Djdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK=0"})
public class Int7uDotProd {
    @Param({"1024"})
    int SIZE;

    byte[] a;
    byte[] b;

    private byte generateRandomByte(byte min, byte max) {
        Random random = new Random();
        int range = max - min + 1;
        int randomInt = random.nextInt(range);
        return (byte) (min + randomInt);
    }

    @Setup(Level.Trial)
    public void int7u_setup() {
        a = new byte[SIZE];
        b = new byte[SIZE];
        for (int i = 0; i < a.length; i++) {
            a[i] = generateRandomByte((byte)0, (byte)127);
        }
        for (int i = 0; i < b.length; i++) {
            b[i] = generateRandomByte((byte)0, (byte)127);
        }
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
    public int dotProduct_vec128_lucene() {
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
    public int dotProduct_vec128_lucene_with_manual_loop_unrolling() {
        int i = 0, result = 0;
        int upperBound = ByteVector.SPECIES_64.loopBound(a.length);
        IntVector acc = IntVector.zero(IntVector.SPECIES_128);
 
        for (; i < upperBound; i += ByteVector.SPECIES_64.length()) {
            // load 8 bytes
            ByteVector va8 = ByteVector.fromArray(ByteVector.SPECIES_64, a, i);
            ByteVector vb8 = ByteVector.fromArray(ByteVector.SPECIES_64, b, i);
 
            Vector<Short> va16 = va8.convert(B2S, 0); // B2S Byte2Short
            Vector<Short> vb16 = vb8.convert(B2S, 0);
            Vector<Short> prod16_1 = va16.mul(vb16);

            Vector<Short> vc16 = va8.convert(B2S, 1); // B2S Byte2Short
            Vector<Short> vd16 = vb8.convert(B2S, 1);
            Vector<Short> prod16_2 = vc16.mul(vd16);
 
            // 32-bit add - S2I Short2Int
            acc = acc.add(prod16_1.convertShape(S2I, IntVector.SPECIES_128, 0));
            acc = acc.add(prod16_2.convertShape(S2I, IntVector.SPECIES_128, 0));
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
    public int dotProduct_vec256_lucene() {
        int i = 0, result = 0;
        int upperBound = ByteVector.SPECIES_64.loopBound(a.length);
        IntVector acc = IntVector.zero(IntVector.SPECIES_256);

        for (; i < upperBound; i += ByteVector.SPECIES_64.length()) {
            ByteVector va8 = ByteVector.fromArray(ByteVector.SPECIES_64, a, i);
            ByteVector vb8 = ByteVector.fromArray(ByteVector.SPECIES_64, b, i);

            // 32-bit multiply and add into accumulator
            Vector<Integer> va32 = va8.convertShape(B2I, IntVector.SPECIES_256, 0);
            Vector<Integer> vb32 = vb8.convertShape(B2I, IntVector.SPECIES_256, 0);
            acc = acc.add(va32.mul(vb32));

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
    public int dotProduct_vec512_lucene() {
        int i = 0, result = 0;
        int upperBound = ByteVector.SPECIES_128.loopBound(a.length);
        IntVector acc = IntVector.zero(IntVector.SPECIES_512);

        for (; i < upperBound; i += ByteVector.SPECIES_128.length()) {
            ByteVector va8 = ByteVector.fromArray(ByteVector.SPECIES_128, a, i);
            ByteVector vb8 = ByteVector.fromArray(ByteVector.SPECIES_128, b, i);

          // 16-bit multiply: avoid AVX-512 heavy multiply on zmm
            Vector<Short> va16 = va8.convertShape(B2S, ShortVector.SPECIES_256, 0);
            Vector<Short> vb16 = vb8.convertShape(B2S, ShortVector.SPECIES_256, 0);
            Vector<Short> prod16 = va16.mul(vb16);
            // 32-bit add
            Vector<Integer> prod32 = prod16.convertShape(S2I, IntVector.SPECIES_512, 0);
            acc = acc.add(prod32);
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
    public int dotProduct_vec128_ajdk() {
        int i = 0, result = 0;
        int upperBound = ByteVector.SPECIES_128.loopBound(a.length);
        IntVector acc = IntVector.zero(IntVector.SPECIES_128);
 
        for (; i < upperBound; i += ByteVector.SPECIES_128.length()) {
            ByteVector va8 = ByteVector.fromArray(ByteVector.SPECIES_128, a, i);
            ByteVector vb8 = ByteVector.fromArray(ByteVector.SPECIES_128, b, i);
            acc = va8.fma7u(vb8, acc);
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
    public int dotProduct_vec256_ajdk() {
        int i = 0, result = 0;
        int upperBound = ByteVector.SPECIES_256.loopBound(a.length);
        IntVector acc = IntVector.zero(IntVector.SPECIES_256);

        for (; i < upperBound; i += ByteVector.SPECIES_256.length()) {
            ByteVector va8 = ByteVector.fromArray(ByteVector.SPECIES_256, a, i);
            ByteVector vb8 = ByteVector.fromArray(ByteVector.SPECIES_256, b, i);
            acc = va8.fma7u(vb8, acc);
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
    public int dotProduct_vec512_ajdk() {
        int i = 0, result = 0;
        int upperBound = ByteVector.SPECIES_512.loopBound(a.length);
        IntVector acc = IntVector.zero(IntVector.SPECIES_512);

        for (; i < upperBound; i += ByteVector.SPECIES_512.length()) {
            ByteVector va8 = ByteVector.fromArray(ByteVector.SPECIES_512, a, i);
            ByteVector vb8 = ByteVector.fromArray(ByteVector.SPECIES_512, b, i);
            acc = va8.fma7u(vb8, acc);
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

