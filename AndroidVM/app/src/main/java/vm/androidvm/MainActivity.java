package vm.androidvm;

import android.app.Activity;
import android.os.Bundle;


public class MainActivity extends Activity {

    //int[] global_memory = new int[1024*1024*50];
    Bean[] global_beans = new Bean[1024];

    /** Rule_1: 以下两种情况都会导致Bean对象呗初始化，应该延迟初始化 */
    static Bean static_bean;    // = new Bean();
    Bean non_static_bean;       // = new Bean();

    static int global_value = 1;

    static void show_variable() {
        int static_method_v = 2;
    }


    void showBeanValue(int index) {
        global_beans[index] = new Bean();
        int value = global_beans[index].value;
        System.out.println("androidvm: showBeanValue " + value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ClassLoader classLoader = getClassLoader();
        try {
//            Class cl = classLoader.loadClass("vm.androidvm.Bean");     /** 只"加载"类的字节码，不调用<clinit>方法 */
//            Constructor constructor = cl.getDeclaredConstructor();
//            Object instance = constructor.newInstance();            /** 导致调用<clinit>方法，输出"androidvm: Bean static code" */

        } catch (Exception e) {
            e.printStackTrace();
        }

//        non_static_bean = new Bean();
//        showBeanValue(0);

//        System.out.println("androidvm: global_memory size=" + global_memory.length * 4 / 1024 / 1024 + "M");
//        global_memory[1] = 100;
    }
}
