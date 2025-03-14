package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environ ment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)


    //holds all the players tokens in the table
    private volatile boolean[][] playersTokens;

    public volatile Player[] players;

    public volatile boolean slotIsNull;

    public volatile Object[] slotsLocks;



    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        this.slotsLocks = new Object[slotToCard.length];
        for( int i = 0; i < slotToCard.length; i++){
            slotsLocks[i] = new Object();
        }
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
        this.playersTokens = new boolean[env.config.columns * env.config.rows][env.config.players];
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        synchronized (slotsLocks[slot]) {
            cardToSlot[card] = slot;
            slotToCard[slot] = card;
            env.ui.placeCard(card, slot);
        }
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {

        }
        synchronized (slotsLocks[slot]){
        if (slotToCard[slot]!=null) {
            for (int i = 0; i < playersTokens[slot].length; i++) {
                if (playersTokens[slot][i]) {
                    removeToken(i, slot);
                    if (players[i].threadState() != Thread.State.RUNNABLE) {
                        players[i].myList.remove((Integer) slot);
                    }
                }
            }
            env.ui.removeCard(slot);
            cardToSlot[slotToCard[slot]] = null;
            slotToCard[slot] = null;
        }
        }
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        if(slotToCard[slot] == null){
            env.logger.warning("error placing a token! , slot is empty");
            //slotIsNull = true;
        }else {
            synchronized (slotsLocks[slot]) {
                env.ui.placeToken(player, slot);
                playersTokens[slot][player] = true;
                //slotIsNull = false;
            }
        }

    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {

        synchronized (slotsLocks[slot]) {
            env.ui.removeToken(player, slot);
            playersTokens[slot][player] = false;
            return true;
        }

    }

    public boolean isToken(int player, int slot){
        return playersTokens[slot][player];
    }
}