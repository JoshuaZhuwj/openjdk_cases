package org.openjdk.bench.jdk.incubator.vector;

import java.util.Random;
import jdk.incubator.vector.*;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
public class FloatVectorDotProd {
    @Param({"1024"})
    static int SIZE;

    static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
    static final VectorSpecies<Float> SPECIES_256 = FloatVector.SPECIES_256;

    private float[] a;

    private float[] b;

    @Setup(Level.Trial)
    public void BmSetup() {
        Random r = new Random();
        a = new float[SIZE];
        b = new float[SIZE];
        for(int i = 0; i < SIZE; i++) {
            a[i] = r.nextFloat();
            b[i] = r.nextFloat();
        }
    }

    @Benchmark
    public float dotProduct_scalar() {
        float result = 0.0f;
        for (int i = 0; i < a.length; i++) {
            result += a[i] * b[i];
        }
        return result;
    }


    @Benchmark
    public float dotProduct_vec_256() {
        int i = 0;
        float result = 0.0f;
        FloatVector acc = FloatVector.zero(SPECIES_256);
        int upperBound = SPECIES_256.loopBound(a.length);
        for (; i < upperBound; i += SPECIES_256.length()) {
            FloatVector va = FloatVector.fromArray(SPECIES_256, a, i);
            FloatVector vb = FloatVector.fromArray(SPECIES_256, b, i);
            acc = acc.add(va.mul(vb));
        }
        // reduction
        result = acc.reduceLanes(VectorOperators.ADD);
        // tail loop
        for (; i < a.length; i++) {
            result += b[i] * a[i];
        }
        return result;
    }

    @Benchmark
    public float dotProduct_vec_512() {
        int i = 0;
        float result = 0.0f;
        FloatVector acc = FloatVector.zero(SPECIES);
        int upperBound = SPECIES.loopBound(a.length);
        for (; i < upperBound; i += SPECIES.length()) {
            FloatVector va = FloatVector.fromArray(SPECIES, a, i);
            FloatVector vb = FloatVector.fromArray(SPECIES, b, i);
            acc = acc.add(va.mul(vb));
        }
        // reduction
        result = acc.reduceLanes(VectorOperators.ADD);
        // tail loop
        for (; i < a.length; i++) {
            result += b[i] * a[i];
        }
        return result;
    }
}
