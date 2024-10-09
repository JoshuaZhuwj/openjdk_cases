import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

class Inner {}

class InnerFloat extends Inner {
    float data;
    public InnerFloat(float f) {
        data = f;
    }
}

class InnerDouble extends Inner {
    double data;
    public InnerDouble(double f) {
        data = f;
    }
}

class Outer {
    Inner field;
    public Outer(Inner i) {
        field = i;
    }
}

public class TestZGCSpillingAtLoadBarrierStub {

    float test_float(Outer outer, float f) {
        Inner inner = outer.field;
        return f + ((InnerFloat)inner).data;
    }

    double test_double(Outer outer, double d) {
        Inner inner = outer.field;
        return d + ((InnerDouble)inner).data;
    }

    void test_float_vector_128(float[] b, Outer[] array) {
        VectorSpecies<Float> float_species = FloatVector.SPECIES_128;

        FloatVector av = FloatVector.zero(float_species);
        for (int i = 0; i < b.length; i += float_species.length()) {
            FloatVector bv = FloatVector.fromArray(float_species, b, i);
            Inner inner = array[RANDOM.nextInt(NUM)].field;
            float value = ((InnerFloat)inner).data;
            av = av.add(bv).add(value);
        }
    }

    void test_float_vector_PREFERRED(float[] b, Outer[] array) {
        VectorSpecies<Float> float_species = FloatVector.SPECIES_PREFERRED;

        FloatVector av = FloatVector.zero(float_species);
        for (int i = 0; i < b.length; i += float_species.length()) {
            FloatVector bv = FloatVector.fromArray(float_species, b, i);
            Inner inner = array[RANDOM.nextInt(NUM)].field;
            float value = ((InnerFloat)inner).data;
            av = av.add(bv).add(value);
        }
    }

    ////////////////////////////////////////////////////////////

    private final static int NUM = 1024;
    private final static int ITERATIONS = 20_000;
    private final static RandomGenerator RANDOM = RandomGeneratorFactory.getDefault().create(0);

    Outer[] generateOuterArray(Inner[] inners) {
        Outer[] outers = new Outer[NUM];
        for (int i = 0; i < NUM; i++) {
            outers[i] = new Outer(inners[i]);
        }
        return outers;
    }

    public void test() {
        TestZGCSpillingAtLoadBarrierStub t = new TestZGCSpillingAtLoadBarrierStub();

        ////////////////////////////////////////////////////////

        float[] f_array = new float[NUM];
        InnerFloat[] f_inners = new InnerFloat[NUM];
        InnerDouble[] d_inners = new InnerDouble[NUM];

        for (int i = 0; i < NUM; i++) {
            f_array[i] = RANDOM.nextFloat();
            f_inners[i] = new InnerFloat(RANDOM.nextFloat());
            d_inners[i] = new InnerDouble(RANDOM.nextDouble());
        }

        Outer[] f_outers = generateOuterArray(f_inners);
        Outer[] d_outers = generateOuterArray(d_inners);

        ////////////////////////////////////////////////////////

        for (int i = 0; i < ITERATIONS; i++) {
            t.test_float(f_outers[RANDOM.nextInt(NUM)], RANDOM.nextFloat());
            t.test_double(d_outers[RANDOM.nextInt(NUM)], RANDOM.nextDouble());
            t.test_float_vector_128(f_array, f_outers);
            t.test_float_vector_PREFERRED(f_array, f_outers);
        }
    }
}

