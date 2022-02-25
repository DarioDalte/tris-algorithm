public class  test_thread implements Runnable {

    public test_thread(){

        Thread t1 = new Thread(this);
        t1.setName("Thread 1");

        Thread t2 = new Thread(this);
        t2.setName("Thread 2");

        t1.start();
        t2.start();

    }


    public void run() {
        int i = 0;

        if(i == 0){
            System.out.println(Thread.currentThread().getName());
        }

        while (true) {
            System.out.println(Thread.currentThread().getName() + " ciao" + i++);
            if ( i == 20) { break; }
        }
    }

    public static void main(String[] args) {

       new test_thread();

    }
}
