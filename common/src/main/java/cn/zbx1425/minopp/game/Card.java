package cn.zbx1425.minopp.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public record Card(Family family, Suit suit, int number) {

    public static List<Card> createDeck() {
        // Create a deck of UNO cards.
        List<Card> deck = new ArrayList<>();
        // Numbers
        for (Suit suit : Suit.values()) {
            if (suit == Suit.WILD) continue;
            for (int i = 0; i <= 9; i++) deck.add(new Card(Family.NUMBER, suit, i));
            for (int i = 1; i <= 9; i++) deck.add(new Card(Family.NUMBER, suit, i));
        }
        // Skip, Reverse, Draw 2
        for (Suit suit : Suit.values()) {
            if (suit == Suit.WILD) continue;
            for (int i = 0; i < 2; i++) {
                deck.add(new Card(Family.SKIP, suit, -101));
                deck.add(new Card(Family.REVERSE, suit, -102));
                deck.add(new Card(Family.DRAW, suit, -2));
            }
        }
        // Wild, Wild Draw 4
        for (int i = 0; i < 4; i++) {
            deck.add(new Card(Family.NUMBER, Suit.WILD, -1));
            deck.add(new Card(Family.DRAW, Suit.WILD, -4));
        }
        return deck;
    }

    public boolean canPlayOn(Card topCard) {
        if (topCard.family == Family.DRAW) {
            // Draw 2 and Wild Draw 4 can only be resolved by playing another Draw 2 or Wild Draw 4.
            return family == topCard.family && number == topCard.number;
        }
        if (suit == Suit.WILD) {
            // Wild cards can be played on any card.
            return true;
        }
        return suit == topCard.suit || number == topCard.number;
    }

    public Component getDisplayName() {
        return Component.translatable("game.minopp.card.suit." + suit.name().toLowerCase(),
                Component.translatable("game.minopp.card.family." + family.name().toLowerCase(), Math.abs(number)));
    }

    public enum Suit {

        RED(0xFFBE0C00),
        YELLOW(0xFFE7D004),
        GREEN(0xFF328A10),
        BLUE(0xFF1254AB),

        WILD(0xFF1E1A19);

        public final int color;

        Suit(int color) {
            this.color = color;
        }
    }

    public enum Family {

        NUMBER,
        SKIP,
        REVERSE,
        DRAW
    }

    public Card(CompoundTag tag) {
        this(Family.valueOf(tag.getString("family")), Suit.valueOf(tag.getString("suit")), tag.getInt("number"));
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("family", family.name());
        tag.putString("suit", suit.name());
        tag.putInt("number", number);
        return tag;
    }
}
