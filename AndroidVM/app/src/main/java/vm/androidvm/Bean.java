package vm.androidvm;

/**
 * Created by wuyongbo on 17-4-6.
 */

public class Bean {

    public int value;

    static {
        System.out.println("androidvm: Bean static code");
    }

//    public Bean() {
//        System.out.println("Class object initialization xx " + getClass().getName());
//    }
//
//    public Bean(int value) {
//        this.value = value;
//    }
}
