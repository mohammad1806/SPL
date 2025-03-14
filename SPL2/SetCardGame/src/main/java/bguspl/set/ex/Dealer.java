package bguspl.set.ex;
import bguspl.set.Env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;


    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;


    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;



    private  BlockingQueue<int[]> setsTotest;

    private Thread[] myThreads;
    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        this.table.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        this.setsTotest = new ArrayBlockingQueue<>(players.length);
        this.myThreads = new Thread[players.length];


    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        for (int i = 0 ; i < players.length ; i++) {
            myThreads[i] = new Thread(players[i],"Player " + i);
            myThreads[i].start();
        }
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        while (!shouldFinish()) {
            placeCardsOnTable();
            table.hints();
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        announceWinners();
        for( int i = myThreads.length - 1; i >= 0; i--){
            try {
            players[i].terminate();
            myThreads[i].interrupt();
                myThreads[i].join();
            } catch (InterruptedException ignored) {
            }
        }
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        reshuffleTime=System.currentTimeMillis()+env.config.turnTimeoutMillis;
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            removeCardsFromTable();
            placeCardsOnTable();
            updateTimerDisplay(false);
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    public void removeCardsFromTable() {

            while (!setsTotest.isEmpty()) {
                int[] temp = new int[4];
                for(int i = 0; i < temp.length - 1; i++){
                    if( table.slotToCard[setsTotest.peek()[i]] != null ){
                    temp[i] = table.slotToCard[setsTotest.peek()[i]];
                    }
                }
                int playerId = setsTotest.peek()[3];
                int[] temp2 = setsTotest.remove();
                for( int j = 0; j < temp2.length - 1; j++){
                    if( !table.isToken(playerId,temp2[j])){
                        myThreads[playerId].interrupt();
                        return;
                    }
                }
                if (isLegalSet(temp)) {
                    for (int i = 0; i < temp.length - 1; i++) {
                        table.removeCard(table.cardToSlot[temp[i]]);
                    }
                    updateTimerDisplay(true);
                    players[playerId].pointPlayer();
                    myThreads[playerId].interrupt();
                }else {
                    players[playerId].penalizePlayer();
                    myThreads[playerId].interrupt();
                }
            }
    }


    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */

    private void placeCardsOnTable() {
        List<Integer> availableSlots = IntStream.range(0, table.slotToCard.length)
                .boxed()
                .collect(Collectors.toList());

        Collections.shuffle(availableSlots);
        Collections.shuffle(deck);

        for (int slot : availableSlots) {
            if (deck.isEmpty()) {
                break;
            }

            if (table.slotToCard[slot] == null) {
                table.placeCard(deck.remove(0), slot);
            }
        }
    }
    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        //TODO implement
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        if (reset) {

            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
            env.ui.setCountdown(reshuffleTime - System.currentTimeMillis(), reshuffleTime - System.currentTimeMillis() <= env.config.turnTimeoutWarningMillis);
        } else {
            if(reshuffleTime<=System.currentTimeMillis()) { updateTimerDisplay(true);}
            env.ui.setCountdown( reshuffleTime - System.currentTimeMillis(),
                    reshuffleTime - System.currentTimeMillis()<=env.config.turnTimeoutWarningMillis);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        List<Integer> arrayList = IntStream.range(0, table.slotToCard.length)
                .boxed()
                .collect(Collectors.toList());
        Collections.shuffle(arrayList);
        for(int i = 0 ; i < arrayList.size() ; i++) {
            Integer curslot = table.slotToCard[arrayList.get(i)];
            table.removeCard(arrayList.get(i));
            if(curslot != null) {
                deck.add(curslot);
            }
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        List<Integer> winners = new ArrayList<>();
        int maxScore = players[0].score();
        int winner = 0;
        for( int i = 0; i < players.length; i++){
            if( players[i].score() > maxScore ){
                maxScore = players[i].score();
                winner = i;
            }
        }
        winners.add(winner);
        for( int j = 0; j < players.length; j++){
            if( players[j].score() == maxScore && j != winner){
                winners.add(j);
            }
        }
        int[] array = new int[winners.size()];
        for( int k = 0; k < array.length; k++){
            array[k] = winners.get(k);
        }
        env.ui.announceWinner(array);

    }

    /** function i add*/

    public void addSetToTest(int[] arr){
        try {
                setsTotest.put(arr);
        } catch (InterruptedException e) {}
    }


    public boolean isLegalSet(int[] arr){
        return env.util.testSet(Arrays.copyOfRange(arr,0,arr.length-1));
    }

}