package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Thread.*;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    private volatile boolean penaltied, scored, sleep, still;
    public volatile List<Integer> myList;


    private Dealer dealer;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.myList = new ArrayList<>(3);
        this.dealer = dealer;

    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            if( myList.size() == 3 && !penaltied & !sleep){
                synchronized (this) {
                    try {
                        sendSetToDealer();
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                        if (penaltied) {
                            still = true;
                            penalty();

                        } else if (scored) {
                            point();
                        }
                        if( !human ) {
                                sleep = true;
                                aiThread.interrupt();
                        }

            }
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate ) {
                Random rnd = new Random();
                int rndRow = rnd.nextInt(env.config.rows);
                int rndColumn = rnd.nextInt(env.config.columns);
                int randomSlot = rndColumn + (env.config.columns * rndRow);
                keyPressed(randomSlot);
                    if (myList.size() == 3 && !penaltied) {
                        synchronized (this) {
                            try {
                                sleep = false;
                                wait();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */

    public void keyPressed(int slot) {

        if( !scored && !still) {
            if (myList.contains(slot)) {
                table.removeToken(id, slot);
                myList.remove((Integer) slot);
                penaltied = false;
            } else if (myList.size() != 3) {
                table.placeToken(id, slot);
                myList.add(slot);
//                    if( !table.slotIsNull ) {
//                        myList.add(slot);
//                    }else {
//                        table.slotIsNull = false;
//                    }

            }

        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        long i =  env.config.pointFreezeMillis;
        while ( i >= 0) {
            env.ui.setFreeze(id, i);
            try{
                if (i < 1000) {
                    sleep(i);
                } else {
                    sleep(1000);
                }
            } catch (InterruptedException e) {}
            i -= 1000;
        }
        env.ui.setFreeze(id, i);
        scored = false;
    }

    public void pointPlayer(){
        scored = true;
    }
    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        long i =  env.config.penaltyFreezeMillis;
        while ( i >= 0) {
            env.ui.setFreeze(id, i);
            try{
            if (i < 1000) {
                    sleep(i);
            } else {
                sleep(1000);
            }
            } catch (InterruptedException e) {}
            i -= 1000;
        }
        still = false;
        env.ui.setFreeze(id, i);
    }

    public void penalizePlayer(){
        penaltied = true;
    }
    public int score() {
        return score;
    }

    public void sendSetToDealer(){
        synchronized (myList) {
            int[] arr = new int[myList.size() + 1];
            for (int i = 0; i < arr.length - 1; i++) {
                arr[i] = myList.get(i);
            }

            arr[arr.length - 1] = id;
            dealer.addSetToTest(arr);
        }

    }

    public State threadState(){
        return playerThread.getState();
    }

}

