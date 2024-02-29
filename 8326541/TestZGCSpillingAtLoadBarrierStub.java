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

    float test_float(Outer[] array, int index) {
        float float_const = (float) index;
        Inner inner = array[index].field;
        return float_const + ((InnerFloat)inner).data;
    }

    double test_double(Outer[] array, int index) {
        double double_const = (double) index;
        Inner inner = array[index].field;
        return double_const + ((InnerDouble)inner).data;
    }

    void test_float_vector_128(float[] b, Outer[] array) {
        VectorSpecies<Float> float_species = FloatVector.SPECIES_128;

        FloatVector av = FloatVector.zero(float_species);
        for (int i = 0; i < b.length; i += float_species.length()) {
            FloatVector bv = FloatVector.fromArray(float_species, b, i);
            Inner inner = array[random.nextInt(NUM)].field;
            float value = ((InnerFloat)inner).data;
            av = av.add(bv).add(value);
        }
    }

    void test_float_vector_PREFERRED(float[] b, Outer[] array) {
        VectorSpecies<Float> float_species = FloatVector.SPECIES_PREFERRED;

        FloatVector av = FloatVector.zero(float_species);
        for (int i = 0; i < b.length; i += float_species.length()) {
            FloatVector bv = FloatVector.fromArray(float_species, b, i);
            Inner inner = array[random.nextInt(NUM)].field;
            float value = ((InnerFloat)inner).data;
            av = av.add(bv).add(value);
        }
    }

    ////////////////////////////////////////////////////////////

    private static final int NUM = 1024;
    private static final RandomGenerator random = RandomGeneratorFactory.getDefault().create(0);

    Outer[] generateOuterArray(Inner[] inners) {
        Outer[] outers = new Outer[NUM];
        for (int i = 0; i < NUM; i++) {
            outers[i] = new Outer(inners[i]);
        }
        return outers;
    }

    public void test() {
        TestZGCSpillingAtLoadBarrierStub t = new TestZGCSpillingAtLoadBarrierStub();

        ////////////////////////////////////////////////

        InnerFloat[]  inners_1 = new InnerFloat[NUM];
        InnerDouble[] inners_2 = new InnerDouble[NUM];
        float[] f_array = new float[NUM];

        for (int i = 0; i < NUM; i++) {
            inners_1[i] = new InnerFloat(random.nextFloat());
            inners_2[i] = new InnerDouble(random.nextDouble());
            f_array[i] = random.nextFloat();
        }

        Outer[] outers_1 = generateOuterArray(inners_1);
        Outer[] outers_2 = generateOuterArray(inners_2);

        ////////////////////////////////////////////////

        for (int i = 0; i < 20000; i++) {
            t.test_float(outers_1, random.nextInt(NUM));
            t.test_double(outers_2, random.nextInt(NUM));
            t.test_float_vector_128(f_array, outers_1);
            t.test_float_vector_PREFERRED(f_array, outers_1);
        }
    } 
}

