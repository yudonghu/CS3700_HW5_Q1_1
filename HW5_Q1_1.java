import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class person implements Callable{
    final int ITEM_NEED_TO_PRODUCE = 100;
    private String name;
    private int itemRemaining;
    private static Lock bufferLock = new ReentrantLock();
    static BlockingQueue itemBuffer = new ArrayBlockingQueue(10);
    static int workingProducerAmount = 0;
    public person(String name){
        this.name = name;
        if(name.contains("Producer")){
            workingProducerAmount++;
        }
        this.itemRemaining = ITEM_NEED_TO_PRODUCE;
    }

    @Override
    public Object call() throws Exception {
        long start = System.currentTimeMillis();
        if(this.name.contains("Producer")){
            while(itemRemaining > 0){
                if(itemBuffer.remainingCapacity()> 0 && bufferLock.tryLock()){
                    try{
                        itemBuffer.add("Item #" +(ITEM_NEED_TO_PRODUCE-itemRemaining) +" from "+ this.name);
                        itemRemaining--;
                        System.out.format(this.name+": Item #%d added to buffer - remaining capacity: %d\n",ITEM_NEED_TO_PRODUCE-itemRemaining,itemBuffer.remainingCapacity());
                    }finally{
                        bufferLock.unlock();
                    }
                }
            }
            workingProducerAmount--;
        }else{//"Consumer"
            int counter = 0;
            Thread.sleep(5);
            while( (workingProducerAmount!=0) || (!itemBuffer.isEmpty()) ){
                if((!itemBuffer.isEmpty()) && bufferLock.tryLock()){
                    try{
                        String item = (String)itemBuffer.take();
                        //itemConsumed++;
                        System.out.format(this.name + ":consumed  %s\n", item);
                        counter++;
                        Thread.sleep(1000);//time to eat a bowl of soup
                    }catch(Exception e){
                        e.printStackTrace();
                    }finally {
                        bufferLock.unlock();
                    }

                }
            }
            System.out.println(this.name +": total items consumed: "+counter);
        }
        return System.currentTimeMillis()-start;
    }
}

public class HW5_Q1_1 {
    public static void main(String[] args){

        final int PRODUCER_AMOUNT = 5;
        final int CONSUMER_AMOUNT = 2;
        ExecutorService pool = Executors.newFixedThreadPool((PRODUCER_AMOUNT+CONSUMER_AMOUNT));
        for(int i = 0;i < PRODUCER_AMOUNT;i++){
            pool.submit(new person(("Producer "+i)));
        }
        Future[] result = new Future[CONSUMER_AMOUNT];
        for(int i = 0;i < CONSUMER_AMOUNT;i++){
            result[i] = pool.submit(new person(("Consumer "+i)));
        }
        pool.shutdown();



        long escapedTime = 0;
        for(int i = 0;i <CONSUMER_AMOUNT;i++ ){
            try {
                long tempLong = (long) result[i].get();
                if(tempLong > escapedTime){
                    escapedTime=tempLong;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Evaluating Implementation, Case : Locks");
        System.out.println("Producer amount: "+PRODUCER_AMOUNT +" & Consumer amount: "+  CONSUMER_AMOUNT+"\nTime used: "+escapedTime+" milli-sec.");

    }
}
